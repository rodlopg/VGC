package edu.up.isgc.vgc.tools.ffmpeg;

import java.util.List;
import java.util.function.Function;

import edu.up.isgc.vgc.tools.CMD;
import edu.up.isgc.vgc.tools.Pipeline;

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

    public static String[] preset(int preset){ return new String[]{"-preset", Format.getPreset(preset)}; }

    public static String[] crf(int crf){ return new String[]{"-crf", Format.getCRF(crf)}; }

    public static String[] mapOutput(int iFormat){ return new String[]{"-map", "[out"+Format.getFile(iFormat)+"]"}; }

    public static String[] copy(int format){
        return new String[]{"-c:" + Format.getFile(format), "copy"};
    }

    public static String[] cFormat(int format){
        return new String[]{"|||EMPTY|||"};
    }

    public static String[] cFRate(String newRate){
        return new String[]{"-r", newRate};
    }

    public static String[] lxcEncode(int format, int codec, int crf, int preset){
        List<Function<String[], String[]>> functions = List.of(
                input -> new String[]{"-c:" + Format.getFile(format)},
                input -> new String[]{Format.getCodec(format, codec)},
                input -> FFMPEG.crf(crf),
                input -> FFMPEG.preset(preset)
        );
        return Pipeline.biLambda(functions, CMD::concat);
    }
    public static String[] lxcEncode(int format, int codec){
        return lxcEncode(format, codec, 2, 5);
    }

    public static String[] cCodec(int format, String newCodec){
        return new String[]{"-codec:" + Format.getFile(format), newCodec};
    }

    public static String[] pixelFormat(int format){ return new String[]{"-pix_fmt", Format.getPixel(format)}; }
    public static String[] pixelFormat(){ return FFMPEG.pixelFormat(0); }

    public static String[] loopImg(int duration, String codec, String filePath){
        String sDuration = Integer.toString(duration);

        List<Function<String[], String[]>> functions = List.of(
                input -> new String[]{"-loop", "1"},
                input -> FFMPEG.input(filePath),
                input -> FFMPEG.lxcEncode(0, 0),
                input -> new String[]{"-t", sDuration},
                input -> FFMPEG.pixelFormat()
        );
        return Pipeline.biLambda(functions, CMD::concat);
    }

    public static String[] inputMany(String[] inputFiles){
        String[] inputCommand = new String[]{};
        for(String file : inputFiles){ CMD.concat(inputCommand, FFMPEG.input(file)); }
        return inputCommand;
    }

    public static String[] normalize(int amount, int iFormat, int cFormat, String newSize, String[] inputFiles){
        String[] filter = new String[]{Filter.sVideo(newSize, 0, 1), Filter.setPTS()};

        List<Function<String[], String[]>> functions = List.of(
                input -> FFMPEG.inputMany(inputFiles),
                input -> new String[]{"-filter_complex", "[0:v:0]scale=1280:720:force_original_aspect_ratio=decrease,setpts=PTS-STARTPTS[v0];"},
                input -> Filter.complex(amount, iFormat, 0, CMD.join(filter, ",")),
                input -> FFMPEG.mapOutput(0),
                input -> FFMPEG.mapOutput(1),
                input -> FFMPEG.lxcEncode(0,1, 18,8),
                input -> new String[]{"-c:a", "aac", "-b:a", "192k", "output.mp4"}
        );

        return Pipeline.biLambda(functions, CMD::concat);
    }

}

