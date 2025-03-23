package edu.up.isgc.vgc.tools.ffmpeg;

import edu.up.isgc.vgc.Component;
import edu.up.isgc.vgc.graphic.Video;
import edu.up.isgc.vgc.tools.CMD;
import edu.up.isgc.vgc.tools.Pipeline;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class FFMPEG {
    private static String exePath = new File("Tools/FFMPEG/ffmpeg.exe").getAbsolutePath();
    private static String outPath = new File("Outputs").getAbsolutePath();

    public static void setExePath(String newPath) {
        FFMPEG.exePath = newPath;
    }

    public static void setOutputPath(String newPath) {
        FFMPEG.outPath = newPath;
    }

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
            functions.add(input -> FFMPEG.crf(crf));
            functions.add(input -> FFMPEG.preset(preset));
        }
        return Pipeline.biLambda(functions, CMD::concat);
    }

    public static String[] lxcEncode(int iFormat, int cFormat) {
        return lxcEncode(iFormat, cFormat, 18, 0);
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
                input -> new String[]{"-t", sDuration},
                input -> FFMPEG.lxcEncode(0, 0),
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

    public static String[] normalize(int amount, int iFormat, int cFormat, int targetFPS, String newSize, String[] inputFiles) {
        List<String> normalizedFiles = new ArrayList<>();

        for (int i = 0; i < inputFiles.length; i++) {
            String inputFile = inputFiles[i];
            String normalizedOutputPath = CMD.normalizePath(
                    outPath + File.separator + "normalized" + i + ".mp4"
            );
            String[] filter = new String[]{
                    Filter.sVideo(newSize, 0, 1),
                    Filter.setPTS(),
                    Filter.fps(targetFPS)
            };

            List<Function<String[], String[]>> functions = List.of(
                    input -> FFMPEG.input(inputFile),
                    input -> new String[]{"-vf", CMD.join(filter, ",")},
                    input -> FFMPEG.cPRate(targetFPS),
                    input -> FFMPEG.lxcEncode(iFormat, cFormat),
                    input -> FFMPEG.output(normalizedOutputPath)
            );

            String[] command = Pipeline.biLambda(functions, CMD::concat);
            String[] fullCommand = CMD.concat(new String[]{CMD.normalizePath(exePath), "-y"}, command);
            CMD.run(fullCommand);
            normalizedFiles.add(normalizedOutputPath);
        }

        return normalizedFiles.toArray(new String[0]);
    }

    public static String[] createGrid(ArrayList<Video> videos, String outputPath, int targetFPS) {
        int[] maxRes = Component.getMaxResolution();
        String maxWidth = Integer.toString(maxRes[0]);
        String maxHeight = Integer.toString(maxRes[1]);
        StringBuilder filterComplex = new StringBuilder();

        for (int i = 0; i < videos.size(); i++) {
            filterComplex.append(String.format(
                    "[%d:v]scale=%s:%s:force_original_aspect_ratio=decrease," +
                            "pad=%s:%s:(ow-iw)/2:(oh-ih)/2,fps=%s[v%d];",
                    i, maxWidth, maxHeight, maxWidth, maxHeight, targetFPS, i
            ));
        }

        int cols = (int) Math.ceil(Math.sqrt(videos.size()));
        List<String> positions = new ArrayList<>();
        List<String> inputs = new ArrayList<>();

        for (int i = 0; i < videos.size(); i++) {
            inputs.add("[v" + i + "]");
            int row = i / cols;
            int col = i % cols;
            String xPos = col == 0 ? "0" : "w" + (col - 1);
            String yPos = row == 0 ? "0" : "h" + (row - 1);
            positions.add(xPos + "_" + yPos);
        }

        filterComplex.append(String.join("", inputs))
                .append("xstack=inputs=").append(videos.size())
                .append(":layout=").append(String.join("|", positions))
                .append(",fps=").append(targetFPS).append("[vout]");

        List<Function<String[], String[]>> functions = List.of(
                input -> FFMPEG.inputMany(videos.stream().map(Video::getPath).toArray(String[]::new)),
                input -> new String[]{"-filter_complex", filterComplex.toString().replaceAll(";{2,}", ";")},
                input -> new String[]{"-map", "[vout]"},
                input -> new String[]{"-r", Integer.toString(targetFPS)},
                input -> FFMPEG.lxcEncode(0, 0, 18, 0),
                input -> FFMPEG.output(CMD.normalizePath(outputPath))
        );

        return Pipeline.biLambda(functions, CMD::concat);
    }

    public static void generateVideo(String videoFormat, String audioFormat, boolean addSubtitles, ArrayList<Component> components, String outputPath) {
        try {
            List<String> inputFiles = new ArrayList<>();
            ArrayList<Video> videoComponents = new ArrayList<>();
            int targetFPS = 30;

            for (int i = 0; i < components.size(); i++) {
                Component component = components.get(i);
                if (component.returnIFormat().equals("Image")) {
                    String imageVideoPath = outPath + File.separator + "image_" +
                            component.getPath().hashCode() + ".mp4";
                    String[] loopCommand = loopImg(5, Format.getCodec(0, 0), component.getPath());

                    String[] fullLoopCommand = CMD.concat(
                            new String[]{CMD.normalizePath(exePath), "-y"},
                            loopCommand
                    );
                    CMD.run(fullLoopCommand);

                    Video videoComponent = new Video(
                            component.getWidth(),
                            component.getHeight(),
                            component.getDate(),
                            5.0,
                            component.getType(),
                            CMD.normalizePath(imageVideoPath),
                            Format.getCodec(0, 0)
                    );
                    videoComponents.add(videoComponent);
                    inputFiles.add(imageVideoPath);
                    components.remove(i);
                    i--;
                } else if (component.returnIFormat().equals("Video")) {
                    videoComponents.add((Video) component);
                    inputFiles.add(component.getPath());
                }
            }

            int[] maxRes = Component.getMaxResolution();
            String[] normalizedFiles = normalize(
                    inputFiles.size(),
                    0,
                    0,
                    targetFPS,
                    maxRes[0] + ":" + maxRes[1],
                    inputFiles.stream().map(CMD::normalizePath).toArray(String[]::new)
            );

            String gridOutputPath = outPath + File.separator + "grid.mp4";
            String[] gridCommand = createGrid(videoComponents, gridOutputPath, targetFPS);
            String[] fullGridCommand = CMD.concat(
                    new String[]{CMD.normalizePath(exePath), "-y"},
                    gridCommand
            );
            CMD.run(fullGridCommand);

            String finalOutputPath = outPath + File.separator + outputPath;
            String listPath = outPath + File.separator + "list.txt";

            try (PrintWriter writer = new PrintWriter(listPath)) {
                for (String file : normalizedFiles) {
                    writer.println("file '" + file.replace("\"", "") + "'");
                }
                writer.println("file '" + gridOutputPath.replace("\"", "") + "'");
            }

            List<Function<String[], String[]>> functions = new ArrayList<>(List.of(
                    input -> new String[]{"-f", "concat", "-safe", "0", "-i", CMD.normalizePath(listPath)},
                    input -> new String[]{"-r", Integer.toString(targetFPS)},
                    input -> FFMPEG.lxcEncode(0, 0),
                    input -> FFMPEG.lxcEncode(1, 0)
            ));

            if (addSubtitles) {
                functions.add(1, input -> new String[]{"-vf", "subtitles=" + CMD.normalizePath(inputFiles.get(0))});
            }

            functions.add(input -> FFMPEG.output(CMD.normalizePath(finalOutputPath)));

            String[] command = Pipeline.biLambda(functions, CMD::concat);
            String[] fullCommand = CMD.concat(
                    new String[]{CMD.normalizePath(exePath), "-y"},
                    command
            );
            CMD.run(fullCommand);

            Files.deleteIfExists(Paths.get(listPath));
        } catch (Exception e) {
            e.printStackTrace();
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