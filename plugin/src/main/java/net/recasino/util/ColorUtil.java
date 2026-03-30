package net.recasino.util;

import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&?#[a-fA-F0-9]{6}");
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("0.##");

    private ColorUtil() {
    }

    public static String color(String message) {
        if (message == null) {
            return "";
        }

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder builder = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            String hexCode = matcher.group().replace("&", "").replace('#', 'x');
            StringBuilder translated = new StringBuilder();
            for (char character : hexCode.toCharArray()) {
                translated.append('&').append(character);
            }

            builder.append(message, lastEnd, matcher.start());
            builder.append(translated);
            lastEnd = matcher.end();
        }
        builder.append(message.substring(lastEnd));
        return ChatColor.translateAlternateColorCodes('&', builder.toString());
    }

    public static String formatNumber(double value) {
        return NUMBER_FORMAT.format(value);
    }
}

