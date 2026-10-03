package com.sofoste.thorcompanion;

import java.lang.management.ManagementFactory;
import java.time.Duration;

public final class TelemetryService {
    private final com.sun.management.OperatingSystemMXBean operatingSystem =
            (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    public TelemetrySnapshot sample() {
        double cpu = Math.max(0, operatingSystem.getCpuLoad());
        long total = operatingSystem.getTotalMemorySize();
        long used = total - operatingSystem.getFreeMemorySize();
        double memory = total == 0 ? 0 : (double) used / total;
        Duration uptime = Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());
        return new TelemetrySnapshot(cpu, memory, uptime);
    }

    public record TelemetrySnapshot(double cpu, double memory, Duration uptime) {
    }
}
