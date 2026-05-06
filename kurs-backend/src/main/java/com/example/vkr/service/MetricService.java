package com.example.vkr.service;

import com.example.vkr.dto.MetricDto;
import com.example.vkr.entity.Device;
import com.example.vkr.entity.Event;
import com.example.vkr.entity.Metric;
import com.example.vkr.entity.User;
import com.example.vkr.mapper.MetricMapper;
import com.example.vkr.repository.DeviceRepository;
import com.example.vkr.repository.EventRepository;
import com.example.vkr.repository.MetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.NetworkIF;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetricService {
    private static final double CPU_CRITICAL_THRESHOLD = 90.0;
    private static final double RAM_CRITICAL_THRESHOLD = 90.0;
    private static final double DISK_WARNING_THRESHOLD = 95.0;
    private static final double PING_WARNING_THRESHOLD = 200.0;
    private static final double PACKET_LOSS_WARNING_THRESHOLD = 3.0;
    private static final double PACKET_LOSS_CRITICAL_THRESHOLD = 10.0;
    private static final long DUPLICATE_EVENT_COOLDOWN_MINUTES = 5L;

    private final MetricRepository metricRepository;
    private final DeviceRepository deviceRepository;
    private final EventRepository eventRepository;
    private final MetricMapper metricMapper;

    public List<MetricDto> getAll() {
        return metricRepository.findAll()
                .stream()
                .map(metricMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<MetricDto> getAllByUser(User user) {
        return metricRepository.findByDeviceUser(user)
                .stream()
                .map(metricMapper::toDto)
                .collect(Collectors.toList());
    }

    public MetricDto create(MetricDto dto) {
        Device device = deviceRepository.findById(dto.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Device not found"));
        if (dto.getTimestamp() == null) {
            dto.setTimestamp(LocalDateTime.now());
        }
        Metric metric = metricMapper.toEntity(dto, device);
        Metric saved = metricRepository.save(metric);
        return metricMapper.toDto(saved);
    }

    public List<MetricDto> getByDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        return metricRepository.findByDevice(device)
                .stream()
                .map(metricMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<MetricDto> collectForDevice(Long deviceId, int samples, int intervalSeconds, boolean saveToDb) {
        if (samples < 1 || samples > 60) {
            throw new RuntimeException("Samples must be between 1 and 60");
        }
        if (intervalSeconds < 1 || intervalSeconds > 30) {
            throw new RuntimeException("Interval must be between 1 and 30 seconds");
        }

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        SystemInfo systemInfo = new SystemInfo();
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
        FileSystem fileSystem = operatingSystem.getFileSystem();
        NetworkIF networkIF = resolveActiveNetworkInterface(systemInfo);

        List<MetricDto> collectedMetrics = new java.util.ArrayList<>();
        long[] ticks = processor.getSystemCpuLoadTicks();

        for (int i = 0; i < samples; i++) {
            long prevSent = 0L;
            long prevRecv = 0L;
            long startTrafficTs = System.currentTimeMillis();
            if (networkIF != null) {
                networkIF.updateAttributes();
                prevSent = networkIF.getBytesSent();
                prevRecv = networkIF.getBytesRecv();
            }

            try {
                Thread.sleep(intervalSeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Monitoring was interrupted", e);
            }

            double cpu = processor.getSystemCpuLoadBetweenTicks(ticks) * 100.0;
            ticks = processor.getSystemCpuLoadTicks();

            long totalMem = memory.getTotal();
            long usedMem = totalMem - memory.getAvailable();
            double ramUsage = totalMem == 0 ? 0.0 : (usedMem * 100.0 / totalMem);

            double diskUsage = calculateDiskUsage(fileSystem);
            double uploadMbps = 0.0;
            double downloadMbps = 0.0;
            if (networkIF != null) {
                networkIF.updateAttributes();
                long elapsedSec = Math.max(1L, (System.currentTimeMillis() - startTrafficTs) / 1000L);
                long sentDiff = Math.max(0L, networkIF.getBytesSent() - prevSent);
                long recvDiff = Math.max(0L, networkIF.getBytesRecv() - prevRecv);
                uploadMbps = (sentDiff * 8.0) / (elapsedSec * 1_000_000.0);
                downloadMbps = (recvDiff * 8.0) / (elapsedSec * 1_000_000.0);
            }

            double pingMs = measurePingMs("8.8.8.8");
            double packetLossPercent = measurePacketLoss("8.8.8.8");
            LocalDateTime now = LocalDateTime.now();

            device.setStatus("ONLINE");
            device.setLastCheck(now);
            deviceRepository.save(device);

            Metric metric = Metric.builder()
                    .device(device)
                    .cpuLoad(round(cpu))
                    .ramUsage(round(ramUsage))
                    .diskUsage(round(diskUsage))
                    .uploadMbps(round(uploadMbps))
                    .downloadMbps(round(downloadMbps))
                    .pingMs(round(pingMs))
                    .packetLossPercent(round(packetLossPercent))
                    .uptime(operatingSystem.getSystemUptime())
                    .timestamp(now)
                    .build();

            // События мониторинга фиксируем всегда, даже если метрики не сохраняются в БД.
            generateMonitoringEvents(device, metric, networkIF != null);

            if (saveToDb) {
                Metric saved = metricRepository.save(metric);
                collectedMetrics.add(metricMapper.toDto(saved));
            } else {
                collectedMetrics.add(metricMapper.toDto(metric));
            }
        }

        return collectedMetrics.stream()
                .sorted(Comparator.comparing(MetricDto::getTimestamp))
                .collect(Collectors.toList());
    }

    private double calculateDiskUsage(FileSystem fileSystem) {
        List<OSFileStore> stores = fileSystem.getFileStores();
        if (stores == null || stores.isEmpty()) {
            return 0.0;
        }

        long total = 0L;
        long free = 0L;
        for (OSFileStore store : stores) {
            long totalSpace = store.getTotalSpace();
            if (totalSpace > 0) {
                total += totalSpace;
                free += store.getUsableSpace();
            }
        }

        if (total == 0L) {
            return 0.0;
        }
        return (total - free) * 100.0 / total;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private NetworkIF resolveActiveNetworkInterface(SystemInfo systemInfo) {
        List<NetworkIF> interfaces = systemInfo.getHardware().getNetworkIFs();
        NetworkIF selected = null;
        long maxTraffic = -1L;
        for (NetworkIF nif : interfaces) {
            nif.updateAttributes();
            long traffic = nif.getBytesRecv() + nif.getBytesSent();
            boolean isLoopbackOrVirtual = nif.getDisplayName().toLowerCase().contains("loopback")
                    || nif.getDisplayName().toLowerCase().contains("virtual");
            if (!isLoopbackOrVirtual && traffic > maxTraffic) {
                maxTraffic = traffic;
                selected = nif;
            }
        }
        return selected;
    }

    private double measurePingMs(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            long start = System.nanoTime();
            boolean reachable = address.isReachable(3000);
            long end = System.nanoTime();
            return reachable ? (end - start) / 1_000_000.0 : -1.0;
        } catch (Exception e) {
            return -1.0;
        }
    }

    private double measurePacketLoss(String host) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String command = os.contains("win") ? "ping -n 4 " + host : "ping -c 4 " + host;
            Process process = Runtime.getRuntime().exec(command);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), os.contains("win") ? "CP866" : "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("%")) {
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)%").matcher(line);
                        if (m.find()) {
                            return Double.parseDouble(m.group(1));
                        }
                    }
                }
            }
            return -1.0;
        } catch (Exception e) {
            return -1.0;
        }
    }

    private void generateMonitoringEvents(Device device, Metric metric, boolean networkInterfaceDetected) {
        if (metric.getCpuLoad() != null && metric.getCpuLoad() >= CPU_CRITICAL_THRESHOLD) {
            createMonitoringEvent(
                    device,
                    "CRITICAL",
                    "Высокая нагрузка CPU",
                    String.format("Высокая нагрузка CPU: %.2f%%", metric.getCpuLoad())
            );
        }

        if (metric.getRamUsage() != null && metric.getRamUsage() >= RAM_CRITICAL_THRESHOLD) {
            createMonitoringEvent(
                    device,
                    "CRITICAL",
                    "Высокое использование RAM",
                    String.format("Высокое использование RAM: %.2f%%", metric.getRamUsage())
            );
        }

        if (metric.getDiskUsage() != null && metric.getDiskUsage() >= DISK_WARNING_THRESHOLD) {
            createMonitoringEvent(
                    device,
                    "WARNING",
                    "Диск почти заполнен",
                    String.format("Диск почти заполнен: %.2f%%", metric.getDiskUsage())
            );
        }

        if (!networkInterfaceDetected) {
            createMonitoringEvent(
                    device,
                    "WARNING",
                    "Не удалось определить активный сетевой интерфейс",
                    "Не удалось определить активный сетевой интерфейс"
            );
        }

        if (metric.getPingMs() != null) {
            if (metric.getPingMs() < 0) {
                createMonitoringEvent(
                        device,
                        "ERROR",
                        "Узел 8.8.8.8 недоступен",
                        "Узел 8.8.8.8 недоступен (ping timeout)"
                );
            } else if (metric.getPingMs() >= PING_WARNING_THRESHOLD) {
                createMonitoringEvent(
                        device,
                        "WARNING",
                        "Высокий ping",
                        String.format("Высокий ping: %.2f ms", metric.getPingMs())
                );
            }
        }

        if (metric.getPacketLossPercent() != null) {
            if (metric.getPacketLossPercent() >= PACKET_LOSS_CRITICAL_THRESHOLD) {
                createMonitoringEvent(
                        device,
                        "CRITICAL",
                        "Критические потери пакетов",
                        String.format("Критические потери пакетов: %.2f%%", metric.getPacketLossPercent())
                );
            } else if (metric.getPacketLossPercent() >= PACKET_LOSS_WARNING_THRESHOLD) {
                createMonitoringEvent(
                        device,
                        "WARNING",
                        "Потери пакетов",
                        String.format("Потери пакетов: %.2f%%", metric.getPacketLossPercent())
                );
            }
        }
    }

    private void createMonitoringEvent(Device device, String severity, String descriptionPrefix, String description) {
        Event lastSameEvent = eventRepository.findTopByDeviceAndSeverityAndDescriptionStartingWithOrderByTimestampDesc(
                device, severity, descriptionPrefix
        );
        LocalDateTime now = LocalDateTime.now();
        if (lastSameEvent != null
                && lastSameEvent.getTimestamp() != null
                && lastSameEvent.getTimestamp().isAfter(now.minusMinutes(DUPLICATE_EVENT_COOLDOWN_MINUTES))) {
            return;
        }

        User owner = device.getUser();
        Event event = Event.builder()
                .device(device)
                .timestamp(now)
                .severity(severity)
                .description(description)
                .resolved(false)
                .createdBy(owner)
                .build();
        eventRepository.save(event);
    }

    public MetricDto update(MetricDto dto) {
        Metric existing = metricRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Metric not found"));
        if (dto.getCpuLoad() != null) existing.setCpuLoad(dto.getCpuLoad());
        if (dto.getRamUsage() != null) existing.setRamUsage(dto.getRamUsage());
        if (dto.getDiskUsage() != null) existing.setDiskUsage(dto.getDiskUsage());
        if (dto.getUploadMbps() != null) existing.setUploadMbps(dto.getUploadMbps());
        if (dto.getDownloadMbps() != null) existing.setDownloadMbps(dto.getDownloadMbps());
        if (dto.getPingMs() != null) existing.setPingMs(dto.getPingMs());
        if (dto.getPacketLossPercent() != null) existing.setPacketLossPercent(dto.getPacketLossPercent());
        if (dto.getUptime() != null) existing.setUptime(dto.getUptime());
        if (dto.getTimestamp() != null) existing.setTimestamp(dto.getTimestamp());

        Metric updated = metricRepository.save(existing);
        return metricMapper.toDto(updated);
    }

    public void delete(Long id) {
        metricRepository.deleteById(id);
    }
}