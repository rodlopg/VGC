package edu.up.isgc.vgc.tools.ffmpeg;

import edu.up.isgc.vgc.Component;
import edu.up.isgc.vgc.graphic.Video;
import edu.up.isgc.vgc.tools.CMD;
import edu.up.isgc.vgc.tools.EXIF;
import edu.up.isgc.vgc.tools.OpenAI;
import edu.up.isgc.vgc.tools.Pipeline;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

/**
 * FFMPEG utility class for video processing operations
 */
public class FFMPEG {
    // Path to ffmpeg executable
    private static String exePath = new File("Tools/FFMPEG/ffmpeg.exe").getAbsolutePath();
    // Default output directory path
    private static String outPath = new File("Outputs").getAbsolutePath();

    // Setter for ffmpeg executable path
    public static void setExePath(String newPath) { exePath = newPath; }
    // Setter for output directory path
    public static void setOutputPath(String newPath) { outPath = newPath; }

    /**
     * Creates a video loop from a single image
     * @param duration Duration of the output video in seconds
     * @param filePath Path to the input image file
     * @param outputPath Path for the output video file
     * @return FFmpeg command as String array
     */
    public static String[] loopImg(int duration, String filePath, String outputPath) {
        String sDuration = Integer.toString(duration);

        List<Function<String[], String[]>> functions = List.of(
                input -> Utils.loop(),
                input -> Utils.input(filePath),
                input -> Utils.time(duration),
                input -> Filter.simple(new String[]{ "lavfi" }),
                input -> Utils.input("aevalsrc=0:d=" + sDuration),
                input -> Filter.simple(0, new String[]{ Filter.sVideo(Component.getMaxResolution()[0], -1, 1)}),
                input -> Utils.mapStream(0, 0),
                input -> Utils.mapStream(1, 1),
                input -> Utils.pickShortest(),
                input -> Utils.muxQueueSize(1024),
                input -> Utils.threadQueueSize(1024),
                input -> Utils.lxcEncode(0, 0),
                input -> Utils.cFormat(1,0),
                input -> Utils.pixelFormat(),
                input -> Utils.output(outputPath)
        );
        return Pipeline.biLambda(functions, CMD::concat);
    }

    /**
     * Creates input commands for multiple files
     * @param inputFiles Array of input file paths
     * @return FFmpeg input commands as String array
     */
    public static String[] inputMany(String[] inputFiles) {
        String[] inputCommand = new String[0];
        for (String file : inputFiles) {
            inputCommand = CMD.concat(inputCommand, Utils.input(file));
        }
        return inputCommand;
    }

    /**
     * Normalizes a video file to target FPS and resolution
     * @param inputFile Path to input video file
     * @param outputFile Path for output video file
     * @param targetFPS Target frames per second
     * @param newSize Target resolution as "width:height"
     * @return FFmpeg command as String array
     */
    public static String[] normalize(Component inputFile, String outputFile, int targetFPS, String newSize) {
        String[] sizeParts = newSize.split(":");
        int targetWidth = Integer.parseInt(sizeParts[0]);
        int targetHeight = Integer.parseInt(sizeParts[1]);
        int maxWidth = Component.getMaxResolution()[0];
        int maxHeight = Component.getMaxResolution()[1];
        int inputWidth = inputFile.getWidth();
        int inputHeight = inputFile.getHeight();

// Calculate scaling factor to ensure BOTH dimensions meet or exceed target
        double scaleWidth = (double) maxWidth / inputWidth;
        double scaleHeight = (double) maxHeight / inputHeight;
        double scaleFactor = Math.max(scaleWidth, scaleHeight);

// Calculate scaled dimensions maintaining aspect ratio
        int scaledWidth = (int) Math.ceil(inputWidth * scaleFactor);
        int scaledHeight = (int) Math.ceil(inputHeight * scaleFactor);

        String[] filter = new String[]{
                // Scale first to ensure we have enough pixels
                "scale=" + scaledWidth + ":" + scaledHeight,

                // Center crop to exact target dimensions
                "crop=" + maxWidth + ":" + maxHeight + ":(iw-" + maxWidth + ")/2:(ih-" + maxHeight + ")/2",

                // Add padding if final target is larger than max resolution
                "pad=" + targetWidth + ":" + targetHeight + ":(ow-iw)/2:(oh-ih)/2:color=black",

                Filter.setPTS(),
                Filter.fps(targetFPS)
        };

        List<Function<String[], String[]>> functions = List.of(
                input -> Utils.input(inputFile.getPath()),
                input -> Filter.simple(0, new String[]{ CMD.join(filter, ",") }),
                input -> Utils.cPRate(targetFPS),
                input -> Utils.lxcEncode(0, 0),
                input -> Utils.cFormat( 1, 0),
                input -> Utils.output(outputFile)
        );

        return Pipeline.biLambda(functions, CMD::concat);
    }

