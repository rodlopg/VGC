package edu.up.isgc.vgc.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import edu.up.isgc.vgc.tools.CMD.*;

public class FFMPEG {
    private final static String[] iFormat = new String[]{"v", "a", "s"};
    private static String exePath = "Tools/FFMPEG/ffmpeg.exe";
    private static String outPath = "Outputs";

    public static void setExePath(String newPath) {
        FFMPEG.exePath = newPath;
    }

    public static void setOutputPath(String newPath) {
        FFMPEG.outPath = newPath;
    }

    public static String[] input(String filePath){
        return new String[]{"-i", filePath};
    }

    public static String[] output(String filePath){
        return new String[]{"-o", filePath};
    }

    public static String[] copy(int format){
        return new String[]{"-c:" + iFormat[format%3], "copy"};
    }

    public static String[] sVideo(String newSize){
        return new String[]{"-vf", "scale:" + newSize};
    }

    public static String[] lxcEncode(int format, String codec){
        return new String[]{"-c:" + iFormat[format%3], codec, "-crf", "0", "-preset", "ultrafast"};
    }

    public static String[] cCodec(int format, String newCodec){
        return new String[]{"-codec:" + iFormat[format%3], newCodec};
    }

    public static String[] cFormat(int format){
        return new String[]{"|||EMPTY|||"};
    }

    public static String[] cFRate(String newRate){
        return new String[]{"-r", newRate};
    }

}
