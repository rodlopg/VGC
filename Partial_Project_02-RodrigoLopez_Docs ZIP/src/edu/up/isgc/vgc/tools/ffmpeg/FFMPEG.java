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

            List<String> videoPaths = new ArrayList<>();
            List<String> audioPaths = new ArrayList<>();
            Component[] postCardComponents = Component.getPostCards();

            // Process first postcard
            String firstPostcardPath = null;
            if(postCardComponents[0] != null) {
                String outputPath = outPath + File.separator + "postcard_start_" + UUID.randomUUID() + ".mp4";
                String[] loopCommand = loopImg(5, postCardComponents[0].getPath(), outputPath);
                String[] fullCommand = CMD.concat(new String[]{CMD.normalizePath(exePath), "-y"}, loopCommand);
                if (!CMD.run(fullCommand)) {
                    throw new RuntimeException("Failed to create postcard video: " + postCardComponents[0].getPath());
                }
                firstPostcardPath = outputPath;
                tempFiles.add(outputPath);

                // Capture screenshot
                String framePath = outPath + File.separator + "postcard_start_frame_" + UUID.randomUUID() + ".png";
                String[] frameCommand = {
                        exePath, "-y", "-i", outputPath,
                        "-vframes", "1", "-q:v", "2", framePath
                };
                if (CMD.run(frameCommand)) {
                    tempFiles.add(framePath);
                }
            }

            // Process main components
            List<String> componentVideoPaths = new ArrayList<>();
            for (Component component : components) {
                String processedVideoPath = null;

                if (component.returnIFormat().equals("Video")) {
                    // Normalize video
                    String normalizedVideoPath = outPath + File.separator + "normalized_" + UUID.randomUUID() + ".mp4";
                    String[] normalizeCommand = normalize(
                            component.getPath(),
                            normalizedVideoPath,
                            targetFPS,
                            maxRes[0] + ":" + maxRes[1]
                    );
                    String[] fullNormalizeCommand = CMD.concat(
                            new String[]{CMD.normalizePath(exePath), "-y"},
                            normalizeCommand
                    );
                    if (!CMD.run(fullNormalizeCommand)) {
                        throw new RuntimeException("Normalization failed for: " + component.getPath());
                    }
                    processedVideoPath = normalizedVideoPath;
                    tempFiles.add(normalizedVideoPath);
                } else if (component.returnIFormat().equals("Image")) {
                    // Process image as looped video
                    String loopedPath = outPath + File.separator + "looped_" + UUID.randomUUID() + ".mp4";
                    String[] loopCommand = loopImg(5, component.getPath(), loopedPath);
                    String[] fullCommand = CMD.concat(new String[]{CMD.normalizePath(exePath), "-y"}, loopCommand);
                    if (!CMD.run(fullCommand)) {
                        throw new RuntimeException("Failed to create looped video: " + component.getPath());
                    }
                    processedVideoPath = loopedPath;
                    tempFiles.add(loopedPath);
                }

                if (processedVideoPath != null) {
                    // Capture screenshot
                    String framePath = outPath + File.separator + "frame_" + UUID.randomUUID() + ".png";
                    String[] frameCommand = {
                            exePath, "-y", "-i", processedVideoPath,
                            "-vframes", "1", "-q:v", "2", framePath
                    };
                    if (CMD.run(frameCommand)) {
                        // Generate audio description
                        String base64Frame = imageToBase64(framePath);
                        if(base64Frame != null) {
                            String description = OpenAI.describeImage(base64Frame);
                            if(description != null) {
                                String audioPath = OpenAI.generateAudio(description, "audio_" + UUID.randomUUID());
                                if(audioPath != null) {
                                    audioPaths.add(audioPath);
                                    // Merge audio with video
                                    String mergedPath = outPath + File.separator + "merged_" + UUID.randomUUID() + ".mp4";
                                    String[] mergeCommand = {
                                            exePath, "-y", "-i", processedVideoPath,
                                            "-i", audioPath, "-c:v", "copy", "-c:a", "aac",
                                            "-shortest", mergedPath
                                    };
                                    if (CMD.run(mergeCommand)) {
                                        componentVideoPaths.add(mergedPath);
                                        tempFiles.add(mergedPath);
                                    }
                                }
                            }
                        }
                        tempFiles.add(framePath);
                    }
                }
            }

            // Process second postcard
            String secondPostcardPath = null;
            if(postCardComponents[1] != null) {
                String outputPath = outPath + File.separator + "postcard_end_" + UUID.randomUUID() + ".mp4";
                String[] loopCommand = loopImg(5, postCardComponents[1].getPath(), outputPath);
                String[] fullCommand = CMD.concat(new String[]{CMD.normalizePath(exePath), "-y"}, loopCommand);
                if (!CMD.run(fullCommand)) {
                    throw new RuntimeException("Failed to create postcard video: " + postCardComponents[1].getPath());
                }
                secondPostcardPath = outputPath;
                tempFiles.add(outputPath);

                // Capture screenshot
                String framePath = outPath + File.separator + "postcard_end_frame_" + UUID.randomUUID() + ".png";
                String[] frameCommand = {
                        exePath, "-y", "-i", outputPath,
                        "-vframes", "1", "-q:v", "2", framePath
                };
                if (CMD.run(frameCommand)) {
                    tempFiles.add(framePath);
                }
            }

            // Build final sequence
            List<String> finalVideoPaths = new ArrayList<>();
            if(firstPostcardPath != null) finalVideoPaths.add(firstPostcardPath);

            // Create grid from components
            String gridPath = outPath + File.separator + "grid_" + UUID.randomUUID() + ".mp4";
            String[] gridCommand = createGrid(componentVideoPaths.toArray(new String[0]), gridPath, targetFPS, maxRes);
            String[] fullGridCommand = CMD.concat(new String[]{CMD.normalizePath(exePath), "-y"}, gridCommand);
            if (!CMD.run(fullGridCommand)) {
                throw new RuntimeException("Failed to create video grid");
            }
            finalVideoPaths.add(gridPath);
            tempFiles.add(gridPath);

            if(secondPostcardPath != null) finalVideoPaths.add(secondPostcardPath);

            // Mix audio
            String audioMixPath = null;
            if (!audioPaths.isEmpty()) {
                audioMixPath = outPath + File.separator + "mixed_audio_" + UUID.randomUUID() + ".mp3";
                List<String> mixCommandList = new ArrayList<>();
                StringBuilder filterComplex = new StringBuilder();

                mixCommandList.add(exePath);
                mixCommandList.add("-y");
                for (String audioPath : audioPaths) {
                    mixCommandList.add("-i");
                    mixCommandList.add(audioPath);
                }

                for (int i = 0; i < audioPaths.size(); i++) {
                    filterComplex.append("[").append(i).append(":a]");
                }
                filterComplex.append("amix=inputs=").append(audioPaths.size()).append("[aout]");

                mixCommandList.add("-filter_complex");
                mixCommandList.add(filterComplex.toString());
                mixCommandList.add("-map");
                mixCommandList.add("[aout]");
                mixCommandList.add("-c:a");
                mixCommandList.add("libmp3lame");
                mixCommandList.add(audioMixPath);

                if (!CMD.run(mixCommandList.toArray(new String[0]))) {
                    throw new RuntimeException("Failed to mix audio tracks");
                }
                tempFiles.add(audioMixPath);
            }

            // Concatenate final video
            String listPath = outPath + File.separator + "list_" + UUID.randomUUID() + ".txt";
            try (PrintWriter writer = new PrintWriter(listPath)) {
                for (String path : finalVideoPaths) {
                    writer.println("file '" + path.replace("'", "'\\''") + "'");
                }
            }
            tempFiles.add(listPath);

            String finalOutput = outPath + File.separator + outputFileName;
            List<String> concatCommand = new ArrayList<>();
            Collections.addAll(concatCommand, exePath, "-y", "-f", "concat", "-safe", "0", "-i", listPath);

            // Modified section: Remove stream copy when using filters
            if (audioMixPath != null) {
                Collections.addAll(concatCommand, "-i", audioMixPath);
                Collections.addAll(concatCommand, lxcEncode(0, 0)); // Add video encoding
                Collections.addAll(concatCommand, "-c:a", "aac");
                Collections.addAll(concatCommand, "-map", "0:v", "-map", "1:a");
            } else {
                Collections.addAll(concatCommand, lxcEncode(0, 0)); // Add video encoding
                Collections.addAll(concatCommand, "-c:a", "copy");
            }

            // Move filter before output
            Collections.addAll(concatCommand, "-vf", "pad=iw:ih:(ow-iw)/2:(oh-ih)/2:color=black");
            Collections.addAll(concatCommand, finalOutput);

            if (!CMD.run(concatCommand.toArray(new String[0]))) {
                throw new RuntimeException("Failed to concatenate videos");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Video generation failed: " + e.getMessage());
        } finally {
            cleanTempFiles(tempFiles);
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