    /**
     * Creates a grid video from multiple input videos
     * @param inputFiles Array of input video paths
     * @param outputPath Path for output grid video
     * @param targetFPS Target frames per second
     * @param maxRes Maximum resolution as [width, height]
     * @return FFmpeg command as String array
     */
    public static String[] createGrid(String[] inputFiles, String outputPath, int targetFPS, int[] maxRes) {
        if (inputFiles == null || inputFiles.length == 0) {
            throw new IllegalArgumentException("No input files for grid creation");
        }

        // Validate all input files exist
        for(String file : inputFiles) {
            if(!new File(file).exists()) {
                throw new IllegalArgumentException("Missing input file: " + file);
            }
        }

        StringBuilder filterComplex = new StringBuilder();
        int numInputs = inputFiles.length;

        int cols = (int) Math.ceil(Math.sqrt(numInputs));
        int rows = (int) Math.ceil((double) numInputs / cols);

        String maxWidth = Integer.toString(maxRes[0]);
        String maxHeight = Integer.toString(maxRes[1]);

        List<String> positions = new ArrayList<>();
        List<String> inputs = new ArrayList<>();

        // Create scaling and padding filters for each input
        for (int i = 0; i < numInputs; i++) {
            filterComplex.append(String.format(
                    "[%d:v]scale=%d:%d:force_original_aspect_ratio=decrease," +
                            "scale=trunc(iw/2)*2:trunc(ih/2)*2[v%d];",
                    i, maxRes[0], maxRes[1], i
            ));
        }

        // Calculate grid positions
        for (int i = 0; i < numInputs; i++) {
            inputs.add("[v" + i + "]");
            int row = i / cols;
            int col = i % cols;
            String xPos = String.format("%d", col * maxRes[0]);
            String yPos = String.format("%d", row * maxRes[1]);
            positions.add(xPos + "_" + yPos);
        }

        // Combine all inputs into grid
        filterComplex.append(String.join("", inputs))
                .append("xstack=inputs=").append(numInputs)
                .append(":layout=").append(String.join("|", positions))
                .append(",fps=").append(targetFPS).append("[vout]");

        List<Function<String[], String[]>> functions = List.of(
                input -> inputMany(inputFiles),
                input -> new String[]{"-filter_complex", filterComplex.toString()},
                input -> new String[]{"-map", "[vout]"},
                input -> new String[]{"-r", Integer.toString(targetFPS)},
                input -> Utils.lxcEncode(0, 0, 18, 0),
                input -> Utils.output(CMD.normalizePath(outputPath))
        );

        return Pipeline.biLambda(functions, CMD::concat);
    }

    /**
     * Converts an image file to Base64 encoded string
     * @param imagePath Path to the image file
     * @return Base64 encoded string or null if conversion fails
     */
    private static String imageToBase64(String imagePath) {
        try {
            byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            System.err.println("Failed to convert image to Base64: " + e.getMessage());
            return null;
        }
    }

