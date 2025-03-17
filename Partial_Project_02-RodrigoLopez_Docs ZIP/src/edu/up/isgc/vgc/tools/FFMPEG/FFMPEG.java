package edu.up.isgc.vgc.tools.FFMPEG;

import java.util.List;
import java.util.function.Function;

import edu.up.isgc.vgc.tools.CMD;
import edu.up.isgc.vgc.tools.Pipeline;

import static edu.up.isgc.vgc.tools.FFMPEG.Format.*;

public class FFMPEG {

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
        return new String[]{"-c:" + Format.getIFormat(format), "copy"};
    }

    public static String[] sVideo(String newSize){
        return new String[]{"-vf", "scale:" + newSize};
    }

    public static String[] cFormat(int format){
        return new String[]{"|||EMPTY|||"};
    }

    public static String[] cFRate(String newRate){
        return new String[]{"-r", newRate};
    }

    public static String[] lxcEncode(int format, String codec){
        return new String[]{"-c:" + Format.getIFormat(format), codec, "-crf", "0", "-preset", "ultrafast"};
    }

    public static String[] cCodec(int format, String newCodec){
        return new String[]{"-codec:" + Format.getIFormat(format), newCodec};
    }

    public static String[] pixelFormat(int format){ return new String[]{"-pix_fmt", Format.getPFormat(format)}; }
    public static String[] pixelFormat(){ return pixelFormat(0); }

    public static String[] loopImg(int duration, String codec, String filePath){
        String sDuration = Integer.toString(duration);

        List<Function<String[], String[]>> functions = List.of(
                input -> new String[]{"-loop", "1"},
                input -> FFMPEG.input(filePath),
                input -> FFMPEG.lxcEncode(0, Format.getCFormat(0,0)),
                input -> new String[]{"-t", sDuration},
                input -> FFMPEG.pixelFormat()
        );
        return Pipeline.biLambda(functions, CMD::concat);
    }

}

