package com.example.vkr;

import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.*;

import java.net.InetAddress;
import java.util.List;

public class MonitoringApp {

    public static void main(String[] args) throws Exception {

        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        OperatingSystem os = si.getOperatingSystem();

        CentralProcessor cpu = hal.getProcessor();
        GlobalMemory memory = hal.getMemory();

        long[] prevTicks = cpu.getSystemCpuLoadTicks();

        NetworkIF activeNet = hal.getNetworkIFs()
                .stream()
                .peek(NetworkIF::updateAttributes)
                .filter(n ->
                        n.getIPv4addr().length > 0 &&
                                n.getSpeed() > 0 &&
                                !n.getDisplayName().toLowerCase().contains("virtual") &&
                                !n.getDisplayName().toLowerCase().contains("tunnel") &&
                                !n.getDisplayName().toLowerCase().contains("tap")
                )
                .findFirst()
                .orElse(null);

        if (activeNet == null) {
            System.out.println("❌ Не найден реальный сетевой интерфейс");
            return;
        }

        System.out.println("✅ Active interface: " + activeNet.getDisplayName());

        long prevRecv = activeNet.getBytesRecv();
        long prevSent = activeNet.getBytesSent();

        while (true) {

            System.out.println("\n══════════════════════════════════════");
            System.out.println("📊 SYSTEM MONITOR");
            System.out.println("══════════════════════════════════════");

            // ===== CPU =====
            double cpuLoad = cpu.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
            prevTicks = cpu.getSystemCpuLoadTicks();

            cpuLoad = Math.max(0, Math.min(100, cpuLoad));

            // ===== RAM =====
            double totalMem = memory.getTotal();
            double availableMem = memory.getAvailable();
            double ramUsage = ((totalMem - availableMem) / totalMem) * 100;

            System.out.printf("CPU: %.2f %%\n", cpuLoad);
            System.out.printf("RAM: %.2f %%\n", ramUsage);

            // ===== UPTIME =====
            System.out.println("Uptime: " + formatUptime(os.getSystemUptime()));

            // ===== NETWORK =====
            activeNet.updateAttributes();

            long currentRecv = activeNet.getBytesRecv();
            long currentSent = activeNet.getBytesSent();

            long speedIn = Math.max(0, currentRecv - prevRecv);
            long speedOut = Math.max(0, currentSent - prevSent);

            prevRecv = currentRecv;
            prevSent = currentSent;

            System.out.println("\n🌐 NETWORK:");
            System.out.println("IP: " + String.join(", ", activeNet.getIPv4addr()));
            System.out.println("Speed IN: " + formatBytes(speedIn) + "/s");
            System.out.println("Speed OUT: " + formatBytes(speedOut) + "/s");

            // ===== REAL PING =====
            try {
                Process ping = Runtime.getRuntime().exec("ping -n 1 8.8.8.8");
                int result = ping.waitFor();

                if (result == 0) {
                    System.out.println("Ping: OK");
                } else {
                    System.out.println("Ping: FAIL");
                }

            } catch (Exception e) {
                System.out.println("Ping error: " + e.getMessage());
            }

            // ===== DISK =====
            System.out.println("\n💾 DISKS:");
            for (OSFileStore store : os.getFileSystem().getFileStores()) {

                long total = store.getTotalSpace();
                long free = store.getUsableSpace();

                if (total == 0) continue;

                double usage = (double) (total - free) / total * 100;

                System.out.printf("%s: %.2f %%\n", store.getMount(), usage);

                if (usage > 90) {
                    System.out.println("[WARNING] Disk almost full!");
                }
            }

            // ===== TOP PROCESSES =====
            System.out.println("\n⚙️ TOP PROCESSES:");

            List<OSProcess> processes = os.getProcesses();

            processes.sort((a, b) ->
                    Double.compare(
                            b.getProcessCpuLoadCumulative(),
                            a.getProcessCpuLoadCumulative()
                    )
            );

            int limit = Math.min(5, processes.size());

            for (int i = 0; i < limit; i++) {
                OSProcess p = processes.get(i);

                double procCpu = p.getProcessCpuLoadCumulative() * 100;
                procCpu = Math.max(0, Math.min(100, procCpu));

                System.out.printf("%s -> %.2f %%\n",
                        p.getName(),
                        procCpu
                );
            }

            // ===== ALERTS =====
            if (cpuLoad > 80) {
                System.out.println("[WARNING] High CPU load!");
            }

            if (ramUsage > 90) {
                System.out.println("[ERROR] High RAM usage!");
            }

            if (speedIn > 5_000_000) {
                System.out.println("[INFO] High network traffic!");
            }

            System.out.println("══════════════════════════════════════");

            Thread.sleep(2000);
        }
    }

    // ===== UTIL METHODS =====

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private static String formatUptime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return hours + "h " + minutes + "m " + secs + "s";
    }
}