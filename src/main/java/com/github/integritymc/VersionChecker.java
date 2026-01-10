package com.github.integritymc;

import org.bukkit.Bukkit;

public class VersionChecker {
    private static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (num1 < num2) {
                return -1;
            }
            if (num1 > num2) {
                return 1;
            }
        }
        return 0;
    }

    private static String getCleanServerVersion() {
        return Bukkit.getBukkitVersion().split("-")[0];
    }

    public static boolean isNewerOrEqualVersion(ServerVersion serverVersion) {
        try {
            return compareVersions(getCleanServerVersion(), serverVersion.toString()) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isNewerVersion(ServerVersion serverVersion) {
        try {
            return compareVersions(getCleanServerVersion(), serverVersion.toString()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isOlderOrEqualVersion(ServerVersion serverVersion) {
        try {
            return compareVersions(getCleanServerVersion(), serverVersion.toString()) <= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isOlderVersion(ServerVersion serverVersion) {
        try {
            return compareVersions(getCleanServerVersion(), serverVersion.toString()) < 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
