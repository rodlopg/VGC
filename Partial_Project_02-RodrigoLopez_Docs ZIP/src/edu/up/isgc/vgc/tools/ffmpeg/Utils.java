package edu.up.isgc.vgc.tools.ffmpeg;

import edu.up.isgc.vgc.tools.CMD;
import edu.up.isgc.vgc.tools.Pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Utils {

    public static String[] input(String filePath) {
        return new String[]{"-i", CMD.normalizePath(filePath)};
    }

    public static String[] inputConcatList(String listFile){
        return CMD.concat(new String[]{"-f", "concat", "-safe", "0"}, Utils.input(listFile));
    }

    public static String[] output(String filePath) {
        return new String[]{CMD.normalizePath(filePath)};
    }

    public static String[] preset(int preset) {
        return new String[]{"-preset", Format.getPreset(preset)};
    }

    public static String[] time(int time) { return new String[]{"-t", Integer.toString(time)}; }

    public static String[] crf(int crf) {
        return new String[]{"-crf", Format.getCRF(crf)};
    }

    public static String[] exeOverwrite(){ return new String[]{FFMPEG.getExePath(), "-y"}; }

    public static String[] mapOutput(int iFormat) {
        return new String[]{"-map", "[out" + Format.getFile(iFormat) + "]"};
    }

    public static String[] mapStream(int iFormat, int stream) {
        return new String[]{"-map", stream + ":" + Format.getFile(iFormat)};
    }

    public static String[] copy(int iFormat) {
        return new String[]{"-c:" + Format.getFile(iFormat), "copy"};
    }

    public static String[] cFormat(int iFormat, int cFormat) {
        return new String[]{"-c:" + Format.getFile(iFormat), Format.getCodec(iFormat, cFormat)};
    }

    public static String[] cPRate(int newRate) {
        return new String[]{"-r", Integer.toString(newRate)};
    }

    public static String[] lxcEncode(int iFormat, int cFormat, int crf, int preset) {
        List<Function<String[], String[]>> functions = new ArrayList<>();
        functions.add(input -> new String[]{"-c:" + Format.getFile(iFormat), Format.getCodec(iFormat, cFormat)});
        if (iFormat == 0) {
            functions.add(input -> crf(crf));
            functions.add(input -> preset(preset));
        }
        return Pipeline.biLambda(functions, CMD::concat);
    }

    public static String[] lxcEncode(int iFormat, int cFormat) {
        return lxcEncode(iFormat, cFormat, 18, 0);
    }

    public static String[] pixelFormat(int pFormat) {
        return new String[]{"-pix_fmt", Format.getPixel(pFormat)};
    }

    public static String[] pixelFormat() {
        return pixelFormat(0);
    }

    public static String[] streamCopy(int iFormat){
        if(iFormat >= 0) return new String[]{"-c:" + Format.getFile(iFormat), "copy"};
        return new String[]{"-c", "copy"};
    }

    public static String[] streamCopy(){ return streamCopy(-1); }

    public static String[] addPadding(){ return new String[]{"-vf", "pad=iw:ih:(ow-iw)/2:(oh-ih)/2:color=black"}; }

    public static String[] bitRate(int iFormat, int bitrate, int bFormat){
        return new String[]{"-b:" + Format.getFile(iFormat), bitrate + Format.getBitUnit(bFormat)};
    }

    public static String[] sampleRate(int iFormat, int rate){ return new String[]{"-" + Format.getFile(iFormat) +"r", Integer.toString(rate)}; }

    public static String[] pickShortest(){ return new String[]{"-shortest"}; }

    public static String[] pickOneFrame(int quality){
        String sQuality = Integer.toString(quality%32);
        return new String[]{"-vframes", "1", "-q:v", sQuality};
    }

    public static String[] muxQueueSize(int size){  return new String[]{"-max_muxing_queue_size", Integer.toString(size)}; }
    public static String[] threadQueueSize(int size){  return new String[]{"-thread_queue_size", Integer.toString(size)}; }

    public static String[] loop() { return new String[]{"-loop", "1"}; }
}
