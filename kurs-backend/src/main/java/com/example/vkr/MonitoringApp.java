package com.example.vkr;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MonitoringApp {

    public static void main(String[] args) throws InterruptedException {

        for (int iteration = 1; iteration <= 2; iteration++) {
            System.out.println("\n========== ЗАМЕР №" + iteration + " ==========");
            performMonitoring();

            if (iteration < 2) {
                System.out.println("\nОжидание 5 секунд перед следующим замером...");
                TimeUnit.SECONDS.sleep(5);
            }
        }
    }

    // ==================== СЕТЕВЫЕ ПАРАМЕТРЫ ====================

    public static void getNetworkInfo() {
        // IP-адрес
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            System.out.println("  IPv4: " + localHost.getHostAddress());
        } catch (UnknownHostException e) {
            System.out.println("  IPv4: не определен");
        }

        // Шлюз (исправленный парсинг)
        try {
            Process process = Runtime.getRuntime().exec("ipconfig");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "CP866"));
            String line;
            String gateway = "";

            while ((line = reader.readLine()) != null) {
                if (line.contains("Основной шлюз") || line.contains("Default Gateway")) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+").matcher(line);
                    if (m.find()) {
                        gateway = m.group();
                        break;
                    }
                }
            }
            reader.close();

            if (!gateway.isEmpty() && !gateway.equals("0.0.0.0") && !gateway.equals("::")) {
                System.out.println("  Шлюз по умолчанию: " + gateway);
            } else {
                System.out.println("  Шлюз по умолчанию: не определен");
            }
        } catch (Exception e) {
            System.out.println("  Шлюз по умолчанию: ошибка");
        }

        // DNS-серверы
        try {
            Process process = Runtime.getRuntime().exec("ipconfig /all");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "CP866"));
            String line;
            java.util.ArrayList<String> dnsList = new java.util.ArrayList<>();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if ((line.contains("DNS") || line.contains("Сервер")) && line.matches(".*\\d+\\.\\d+\\.\\d+\\.\\d+.*")) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)").matcher(line);
                    while (m.find()) {
                        String dns = m.group(1);
                        if (!dnsList.contains(dns)) {
                            dnsList.add(dns);
                        }
                    }
                }
            }
            reader.close();

            System.out.println("  DNS-серверы:");
            if (dnsList.isEmpty()) {
                System.out.println("    - не определены");
            } else {
                for (String dns : dnsList) {
                    System.out.println("    - " + dns);
                }
            }
        } catch (Exception e) {
            System.out.println("  DNS-серверы: ошибка");
        }
    }

    public static void checkPing(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            long startTime = System.nanoTime();
            boolean reachable = address.isReachable(3000);
            long endTime = System.nanoTime();

            if (reachable) {
                long latencyMs = (endTime - startTime) / 1_000_000;
                System.out.println("  " + host + " - ДОСТУПЕН (задержка: " + latencyMs + " мс)");
            } else {
                System.out.println("  " + host + " - НЕДОСТУПЕН");
            }
        } catch (UnknownHostException e) {
            System.out.println("  " + host + " - НЕИЗВЕСТНЫЙ ХОСТ");
        } catch (Exception e) {
            System.out.println("  " + host + " - ОШИБКА: " + e.getMessage());
        }
    }

    public static void checkPacketLoss(String host) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String command;

            if (os.contains("win")) {
                command = "ping -n 4 " + host;
            } else {
                command = "ping -c 4 " + host;
            }

            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), os.contains("win") ? "CP866" : "UTF-8"));
            String line;
            double lossPercent = -1;

            while ((line = reader.readLine()) != null) {
                if (line.contains("% потерь") || line.contains("% loss") || line.contains("packet loss")) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)%").matcher(line);
                    if (m.find()) {
                        lossPercent = Double.parseDouble(m.group(1));
                    }
                }
            }
            reader.close();

            if (lossPercent >= 0) {
                String status = lossPercent == 0 ? "НОРМА" : (lossPercent < 50 ? "СРЕДНЯЯ" : "ВЫСОКАЯ");
                System.out.println("  " + host + " - Потеря пакетов: " + String.format("%.0f", lossPercent) + "% (" + status + ")");
            } else {
                System.out.println("  " + host + " - Не удалось определить потерю пакетов");
            }
        } catch (Exception e) {
            System.out.println("  " + host + " - Ошибка измерения: " + e.getMessage());
        }
    }

    public static void performMonitoring() throws InterruptedException {
        SystemInfo si = new SystemInfo();

        // === 1. Поиск активного сетевого интерфейса ===
        List<NetworkIF> allInterfaces = si.getHardware().getNetworkIFs();
        NetworkIF networkIF = null;
        String baseInterfaceName = "";

        System.out.println("=== Поиск активного сетевого интерфейса ===");

        for (NetworkIF nif : allInterfaces) {
            nif.updateAttributes();
            String name = nif.getDisplayName();
            long bytesSent = nif.getBytesSent();
            long bytesRecv = nif.getBytesRecv();

            boolean isFilter = name.contains("WFP") || name.contains("Filter") ||
                    name.contains("QoS") || name.contains("LightWeight") ||
                    name.contains("Virtual") || name.contains("TAP") ||
                    name.contains("Loopback");

            if ((bytesSent > 1000000 || bytesRecv > 1000000) && !isFilter && networkIF == null) {
                networkIF = nif;
                baseInterfaceName = name.split("-WFP")[0].split("-Native")[0].split("-VirtualBox")[0].trim();
                System.out.println("Найден рабочий интерфейс: " + baseInterfaceName);
            }
        }

        if (networkIF == null) {
            System.out.println("Активные сетевые интерфейсы не найдены!");
            return;
        }

        // === 2. Замер трафика ===
        networkIF.updateAttributes();
        long prevSent = networkIF.getBytesSent();
        long prevRecv = networkIF.getBytesRecv();
        long prevTime = System.currentTimeMillis();

        // === 3. Замер CPU ===
        CentralProcessor processor = si.getHardware().getProcessor();
        long[] prevTicks = processor.getSystemCpuLoadTicks();

        TimeUnit.SECONDS.sleep(2);

        networkIF.updateAttributes();
        long currentSent = networkIF.getBytesSent();
        long currentRecv = networkIF.getBytesRecv();
        long currentTime = System.currentTimeMillis();

        long timeDiff = (currentTime - prevTime) / 1000;
        if (timeDiff == 0) timeDiff = 1;

        long uploadBytes = currentSent - prevSent;
        long downloadBytes = currentRecv - prevRecv;

        double uploadMbps = (uploadBytes * 8.0) / (timeDiff * 1_000_000.0);
        double downloadMbps = (downloadBytes * 8.0) / (timeDiff * 1_000_000.0);

        double totalSentMB = currentSent / 1_000_000.0;
        double totalRecvMB = currentRecv / 1_000_000.0;

        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;

        GlobalMemory memory = si.getHardware().getMemory();
        long totalMem = memory.getTotal();
        long usedMem = totalMem - memory.getAvailable();

        // === 4. ВЫВОД РЕЗУЛЬТАТОВ ===

        System.out.println("\n=== СЕТЕВЫЕ ПАРАМЕТРЫ ===");
        System.out.println("Интерфейс: " + baseInterfaceName);
        System.out.println("MAC адрес: " + networkIF.getMacaddr());
        System.out.println("Скорость адаптера: " + networkIF.getSpeed() / 1_000_000 + " Mbps");

        System.out.println("\n--- Детальная конфигурация сети ---");
        getNetworkInfo();

        System.out.println("\n--- ПРОВЕРКА ДОСТУПНОСТИ (PING) ---");
        checkPing("8.8.8.8");
        checkPing("ya.ru");
        checkPing("google.com");

        System.out.println("\n--- ПОТЕРЯ ПАКЕТОВ (PACKET LOSS) ---");
        checkPacketLoss("8.8.8.8");
        checkPacketLoss("ya.ru");

        System.out.println("\n--- ТРАФИК ---");
        System.out.printf("Исходящий трафик: %.2f Mbps (%d байт)\n", uploadMbps, uploadBytes);
        System.out.printf("Входящий трафик: %.2f Mbps (%d байт)\n", downloadMbps, downloadBytes);
        System.out.println("  (замер за " + timeDiff + " секунд)");

        System.out.println("\n--- НАКОПЛЕННЫЙ ОБЪЁМ (с момента загрузки) ---");
        System.out.printf("Всего отправлено: %.2f MB\n", totalSentMB);
        System.out.printf("Всего получено: %.2f MB\n", totalRecvMB);

        System.out.println("\n=== СИСТЕМНЫЕ ПАРАМЕТРЫ ===");
        System.out.printf("Загрузка CPU: %.2f%%\n", cpuLoad);
        System.out.printf("Оперативная память: %d MB / %d MB (использовано / всего)\n", usedMem / 1024 / 1024, totalMem / 1024 / 1024);

        System.out.println("\n=== ОПЕРАЦИОННАЯ СИСТЕМА ===");
        OperatingSystem os = si.getOperatingSystem();
        System.out.println("Имя: " + os.getFamily());
        System.out.println("Версия: " + os.getVersionInfo().getVersion());
    }
}