    /**
     * Generates a final video from components
     * @param outputFileName Name for the output video file
     * @param components List of components to include in video
     * @param addSubtitles Flag to enable subtitles (not currently used)
     */
    public static void generateVideo(String outputFileName, List<Component> components, boolean addSubtitles) {
        List<String> tempFiles = new ArrayList<>();
        try {
            int targetFPS = 30;
            int[] maxRes = Component.getMaxResolution();
            maxRes[0] = Math.min(maxRes[0], 1920);
            maxRes[1] = Math.min(maxRes[1], 1080);

            List<String> videoSegments = new ArrayList<>();
            Component[] postCards = Component.getPostCards();
            System.out.println("-- POSTCARD ARRAY: " + Arrays.toString(postCards));

            // Process first postcard (no audio)
            String firstPostcard = processPostcard(postCards[0], tempFiles, targetFPS);
            if(firstPostcard != null){
                videoSegments.add(firstPostcard);
                System.out.println("-- FIRST POSTCARD ADDED: " + firstPostcard);
            }

            // Process main components with individual audio
            List<String> componentVideos = processComponents(components, targetFPS, maxRes, tempFiles);
            videoSegments.addAll(componentVideos);

            // Create grid from normalized videos (no audio)
            String gridVideo = createGridVideo(componentVideos, targetFPS, maxRes, tempFiles);
            videoSegments.add(gridVideo);

            // Process second postcard (no audio)
            String lastPostcard = processPostcard(postCards[1], tempFiles, targetFPS);
            if(lastPostcard != null) {
                videoSegments.add(lastPostcard);
                System.out.println("-- SECOND POSTCARD ADDED: " + lastPostcard);
            }

            // Concatenate final video
            concatenateVideos(videoSegments, outputFileName, tempFiles);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Video generation failed: " + e.getMessage());
        } finally {
            cleanTempFiles(tempFiles);
        }
    }

