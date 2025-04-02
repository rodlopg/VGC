package edu.up.isgc.vgc.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import static edu.up.isgc.vgc.tools.CMD.*;

public class EXIF {
    // Path to ExifTool executable, normalized to ensure compatibility
    private final static String exePath = CMD.normalizePath(
            new File("Tools/exiftool-13.22_64/exiftool.exe").getAbsolutePath() // HERE ADD YOUR PATH TO THE exiftool.exe AND PLEASE DO NOT FORGET TO REMOVE THE (-k) from the name, otherwise it will stay open after running a command and we don't want that
    );

    // Gets the width of an image
    public static String getWidth(String filePath) {
        String[] commands = new String[]{exePath, "-ImageWidth", CMD.normalizePath(filePath)};
        return CMD.normalize(commands, ":");
    }

    // Gets the height of an image
    public static String getHeight(String filePath) {
        String[] commands = new String[]{exePath, "-ImageHeight", CMD.normalizePath(filePath)};
        return CMD.normalize(commands, ":");
    }

    // Gets the creation date of a file
    public static String getDate(String filePath) {
        String[] commands = new String[]{exePath, "-CreateDate", CMD.normalizePath(filePath)};
        return CMD.normalize(commands, ":");
    }

    // Gets the duration of a media file
    public static String getDuration(String filePath) {
        String[] commands = new String[]{exePath, "-Duration", CMD.normalizePath(filePath)};
        String subDuration = CMD.normalize(commands, ":");
        String rawDuration = CMD.trimUntil(subDuration, " ");
        return normalizeDuration(rawDuration);
    }

    // Normalizes duration to a numeric format with three decimal places
    private static String normalizeDuration(String rawDuration) {
        try {
            String cleanDuration = rawDuration
                    .replaceAll("[^\\d.:]", "") // Removes non-numeric characters except ':' and '.'
                    .replaceAll(":+$", "") // Removes trailing ':'
                    .trim();

            if (cleanDuration.contains(":")) {
                String[] parts = cleanDuration.split(":");
                double totalSeconds = 0;

                if (parts.length == 3) { // Format HH:MM:SS[.sss]
                    totalSeconds += Double.parseDouble(parts[0]) * 3600;
                    totalSeconds += Double.parseDouble(parts[1]) * 60;
                    totalSeconds += Double.parseDouble(parts[2]);
                } else if (parts.length == 2) { // Format MM:SS[.sss]
                    totalSeconds += Double.parseDouble(parts[0]) * 60;
                    totalSeconds += Double.parseDouble(parts[1]);
                } else {
                    throw new IllegalArgumentException("Unknown duration format: " + rawDuration);
                }

                return String.format("%.3f", totalSeconds);
            }

            if (rawDuration.matches(".*\\d+\\s*s$")) { // Format X[s]
                String seconds = rawDuration.replaceAll("[^\\d.]", "");
                return String.format("%.3f", Double.parseDouble(seconds));
            }

            return String.format("%.3f", Double.parseDouble(cleanDuration));
        } catch (Exception e) {
            System.err.println("Error normalizing duration: " + rawDuration);
            return "0.000";
        }
    }

    // Gets the MIME type of a file
    public static String getType(String filePath) {
        String[] commands = new String[]{exePath, "-MIMEType", CMD.normalizePath(filePath)};
        String subType = CMD.normalize(commands, ":");
        return CMD.trimFrom(subType, "/");
    }

    // Gets the codec of a media file
    public static String getCodec(String filePath) {
        String[] commands = new String[]{exePath, "-CodecID", CMD.normalizePath(filePath)};
        return CMD.normalize(commands, ":");
    }
}