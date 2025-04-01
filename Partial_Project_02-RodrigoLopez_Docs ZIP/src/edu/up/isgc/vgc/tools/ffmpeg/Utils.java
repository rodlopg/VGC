package edu.up.isgc.vgc.tools.ffmpeg;

import edu.up.isgc.vgc.tools.CMD;
import edu.up.isgc.vgc.tools.Pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Utils {

    // Returns the input command for a file
    public static String[] input(String filePath) {
        return new String[]{"-i", CMD.normalizePath(filePath)};
    }

    // Returns the input command for concatenating a list of files
    public static String[] inputConcatList(String listFile) {
        return CMD.concat(new String[]{"-f", "concat", "-safe", "0"}, Utils.input(listFile));
    }

    // Returns the output command for a file path
    public static String[] output(String filePath) {
        return new String[]{CMD.normalizePath(filePath)};
    }

    // Returns the preset command for encoding
    public static String[] preset(int preset) {
        return new String[]{"-preset", Format.getPreset(preset)};
    }

    // Returns the time duration command for a video
    public static String[] time(int time) {
        return new String[]{"-t", Integer.toString(time)};
    }

    // Returns the CRF (Constant Rate Factor) command for encoding
    public static String[] crf(int crf) {
        return new String[]{"-crf", Format.getCRF(crf)};
    }

    // Returns the overwrite command to force overwrite output file
    public static String[] exeOverwrite() {
        return new String[]{FFMPEG.getExePath(), "-y"};
    }

    // Returns the map output command for a specific format
    public static String[] mapOutput(int iFormat) {
        return new String[]{"-map", "[out" + Format.getFile(iFormat) + "]"};
    }

    // Returns the map stream command for a specific format and stream
    public static String[] mapStream(int iFormat, int stream) {
        return new String[]{"-map", stream + ":" + Format.getFile(iFormat)};
    }

    // Returns the copy command for a specific format
    public static String[] copy(int iFormat) {
        return new String[]{"-c:" + Format.getFile(iFormat), "copy"};
    }

    // Returns the codec format command for a specific format
    public static String[] cFormat(int iFormat, int cFormat) {
        return new String[]{"-c:" + Format.getFile(iFormat), Format.getCodec(iFormat, cFormat)};
    }

    // Returns the frame rate command for a specific rate
    public static String[] cPRate(int newRate) {
        return new String[]{"-r", Integer.toString(newRate)};
    }

    // Returns the encoding commands for a specific format, codec, CRF, and preset
    public static String[] lxcEncode(int iFormat, int cFormat, int crf, int preset) {
        List<Function<String[], String[]>> functions = new ArrayList<>();
        functions.add(input -> new String[]{"-c:" + Format.getFile(iFormat), Format.getCodec(iFormat, cFormat)});
        if (iFormat == 0) {
            functions.add(input -> crf(crf));
            functions.add(input -> preset(preset));
        }
        return Pipeline.biLambda(functions, CMD::concat);
    }

    // Overloaded lxcEncode method with default CRF and preset values
    public static String[] lxcEncode(int iFormat, int cFormat) {
        return lxcEncode(iFormat, cFormat, 18, 0);
    }

    // Returns the pixel format command for a specific format
    public static String[] pixelFormat(int pFormat) {
        return new String[]{"-pix_fmt", Format.getPixel(pFormat)};
    }

    // Returns the default pixel format command
    public static String[] pixelFormat() {
        return pixelFormat(0);
    }

    // Returns the stream copy command for a specific format
    public static String[] streamCopy(int iFormat) {
        if (iFormat >= 0) return new String[]{"-c:" + Format.getFile(iFormat), "copy"};
        return new String[]{"-c", "copy"};
    }

    // Overloaded streamCopy method for default behavior
    public static String[] streamCopy() {
        return streamCopy(-1);
    }

    // Returns the padding command to add padding around the video
    public static String[] addPadding() {
        return new String[]{"-vf", "pad=iw:ih:(ow-iw)/2:(oh-ih)/2:color=black"};
    }

    // Returns the bitrate command for a specific format
    public static String[] bitRate(int iFormat, int bitrate, int bFormat) {
        return new String[]{"-b:" + Format.getFile(iFormat), bitrate + Format.getBitUnit(bFormat)};
    }

    // Returns the sample rate command for a specific format and rate
    public static String[] sampleRate(int iFormat, int rate) {
        return new String[]{"-" + Format.getFile(iFormat) + "r", Integer.toString(rate)};
    }

    // Returns the shortest input selection command
    public static String[] pickShortest() {
        return new String[]{"-shortest"};
    }

    // Returns the command to pick one frame with specific quality
    public static String[] pickOneFrame(int quality) {
        String sQuality = Integer.toString(quality % 32);
        return new String[]{"-vframes", "1", "-q:v", sQuality};
    }

    // Returns the mux queue size command
    public static String[] muxQueueSize(int size) {
        return new String[]{"-max_muxing_queue_size", Integer.toString(size)};
    }

    // Returns the thread queue size command
    public static String[] threadQueueSize(int size) {
        return new String[]{"-thread_queue_size", Integer.toString(size)};
    }

    // Returns the loop command to loop the input
    public static String[] loop() {
        return new String[]{"-loop", "1"};
    }
}