    /**
     * Processes a postcard component into a video loop
     * @param postcard Postcard component to process
     * @param tempFiles List to track temporary files for cleanup
     * @return Path to generated video or null if processing fails
     */
    private static String processPostcard(Component postcard, List<String> tempFiles, int targetFPS) {
        if(postcard == null) return null;
        try {
            System.out.println("--- TRYING POSTCARD GEN");
            String outputPath = FFMPEG.getOutPath() + File.separator + "normalizedPostcard_" + UUID.randomUUID() + ".mp4";
            String prePath = FFMPEG.getOutPath() + File.separator + "postcard_" + UUID.randomUUID() + ".mp4";

            String[] command = loopImg(10, postcard.getPath(), prePath);
            String[] fullCommand = CMD.concat(new String[]{CMD.normalizePath(exePath), "-y"}, command);
            if(!CMD.run(fullCommand)) return null;
            System.out.println("--- POSTCARD PROCESS COMPLETED SUCCESSFULLY");

            Component tempComponent = new Video(Component.getMaxResolution()[0], Component.getMaxResolution()[1], Component.generateNow(), 10.0, "mp4", prePath, Format.getCodec(0,0));

            String[] fullCommandB = CMD.concat(Utils.exeOverwrite(),FFMPEG.normalize(tempComponent, outputPath, targetFPS, Component.getMaxResolution()[0]+":"+Component.getMaxResolution()[1]));
            if(!(CMD.run(fullCommandB))) return null;

            tempFiles.add(outputPath);
            tempFiles.add(prePath);
            return outputPath;
        } catch (Exception e) {
            System.err.println("Postcard processing failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Processes list of components into videos
     * @param components List of components to process
     * @param targetFPS Target frames per second
     * @param maxRes Maximum resolution as [width, height]
     * @param tempFiles List to track temporary files for cleanup
     * @return List of paths to processed videos
     */
    private static List<String> processComponents(List<Component> components, int targetFPS, int[] maxRes, List<String> tempFiles) {
        List<String> processedVideos = new ArrayList<>();
        for(Component component : components) {
            try {
                // Normalize video/loop image
                String processedPath = processMedia(component, targetFPS, maxRes, tempFiles);
                if(processedPath == null) continue;

                // Add audio description
                String finalVideo = addAudioDescription(component, processedPath, tempFiles);
                if(finalVideo != null) {
                    System.out.println("%%^%^^%^%^%^%^%^% Audio description added: " + finalVideo);
                    processedVideos.add(finalVideo);
                    tempFiles.add(finalVideo);
                }
            } catch (Exception e) {
                System.err.println("Component processing failed: " + e.getMessage());
            }
        }
        return processedVideos;
    }

    /**
     * Processes media (image or video) component
     * @param component Component to process
     * @param targetFPS Target frames per second
     * @param maxRes Maximum resolution as [width, height]
     * @param tempFiles List to track temporary files for cleanup
     * @return Path to processed media or null if processing fails
     */
    private static String processMedia(Component component, int targetFPS, int[] maxRes, List<String> tempFiles) {
        try {
            String outputPath = FFMPEG.getOutPath() + File.separator + "normalized_" + UUID.randomUUID() + ".mp4";

            if(component.returnIFormat().equals("Image")) {
                // Generate silent audio
                String silentAudio = FFMPEG.getOutPath() + File.separator + "silent_" + UUID.randomUUID() + ".aac";

                List<Function<String[], String[]>> preCommandA = List.of(
                        input -> Utils.exeOverwrite(),
                        input -> Filter.simple(new String[]{ "lavfi" }),
                        input -> Utils.input("aevalsrc=0:d=20"),
                        input -> Utils.cFormat(1, 0),
                        input -> Utils.bitRate(1, 128, 0),
                        input -> Utils.sampleRate(1, 44100),
                        input -> new String[]{ silentAudio }
                );

                String[] silentCommand = Pipeline.biLambda(preCommandA, CMD::concat);
                if(!CMD.run(silentCommand)) return null;

                // Create video with silent audio
                String prePath = FFMPEG.getOutPath() + File.separator + "normalized_" + UUID.randomUUID() + ".mp4";

                List<Function<String[], String[]>> preCommand = List.of(
                        input -> Utils.exeOverwrite(),
                        input -> Utils.loop(),
                        input -> FFMPEG.inputMany(new String[]{component.getPath(), silentAudio}),
                        input -> Filter.simple(0, new String[]{ Filter.sVideo(maxRes[0],-1, 1)} ),
                        input -> Utils.time(20),
                        input -> Utils.cFormat(0, 0),
                        input -> Utils.crf(18),
                        input -> Utils.preset(0),
                        input -> Utils.pickShortest(),
                        input -> new String[]{ prePath }
                );

                String[] finalCommand = Pipeline.biLambda(preCommand, CMD::concat);
                if(!CMD.run(finalCommand)) return null;

                tempFiles.add(silentAudio);

                Component tempComponent = component.copyTo(prePath);

                String[] command = FFMPEG.normalize(
                        tempComponent,
                        outputPath,
                        targetFPS,
                        maxRes[0] + ":" + maxRes[1]
                );
                String[] fullCommand = CMD.concat(Utils.exeOverwrite(), command);
                if(!CMD.run(fullCommand)) return null;

            } else {
                // Existing video normalization code
                String[] command = FFMPEG.normalize(
                        component,
                        outputPath,
                        targetFPS,
                        maxRes[0] + ":" + maxRes[1]
                );
                String[] fullCommand = CMD.concat(Utils.exeOverwrite(), command);
                if(!CMD.run(fullCommand)) return null;
            }

            tempFiles.add(outputPath);
            return outputPath;
        } catch (Exception e) {
            System.err.println("Media processing error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Adds audio description to a video
     * @param component Component containing the video
     * @param videoPath Path to the video file
     * @param tempFiles List to track temporary files for cleanup
     * @return Path to video with audio or original path if addition fails
     */
    private static String addAudioDescription(Component component, String videoPath, List<String> tempFiles) {
        try {
            // Extract frame for description
            String framePath = FFMPEG.getOutPath() + File.separator + "frame_" + UUID.randomUUID() + ".png";

            List<Function<String[], String[]>> preCommandA = List.of(
                    input -> Utils.exeOverwrite(),
                    input -> Utils.input(videoPath),
                    input -> Utils.pickOneFrame(2),
                    input -> new String[] {framePath}
            );

            String[] frameCommand = Pipeline.biLambda(preCommandA, CMD::concat);
            if(!CMD.run(frameCommand)) return videoPath;

            // Generate audio
            String audioPath = generateAudioFromFrame(framePath, tempFiles);
            if(audioPath == null) return videoPath;

            // Get audio duration
            String duration = getMediaDuration(audioPath);

            // Merge audio with video (match durations)
            String mergedPath = FFMPEG.getOutPath() + File.separator + "merged_" + UUID.randomUUID() + ".mp4";

            List<Function<String[], String[]>> preCommandB = List.of(
                    input -> Utils.exeOverwrite(),
                    input -> FFMPEG.inputMany(new String[]{videoPath, audioPath}),
                    input -> Utils.mapStream(0, 0),
                    input -> Utils.mapStream(1,1),
                    input -> Utils.cFormat(0,0),
                    input -> Utils.crf(18),
                    input -> Utils.preset(0),
                    input -> Utils.pixelFormat(0),
                    input -> Utils.cFormat(1,0),
                    input -> Utils.bitRate(1, 128, 0),
                    input -> Utils.sampleRate(1, 44100),
                    input -> Utils.pickShortest(),
                    input -> new String[]{ mergedPath }
            );

            String[] finalCommand = Pipeline.biLambda(preCommandB, CMD::concat);

            if(!CMD.run(finalCommand)) return videoPath;
            return mergedPath;
        } catch (Exception e) {
            System.err.println("Audio addition failed: " + e.getMessage());
            return videoPath;
        }
    }

    /**
     * Gets duration of a media file
     * @param filePath Path to media file
     * @return Duration as string or default "20.0" if cannot determine
     */
    private static String getMediaDuration(String filePath) {
        String[] command = CMD.concat(new String[]{exePath}, Utils.input(filePath));

        String output = CMD.expect(command);
        return output.contains("Duration:") ?
                output.split("Duration:")[1].split(",")[0].trim() : "20.0";
    }

    /**
     * Generates audio description from an image frame
     * @param framePath Path to image frame
     * @param tempFiles List to track temporary files for cleanup
     * @return Path to generated audio file or null if generation fails
     */
    private static String generateAudioFromFrame(String framePath, List<String> tempFiles) {
        try {
            String base64Image = imageToBase64(framePath);
            if(base64Image == null) return null;

            String description = OpenAI.describeImage(base64Image);
            if(description == null) return null;

            // Generate and trim audio
            String rawAudio = OpenAI.generateAudio(description, "audio_" + UUID.randomUUID());
            if(rawAudio == null) return null;

            String trimmedAudio = FFMPEG.getOutPath() + File.separator + "trimmed_" + UUID.randomUUID() + ".mp3";

            List<Function<String[], String[]>> preCommand = List.of(
                    input -> Utils.exeOverwrite(),
                    input -> Utils.input(rawAudio),
                    input -> Utils.time(20),
                    input -> Utils.streamCopy(),
                    input -> new String[]{trimmedAudio}
            );

            String[] trimCommand = Pipeline.biLambda(preCommand, CMD::concat);
            if(!CMD.run(trimCommand)) return null;

            tempFiles.add(framePath);
            tempFiles.add(rawAudio);
            tempFiles.add(trimmedAudio);

            System.out.println("&*&****** Trimmed audio description: " + trimmedAudio);
            return trimmedAudio;
        } catch (Exception e) {
            System.err.println("Audio generation failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates a grid video from multiple input videos
     * @param inputVideos List of input video paths
     * @param targetFPS Target frames per second
     * @param maxRes Maximum resolution as [width, height]
     * @param tempFiles List to track temporary files for cleanup
     * @return Path to generated grid video or null if creation fails
     */

    private static String createGridVideo(List<String> inputVideos, int targetFPS, int[] maxRes, List<String> tempFiles) {
        try {
            String gridPath = FFMPEG.getOutPath() + File.separator + "grid_" + UUID.randomUUID() + ".mp4";
            String[] command = createGrid(inputVideos.toArray(new String[0]), gridPath, targetFPS, maxRes);
            String[] fullCommand = CMD.concat(new String[]{CMD.normalizePath(exePath), "-y"}, command);
            if(!CMD.run(fullCommand)) throw new Exception("Grid creation failed");

            return gridPath;
        } catch (Exception e) {
            System.err.println("Grid creation failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Concatenates multiple videos into a single output
     * @param inputs List of input video paths
     * @param outputName Name for output video file
     * @param tempFiles List to track temporary files for cleanup
     */
    private static void concatenateVideos(List<String> inputs, String outputName, List<String> tempFiles) {
        try {
            // Step 1: Create concat list file
            String listFile = FFMPEG.getOutPath() + File.separator + "concat_list_" + UUID.randomUUID() + ".txt";
            try (PrintWriter writer = new PrintWriter(listFile)) {
                for (String path : inputs) {
                    if (path != null) writer.println("file '" + path.replace("'", "'\\''") + "'");
                }
            }

            // Step 2: Directly concatenate with re-encoding (no intermediate temp file)
            String finalOutput = FFMPEG.getOutPath() + File.separator + outputName;

            List<Function<String[], String[]>> command = List.of(
                    input -> Utils.exeOverwrite(),
                    input -> new String[]{"-f", "concat"},
                    input -> new String[]{"-safe", "0"},
                    input -> Utils.inputConcatList(listFile),

                    // Video processing
                    input -> new String[]{"-vf", "settb=AVTB,setpts=N/FRAME_RATE/TB"},
                    input -> new String[]{"-r", "30"},  // Force constant frame rate

                    // Audio processing
                    input -> new String[]{"-af", "aresample=async=1:first_pts=0"},

                    // Encoding settings
                    input -> new String[]{"-c:v", "libx264"},
                    input -> new String[]{"-preset", "fast"},
                    input -> new String[]{"-crf", "23"},  // Quality balance
                    input -> new String[]{"-pix_fmt", "yuv420p"},
                    input -> new String[]{"-c:a", "aac"},
                    input -> new String[]{"-b:a", "192k"},

                    // Metadata and streaming optimizations
                    input -> new String[]{"-movflags", "+faststart"},
                    input -> new String[]{"-video_track_timescale", "90000"},

                    input -> new String[]{finalOutput}
            );

            String[] fullCommand = Pipeline.biLambda(command, CMD::concat);

            if (!CMD.run(fullCommand)) {
                throw new Exception("Concatenation with re-encoding failed");
            }

            // Cleanup
            tempFiles.add(listFile);
        } catch (Exception e) {
            System.err.println("Video processing failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Cleans up temporary files
     * @param tempFiles List of temporary file paths to delete
     */
    private static void cleanTempFiles(List<String> tempFiles) {
        for (String path : tempFiles) {
            try {
                Files.deleteIfExists(Paths.get(path));
            } catch (Exception e) {
                System.err.println("Failed to delete temp file: " + path);
                e.printStackTrace();
            }
        }
    }

    // Getters and setters
    public static String getExePath() { return exePath; }
    public static String getOutPath() { return outPath; }
    public static void setOutPath(String outPath) { FFMPEG.outPath = outPath; }
}