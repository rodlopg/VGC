package edu.up.isgc.vgc.tools.ffmpeg;

import edu.up.isgc.vgc.Component;
import edu.up.isgc.vgc.tools.CMD;
import edu.up.isgc.vgc.tools.OpenAI;
import edu.up.isgc.vgc.tools.Pipeline;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.*;
import java.util.function.Function;

public class FFMPEG {
    private static String exePath = new File("Tools/FFMPEG/ffmpeg.exe").getAbsolutePath();
    private static String outPath = new File("Outputs").getAbsolutePath();

    public static void setExePath(String newPath) { exePath = newPath; }
    public static void setOutputPath(String newPath) { outPath = newPath; }

    public static String[] input(String filePath) {
        return new String[]{"-i", CMD.normalizePath(filePath)};
    }

    public static String[] output(String filePath) {
        return new String[]{CMD.normalizePath(filePath)};
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

    public static String[] loopImg(int duration, String filePath, String outputPath) {
        String sDuration = Integer.toString(duration);
        List<Function<String[], String[]>> functions = List.of(
                input -> new String[]{"-loop", "1"},
                input -> input(filePath),
                input -> new String[]{"-t", sDuration},
                input -> new String[]{"-f", "lavfi", "-i", "aevalsrc=0:d=" + sDuration},
                input -> new String[]{"-vf", "scale="+ Component.getMaxResolution()[0] +":-1:flags=lanczos"},
                input -> new String[]{"-map", "0:v"},
                input -> new String[]{"-map", "1:a"},
                input -> new String[]{"-shortest"},
                input -> new String[]{"-max_muxing_queue_size", "1024"},
                input -> new String[]{"-thread_queue_size", "1024"},
                input -> lxcEncode(0, 0),
                input -> new String[]{"-c:a", "aac"},
                input -> pixelFormat(),
                input -> output(outputPath)
        );
        return Pipeline.biLambda(functions, CMD::concat);
    }

    public static String[] inputMany(String[] inputFiles) {
        String[] inputCommand = new String[0];
        for (String file : inputFiles) {
            inputCommand = CMD.concat(inputCommand, input(file));
        }
        return inputCommand;
    }

    public static String[] normalize(String inputFile, String outputFile, int targetFPS, String newSize) {
        String[] sizeParts = newSize.split(":");
        int targetWidth = Integer.parseInt(sizeParts[0]);
        int targetHeight = Integer.parseInt(sizeParts[1]);

        // Safe scaling and cropping
        String[] filter = new String[]{
                "scale=" + Component.getMaxResolution()[0] + ":-1:force_original_aspect_ratio=decrease,crop=" + Component.getMaxResolution()[0] + ":" + Component.getMaxResolution()[1],
                "pad=" + targetWidth + ":" + targetHeight + ":(ow-iw)/2:(oh-ih)/2:color=black",
                Filter.setPTS(),
                Filter.fps(targetFPS)
        };

        List<Function<String[], String[]>> functions = List.of(
                input -> input(inputFile),
                input -> new String[]{"-vf", CMD.join(filter, ",")},
                input -> cPRate(targetFPS),
                input -> lxcEncode(0, 0),
                input -> new String[]{"-c:a", "aac"},
                input -> output(outputFile)
        );

        return Pipeline.biLambda(functions, CMD::concat);
    }

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

        List<String> positions = new ArrayList<>();
        List<String> inputs = new ArrayList<>();

        for (int i = 0; i < numInputs; i++) {
            filterComplex.append(String.format(
                    "[%d:v]scale=%d:%d:force_original_aspect_ratio=decrease:flags=bicubic:force_divisible_by=2," +
                            "pad=%d:%d:(ow-iw)/2:(oh-ih)/2,fps=%d[v%d];",
                    i, maxRes[0], maxRes[1], maxRes[0], maxRes[1], targetFPS, i
            ));
        }

        for (int i = 0; i < numInputs; i++) {
            inputs.add("[v" + i + "]");
            int row = i / cols;
            int col = i % cols;
            String xPos = String.format("%d", col * maxRes[0]);
            String yPos = String.format("%d", row * maxRes[1]);
            positions.add(xPos + "_" + yPos);
        }

        filterComplex.append(String.join("", inputs))
                .append("xstack=inputs=").append(numInputs)
                .append(":layout=").append(String.join("|", positions))
                .append(",fps=").append(targetFPS).append("[vout]");

        List<Function<String[], String[]>> functions = List.of(
                input -> inputMany(inputFiles),
                input -> new String[]{"-filter_complex", filterComplex.toString()},
                input -> new String[]{"-map", "[vout]"},
                input -> new String[]{"-r", Integer.toString(targetFPS)},
                input -> lxcEncode(0, 0, 18, 0),
                input -> output(CMD.normalizePath(outputPath))
        );

        return Pipeline.biLambda(functions, CMD::concat);
    }

