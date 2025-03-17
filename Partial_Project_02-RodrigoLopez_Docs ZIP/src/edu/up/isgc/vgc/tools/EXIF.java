package edu.up.isgc.vgc.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static edu.up.isgc.vgc.tools.CMD.*;

public class EXIF {
    private final static String exePath = "Tools/exiftool-13.22_64/exiftool.exe";

    public static String getWidth(String filePath) {
        String[] commands = new String[]{exePath, "-ImageWidth", filePath};
        return CMD.normalize(commands, ":");
    }

    public static String getHeight(String filePath) {
        String[] commands = new String[]{exePath, "-ImageHeight", filePath};
        return CMD.normalize(commands, ":");
    }

    public static String getDate(String filePath) {
        String[] commands = new String[]{exePath, "-CreateDate", filePath};
        return CMD.normalize(commands, ":");
    }

    public static String getDuration(String filePath) {
        String[] commands = new String[]{exePath, "-Duration", filePath};
        return CMD.normalize(commands, ":");
    }

    public static String getType(String filePath) {
        String[] commands = new String[]{exePath, "-MIMEType", filePath};
        return CMD.normalize(commands, ":");
    }

    public static String getCodec(String filePath) {
        String[] commands = new String[]{exePath, "-CodecID", filePath};
        return CMD.normalize(commands, ":");
    }

}
