package edu.up.isgc.vgc.tools.ffmpeg;

import edu.up.isgc.vgc.Component;
import edu.up.isgc.vgc.tools.CMD;
import edu.up.isgc.vgc.tools.Pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Handles FFMPEG commands for video processing.
 */
public class FFMPEG {
    private static String exePath = "Tools/FFMPEG/ffmpeg.exe";
    private static String outPath = "Outputs";

    public static void setExePath(String newPath) {
        FFMPEG.exePath = newPath;
    }

    public static void setOutputPath(String newPath) {
        FFMPEG.outPath = newPath;
    }

    public static String[] input(String filePath) {
        return new String[]{"-i", filePath};
    }

    public static String[] output(String filePath) {
        return new String[]{filePath};
    }

    public static String[] preset(int preset) {
        return new String[]{"-preset", Format.getPreset(preset)};
    }

    public static String[] crf(int crf) {
        return new String[]{"-crf", Format.getCRF(crf)};
    }

    public static String[] mapOutput(int iFormat) {
        return new String[]{"-map", "[out" + Format.getFile(iFormat) + "]"};
    }

    public static String[] copy(int iFormat) {
        return new String[]{"-c:" + Format.getFile(iFormat), "copy"};
    }

    public static String[] cFormat(int format) {
        return new String[]{"-c", Format.getCodec(0, format)};
    }

    public static String[] cPRate(String newRate) {
        return new String[]{"-r", newRate};
    }

    public static String[] lxcEncode(int iFormat, int cFormat, int crf, int preset) {
        if (iFormat != 0) {
            return lxcEncode(iFormat, cFormat);
        } else {
            List<Function<String[], String[]>> functions = List.of(
                    input -> new String[]{"-c:" + Format.getFile(iFormat)},
                    input -> new String[]{Format.getCodec(iFormat, cFormat)},
                    input -> FFMPEG.crf(crf),
                    input -> FFMPEG.preset(preset)
            );
            return Pipeline.biLambda(functions, CMD::concat);
        }
    }

    public static String[] lxcEncode(int iFormat, int cFormat) {
        if (iFormat != 0) {
            List<Function<String[], String[]>> functions = List.of(
                    input -> new String[]{"-c:" + Format.getFile(iFormat)},
                    input -> new String[]{Format.getCodec(iFormat, cFormat)}
            );
            return Pipeline.biLambda(functions, CMD::concat);
        } else {
            return lxcEncode(iFormat, cFormat, 2, 5);
        }
    }

    public static String[] cCodec(int iFormat, String newCodec) {
        return new String[]{"-codec:" + Format.getFile(iFormat), newCodec};
    }

    public static String[] pixelFormat(int pFormat) {
        return new String[]{"-pix_fmt", Format.getPixel(pFormat)};
    }

    public static String[] pixelFormat() {
        return FFMPEG.pixelFormat(0);
    }

    public static String[] loopImg(int duration, String cFormat, String filePath) {
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

    public static String[] inputMany(String[] inputFiles) {
        String[] inputCommand = new String[0];
        for (String file : inputFiles) {
            inputCommand = CMD.concat(inputCommand, FFMPEG.input(file));
        }
        return inputCommand;
    }

    public static String[] normalize(int amount, int iFormat, int cFormat, String newSize, String[] inputFiles) {
        String[] filter = new String[]{Filter.sVideo(newSize, 0, 1), Filter.setPTS()};
        Filter[] filters = new Filter[]{new Filter(amount, iFormat, 0, CMD.join(filter, ","))};
        List<Function<String[], String[]>> functions = List.of(
                input -> FFMPEG.inputMany(inputFiles),
                input -> Filter.complex(filters),
                input -> FFMPEG.mapOutput(0),
                input -> FFMPEG.mapOutput(1),
                input -> FFMPEG.lxcEncode(iFormat, cFormat, 18, 8),
                input -> new String[]{"-c:" + Format.getFile(1), Format.getCodec(1, 0)},
                input -> new String[]{"-b:" + Format.getFile(iFormat), Format.bitRate(192, 0)},
                input -> new String[]{"output.something"}
        );
        return Pipeline.biLambda(functions, CMD::concat);
    }

    public static void generateVideo(List<Component> components, String outputPath, String videoFormat, String audioFormat, boolean addSubtitles) {
        try {
            // Step 1: Prepare input files
            List<String> inputFiles = new ArrayList<>();
            for (Component component : components) {
                inputFiles.add(component.getPath());
            }

            // Step 2: Prepare FFMPEG command
            List<String> command = new ArrayList<>();
            command.add(exePath); // FFMPEG executable path

            // Add input files
            for (String inputFile : inputFiles) {
                command.add("-i");
                command.add(inputFile);
            }

            // Apply video format and codec
            command.add("-c:v");
            command.add(Format.getCodec(0, 0)); // Default video codec (libx264)

            // Apply audio format and codec
            command.add("-c:a");
            command.add(Format.getCodec(1, 0)); // Default audio codec (aac)

            // Add subtitles if enabled
            if (addSubtitles) {
                command.add("-vf");
                command.add("subtitles=" + inputFiles.get(0)); // Add subtitles from the first input file
            }

            // Set output file
            command.add(outputPath);

            // Step 3: Execute FFMPEG command using CMD.run
            String[] commandArray = command.toArray(new String[0]);
            CMD.run(commandArray);

            System.out.println("Video generation process completed.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error generating video: " + e.getMessage());
        }
    }
}