    private static String imageToBase64(String imagePath) {
        try {
            byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            System.err.println("Failed to convert image to Base64: " + e.getMessage());
            return null;
        }
    }

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
            String firstPostcard = processPostcard(postCards[0], tempFiles);
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
            String lastPostcard = processPostcard(postCards[1], tempFiles);
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

    private static String processPostcard(Component postcard, List<String> tempFiles) {
        if(postcard == null) return null;
        try {
            System.out.println("--- TRYING POSTCARD GEN");
            String outputPath = outPath + File.separator + "postcard_" + UUID.randomUUID() + ".mp4";
            String[] command = loopImg(5, postcard.getPath(), outputPath);
            String[] fullCommand = CMD.concat(new String[]{CMD.normalizePath(exePath), "-y"}, command);
            if(!CMD.run(fullCommand)) return null;
            System.out.println("--- POSTCARD PROCESS COMPLETED SUCCESSFULLY");
            tempFiles.add(outputPath);
            return outputPath;
        } catch (Exception e) {
            System.err.println("Postcard processing failed: " + e.getMessage());
            return null;
        }
    }

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

    private static String processMedia(Component component, int targetFPS, int[] maxRes, List<String> tempFiles) {
        try {
            String outputPath = outPath + File.separator + "normalized_" + UUID.randomUUID() + ".mp4";
            String outputPathLoopedImg = component.getPath();
            if(component.returnIFormat().equals("Image")) {
                outputPathLoopedImg = outPath + File.separator + "looped_" + UUID.randomUUID() + ".mp4";
                String[] command = loopImg(5, component.getPath(), outputPathLoopedImg);
                String[] fullCommand = CMD.concat(new String[]{
                        CMD.normalizePath(exePath), "-y"
                }, command);

                if(!CMD.run(fullCommand)) {
                    throw new Exception("Loop creation failed for: " + component.getPath());
                }
            }

            String[] commandN = normalize(
                    outputPathLoopedImg,
                    outputPath,
                    targetFPS,
                    maxRes[0] + ":" + maxRes[1]
            );

            // Add input validation
            if(!new File(component.getPath()).exists()) {
                throw new IOException("Source file not found: " + component.getPath());
            }

            String[] fullCommandN = CMD.concat(new String[]{
                    CMD.normalizePath(exePath), "-y"
            }, commandN);

            if(!CMD.run(fullCommandN)) {
                throw new Exception("Normalization failed for: " + component.getPath());
            }

            if(outputPath != null && new File(outputPath).exists()) {
                tempFiles.add(outputPath);
                tempFiles.add(outputPathLoopedImg);
                return outputPath;
            }

            return null;
        } catch (Exception e) {
            System.err.println("Media processing error: " + e.getMessage());
            return null;
        }
    }

