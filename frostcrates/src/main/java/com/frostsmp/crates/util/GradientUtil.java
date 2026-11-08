package com.frostsmp.crates.util;

public final class GradientUtil {

    private GradientUtil() {
    }

    public static String letterGradient(String text, String startHex, String endHex) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String from = sanitizeHex(startHex);
        String to = sanitizeHex(endHex);
        StringBuilder builder = new StringBuilder();
        int length = text.length();
        for (int index = 0; index < length; index++) {
            char character = text.charAt(index);
            if (character == ' ') {
                builder.append(' ');
                continue;
            }
            double ratio = length == 1 ? 0.5 : (double) index / (length - 1);
            builder.append("<#").append(interpolateHex(from, to, ratio)).append(">").append(character);
        }
        return builder.toString();
    }

    private static String sanitizeHex(String hex) {
        if (hex == null || hex.isBlank()) {
            return "55FFFF";
        }
        String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
        if (cleaned.length() != 6) {
            return "55FFFF";
        }
        return cleaned.toUpperCase();
    }

    private static String interpolateHex(String from, String to, double ratio) {
        int fromRed = Integer.parseInt(from.substring(0, 2), 16);
        int fromGreen = Integer.parseInt(from.substring(2, 4), 16);
        int fromBlue = Integer.parseInt(from.substring(4, 6), 16);
        int toRed = Integer.parseInt(to.substring(0, 2), 16);
        int toGreen = Integer.parseInt(to.substring(2, 4), 16);
        int toBlue = Integer.parseInt(to.substring(4, 6), 16);

        int red = (int) Math.round(fromRed + (toRed - fromRed) * ratio);
        int green = (int) Math.round(fromGreen + (toGreen - fromGreen) * ratio);
        int blue = (int) Math.round(fromBlue + (toBlue - fromBlue) * ratio);
        return String.format("%02X%02X%02X", red, green, blue);
    }
}
