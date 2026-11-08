package com.frostsmp.license;

import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class LicenseGate {

    private LicenseGate() {
    }

    public static boolean activate(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        String key = plugin.getConfig().getString("license.key", "");
        if (key == null || key.isBlank()) {
            plugin.getLogger().severe("Missing license.key in config.yml");
            return false;
        }
        if (!matches(key.trim())) {
            plugin.getLogger().severe("Invalid license key.");
            return false;
        }
        return true;
    }

    private static boolean matches(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] actual = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            byte[] expected = decodeHex(LicenseConstants.EXPECTED_HASH);
            return MessageDigest.isEqual(actual, expected);
        } catch (Exception exception) {
            return false;
        }
    }

    private static byte[] decodeHex(String hex) {
        int length = hex.length();
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
