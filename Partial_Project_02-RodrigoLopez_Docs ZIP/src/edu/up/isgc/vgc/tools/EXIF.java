package edu.up.isgc.vgc.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import static edu.up.isgc.vgc.tools.CMD.*;

public class EXIF {
    private final static String exePath = CMD.normalizePath(
            new File("Tools/exiftool-13.22_64/exiftool.exe").getAbsolutePath()
    );

    public static String getWidth(String filePath) {
        String[] commands = new String[]{exePath, "-ImageWidth", CMD.normalizePath(filePath)};
        return CMD.normalize(commands, ":");
    }

    public static String getHeight(String filePath) {
        String[] commands = new String[]{exePath, "-ImageHeight", CMD.normalizePath(filePath)};
        return CMD.normalize(commands, ":");
    }

    public static String getDate(String filePath) {
        String[] commands = new String[]{exePath, "-CreateDate", CMD.normalizePath(filePath)};
        return CMD.normalize(commands, ":");
    }

    public static String getDuration(String filePath) {
        String[] commands = new String[]{exePath, "-Duration", CMD.normalizePath(filePath)};
        String subDuration = CMD.normalize(commands, ":");
        String rawDuration = CMD.trimUntil(subDuration, " ");
        return normalizeDuration(rawDuration);
    }

    private static String normalizeDuration(String rawDuration) {
        try {
            String cleanDuration = rawDuration
                    .replaceAll("[^\\d.:]", "")
                    .replaceAll(":+$", "")
                    .trim();

            if (cleanDuration.contains(":")) {
                String[] parts = cleanDuration.split(":");
                double totalSeconds = 0;

                if (parts.length == 3) { // HH:MM:SS[.sss]
                    totalSeconds += Double.parseDouble(parts[0]) * 3600;
                    totalSeconds += Double.parseDouble(parts[1]) * 60;
                    totalSeconds += Double.parseDouble(parts[2]);
                } else if (parts.length == 2) { // MM:SS[.sss]
                    totalSeconds += Double.parseDouble(parts[0]) * 60;
                    totalSeconds += Double.parseDouble(parts[1]);
                } else {
                    throw new IllegalArgumentException("Unknown duration format: " + rawDuration);
                }

                return String.format("%.3f", totalSeconds);
            }

            if (rawDuration.matches(".*\\d+\\s*s$")) { // X[s] format
                String seconds = rawDuration.replaceAll("[^\\d.]", "");
                return String.format("%.3f", Double.parseDouble(seconds));
            }

            return String.format("%.3f", Double.parseDouble(cleanDuration));
        } catch (Exception e) {
            System.err.println("Error normalizing duration: " + rawDuration);
            return "0.000";
        }
    }

    public static String getType(String filePath) {
        String[] commands = new String[]{exePath, "-MIMEType", CMD.normalizePath(filePath)};
        String subType = CMD.normalize(commands, ":");
        return CMD.trimFrom(subType, "/");
    }

    public static String getCodec(String filePath) {
        String[] commands = new String[]{exePath, "-CodecID", CMD.normalizePath(filePath)};
        return CMD.normalize(commands, ":");
    }
}