    private static String addAudioDescription(Component component, String videoPath, List<String> tempFiles) {
        try {
            // Extract frame for description
            String framePath = outPath + File.separator + "frame_" + UUID.randomUUID() + ".png";
            String[] frameCommand = {exePath, "-y", "-i", videoPath, "-vframes", "1", "-q:v", "2", framePath};
            if(!CMD.run(frameCommand)) return videoPath;

            // Generate audio
            System.out.println("#### ABOUT TO GENERATE AUDIO DESCRIPTION");
            String audioPath = generateAudioFromFrame(framePath, tempFiles);
            if(audioPath == null) return videoPath;

            System.out.println("!!!!!!!!!! AUDIO DESCRIPTION GENERATED");
            // Merge audio with video
            String mergedPath = outPath + File.separator + "merged_" + UUID.randomUUID() + ".mp4";
            String[] mergeCommand = {
                    exePath, "-y", "-i", videoPath, "-i", audioPath,
                    "-c:v", "copy", "-c:a", "aac", "-shortest", mergedPath
            };
            if(!CMD.run(mergeCommand)) return videoPath;

            System.out.println("$$$$$$$$ Audio merged SUCCESFULLY");
            return mergedPath;
        } catch (Exception e) {
            System.err.println("Audio addition failed: " + e.getMessage());
            return videoPath;
        }
    }

    private static String generateAudioFromFrame(String framePath, List<String> tempFiles) {
        try {
            String base64Image = imageToBase64(framePath);
            if(base64Image == null) return null;

            String description = OpenAI.describeImage(base64Image);
            if(description == null) return null;

            // Generate and trim audio
            String rawAudio = OpenAI.generateAudio(description, "audio_" + UUID.randomUUID());
            if(rawAudio == null) return null;

            String trimmedAudio = outPath + File.separator + "trimmed_" + UUID.randomUUID() + ".mp3";
            String[] trimCommand = {exePath, "-y", "-i", rawAudio, "-t", "20", "-c", "copy", trimmedAudio};
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

    private static String createGridVideo(List<String> inputVideos, int targetFPS, int[] maxRes, List<String> tempFiles) {
        try {
            String gridPath = outPath + File.separator + "grid_" + UUID.randomUUID() + ".mp4";
            String[] command = createGrid(inputVideos.toArray(new String[0]), gridPath, targetFPS, maxRes);
            String[] fullCommand = CMD.concat(new String[]{CMD.normalizePath(exePath), "-y"}, command);
            if(!CMD.run(fullCommand)) throw new Exception("Grid creation failed");

            tempFiles.add(gridPath);
            return gridPath;
        } catch (Exception e) {
            System.err.println("Grid creation failed: " + e.getMessage());
            return null;
        }
    }

    private static void concatenateVideos(List<String> inputs, String outputName, List<String> tempFiles) {
        try {
            // Step 1: Concatenate without filters
            String tempOutput = outPath + File.separator + "temp_" + UUID.randomUUID() + ".mp4";
            String listFile = outPath + File.separator + "concat_list_" + UUID.randomUUID() + ".txt";

            try (PrintWriter writer = new PrintWriter(listFile)) {
                for(String path : inputs) {
                    if(path != null) writer.println("file '" + path.replace("'", "'\\''") + "'");
                }
            }

            String[] concatCommand = {
                    exePath, "-y", "-f", "concat", "-safe", "0", "-i", listFile,
                    "-c", "copy", tempOutput
            };

            if(!CMD.run(concatCommand)) throw new Exception("Concatenation failed");

            // Step 2: Apply padding filter
            String finalOutput = outPath + File.separator + outputName;
            String[] fCommand = {
                    exePath, "-y", "-i", tempOutput,
                    "-vf", "pad=iw:ih:(ow-iw)/2:(oh-ih)/2:color=black"
            };

            List<Function<String[], String[]>> filterCommands = List.of(
                    input -> fCommand,
                    input -> lxcEncode(0, 0),
                    input -> new String[] {"-c:a", "copy", finalOutput}
            );

            String[] filterCommand = Pipeline.biLambda(filterCommands, CMD::concat);

            if(!CMD.run(filterCommand)) throw new Exception("Filter application failed");

            tempFiles.add(listFile);
            tempFiles.add(tempOutput);
        } catch (Exception e) {
            System.err.println("Video processing failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }


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

    public static String getExePath() { return exePath; }
    public static String getOutPath() { return outPath; }
    public static void setOutPath(String outPath) { FFMPEG.outPath = outPath; }
}