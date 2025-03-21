package edu.up.isgc.vgc.tools.ffmpeg;

import edu.up.isgc.vgc.Component;
import edu.up.isgc.vgc.graphic.Video;
import edu.up.isgc.vgc.tools.CMD;
import edu.up.isgc.vgc.tools.Pipeline;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
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

    public static String[] cFormat(int iFormat, int cFormat) {
        return new String[]{"-c:" + Format.getFile(iFormat), Format.getCodec(iFormat, cFormat)};
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

    /**
     * Creates a video grid from the input videos.
     *
     * @param videos      The array of Video objects.
     * @param outputPath  The output file path for the grid video.
     * @return The FFMPEG command to create the video grid.
     */
    public static String[] createGrid(ArrayList<Video> videos, String outputPath) {
        // Step 1: Prepare the filter complex for the grid
        List<Filter> filters = new ArrayList<>();

        // Add scale filters for each video
        for (int i = 0; i < videos.size(); i++) {
            String scaleFilter = Filter.sVideo(Component.getMaxResolution()[0] + ":" + Component.getMaxResolution()[1], 0, 1);
            filters.add(new Filter(1, 0, 0, scaleFilter));
        }

        // Add xstack filter for the grid
        StringBuilder xstackInputs = new StringBuilder();
        for (int i = 0; i < videos.size(); i++) {
            xstackInputs.append("[v").append(i).append("]");
        }
        String xstackFilter = xstackInputs + "xstack=inputs=" + videos.size() + ":layout=0_0|w0_0|0_h0|w0_h0[v]";
        filters.add(new Filter(1, 0, 0, xstackFilter));

        ArrayList<String> inputFiles = new ArrayList<>();
        for(Video video : videos) { inputFiles.add(video.getPath()); }

        // Step 2: Prepare the FFMPEG command
        List<Function<String[], String[]>> functions = List.of(
                input -> new String[]{FFMPEG.getExePath(), "ffmpeg"},
                input -> FFMPEG.inputMany(inputFiles.toArray(new String[0])),
                input -> Filter.complex(filters.toArray(new Filter[0])),
        input -> FFMPEG.output(outputPath)
        );

        // Step 3: Construct the final command using Pipeline.biLambda
        return Pipeline.biLambda(functions, CMD::concat);
    }

    /**
     * Generates a final video from the input components.
     *
     * @param components    The array of components (images and videos).
     * @param outputPath    The output file path.
     * @param videoFormat   The video format (e.g., mp4).
     * @param audioFormat   The audio format (e.g., aac).
     * @param addSubtitles  Whether to add subtitles.
     */
    public static void generateVideo(Component[] components, String outputPath, String videoFormat, String audioFormat, boolean addSubtitles) {
        try {
            // Step 1: Prepare input files
            List<String> inputFiles = new ArrayList<>();
            ArrayList<Video> videoComponents = new ArrayList<>();

            for (Component component : components) {
                if (component.getType().equals("Image")) {
                    // Convert image to video using loopImg
                    String imageVideoPath = outPath + File.separator + "image_" + component.getPath().hashCode() + ".mp4";
                    String[] loopCommand = loopImg(5, "libx264", component.getPath());
                    CMD.run(loopCommand);

                    // Replace the image component with the new video component
                    Video videoComponent = new Video(component.getWidth(), component.getHeight(), component.getDate(), 5.0, "Video", imageVideoPath, "libx264");
                    videoComponents.add(videoComponent);
                    inputFiles.add(imageVideoPath);
                } else {
                    videoComponents.add((Video) component);
                    inputFiles.add(component.getPath());
                }
            }

            // Step 2: Normalize all videos to maxResolution
            String normalizedOutputPath = outPath + File.separator + "normalized.mp4";
            String[] normalizeCommand = normalize(inputFiles.size(), 0, 0, Component.getMaxResolution()[0] + ":" + Component.getMaxResolution()[1], inputFiles.toArray(new String[0]));
            CMD.run(normalizeCommand);

            // Step 3: Create a video grid
            String gridOutputPath = outPath + File.separator + "grid.mp4";
            String[] gridCommand = createGrid(videoComponents, gridOutputPath);
            CMD.run(gridCommand);

            // Step 4: Generate the final video
            String finalOutputPath = outPath + File.separator + outputPath;
            List<Function<String[], String[]>> functions = List.of(
                    input -> new String[]{FFMPEG.getExePath(), "ffmpeg"},
                    input -> FFMPEG.input(normalizedOutputPath),
                    input -> FFMPEG.input(gridOutputPath),
                    input -> new String[]{"-c:v", Format.getCodec(0, 0)}, // Apply video codec
                    input -> new String[]{"-c:a", Format.getCodec(1, 0)}, // Apply audio codec
                    input -> FFMPEG.output(finalOutputPath) // Set output file
            );

            // Add subtitles if enabled
            if (addSubtitles) {
                functions.add(input -> new String[]{"-vf", "subtitles=" + inputFiles.get(0)});
            }

            // Step 5: Construct the final command using Pipeline.biLambda
            String[] command = Pipeline.biLambda(functions, CMD::concat);

            // Step 6: Execute FFMPEG command using CMD.run
            CMD.run(command);

            System.out.println("Video generation process completed. Output: " + finalOutputPath);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error generating video: " + e.getMessage());
        }
    }

    public static String getExePath() {
        return exePath;
    }

    public static String getOutPath() {
        return outPath;
    }

    public static void setOutPath(String outPath) {
        FFMPEG.outPath = outPath;
    }
}