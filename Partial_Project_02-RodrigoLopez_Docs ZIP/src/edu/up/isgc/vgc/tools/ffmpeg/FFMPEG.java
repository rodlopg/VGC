package edu.up.isgc.vgc.tools.ffmpeg;

import edu.up.isgc.vgc.Component;
import edu.up.isgc.vgc.graphic.Video;
import edu.up.isgc.vgc.tools.CMD;
import edu.up.isgc.vgc.tools.Pipeline;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FFMPEG {
    private static String exePath = new File("Tools/FFMPEG/ffmpeg.exe").getAbsolutePath();
    private static String outPath = new File("Outputs").getAbsolutePath();

    public static void setExePath(String newPath) {
        FFMPEG.exePath = newPath;
    }

    public static void setOutputPath(String newPath) {
        FFMPEG.outPath = newPath;
    }

    private static String quotePath(String path) {
        return "\"" + path.replace("\\", "/") + "\"";
    }

    public static String[] input(String filePath) {
        return new String[]{"-i", quotePath(filePath)};
    }

    public static String[] output(String filePath) {
        return new String[]{quotePath(filePath)};
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
        List<String> normalizedFiles = new ArrayList<>();

        for (int i = 0; i < inputFiles.length; i++) {
            String inputFile = inputFiles[i];
            String normalizedOutputPath = outPath + "/normalized" + i + ".mp4";
            String[] filter = new String[]{Filter.sVideo(newSize, 0, 1), Filter.setPTS()};

            List<Function<String[], String[]>> functions = List.of(
                    input -> FFMPEG.input(inputFile),
                    input -> new String[]{"-vf", CMD.join(filter, ",")},
                    input -> FFMPEG.lxcEncode(iFormat, cFormat, 18, 0),
                    input -> FFMPEG.output(normalizedOutputPath)
            );

            String[] command = Pipeline.biLambda(functions, CMD::concat);
            String[] fullCommand = CMD.concat(new String[]{quotePath(exePath), "-y"}, command);
            CMD.run(fullCommand);
            normalizedFiles.add(normalizedOutputPath);
        }

        return normalizedFiles.toArray(new String[0]);
    }

    public static String[] createGrid(ArrayList<Video> videos, String outputPath) {
        List<Filter> filters = new ArrayList<>();
        String maxWidth = Integer.toString(Component.getMaxResolution()[0]);
        String maxHeight = Integer.toString(Component.getMaxResolution()[1]);

        // 1. Create scale+pad filters
        for (int i = 0; i < videos.size(); i++) {
            String filter = String.format(
                    "[%d:v]scale=%s:%s:force_original_aspect_ratio=decrease," +
                            "pad=%s:%s:(ow-iw)/2:(oh-ih)/2[v%d];",
                    i, maxWidth, maxHeight, maxWidth, maxHeight, i
            );
            filters.add(new Filter(1, 0, 0, filter));
        }

        // 2. Create xstack filter
        StringBuilder xstackInputs = new StringBuilder();
        List<String> positions = new ArrayList<>();

        for (int i = 0; i < videos.size(); i++) {
            xstackInputs.append("[v").append(i).append("]");
            int row = i / 2;
            int col = i % 2;

            // Fixed format specifiers
            positions.add(String.format("%s_%s",
                    (col == 0) ? "0" : "w0",
                    (row == 0) ? "0" : "h0"));
        }

        String xstackFilter = String.format(
                "%sxstack=inputs=%d:layout=%s[vout];",
                xstackInputs.toString(),
                videos.size(),
                String.join("|", positions)
        );
        filters.add(new Filter(1, 0, 0, xstackFilter));

        // 3. Build command
        List<Function<String[], String[]>> functions = List.of(
                input -> FFMPEG.inputMany(videos.stream().map(v -> v.getPath()).toArray(String[]::new)),
                input -> Filter.complex(filters.toArray(new Filter[0])),
                input -> new String[]{"-map", "[vout]"},
                input -> new String[]{"-vsync", "2"},
                input -> FFMPEG.lxcEncode(0, 0, 18, 0),
                input -> FFMPEG.output(outputPath)
        );

        return Pipeline.biLambda(functions, CMD::concat);
    }

    public static void generateVideo(String videoFormat, String audioFormat, boolean addSubtitles, ArrayList<Component> components, String outputPath) {
        try {
            List<String> inputFiles = new ArrayList<>();
            ArrayList<Video> videoComponents = new ArrayList<>();

            for (int i = 0; i < components.size(); i++) {
                Component component = components.get(i);
                if (component.returnIFormat().equals("Image")) {
                    String imageVideoPath = outPath + "/image_" + component.getPath().hashCode() + ".mp4";
                    String[] loopCommand = loopImg(5, Format.getCodec(0, 0), component.getPath());

                    String[] fullLoopCommand = CMD.concat(new String[]{quotePath(exePath), "-y"}, loopCommand);
                    CMD.run(fullLoopCommand);

                    Video videoComponent = new Video(component.getWidth(), component.getHeight(),
                            component.getDate(), 5.0, component.getType(), imageVideoPath, Format.getCodec(0, 0));
                    videoComponents.add(videoComponent);
                    inputFiles.add(imageVideoPath);
                    components.remove(i);
                    i--;
                } else if (component.returnIFormat().equals("Video")) {
                    videoComponents.add((Video) component);
                    inputFiles.add(component.getPath());
                }
            }

            String[] normalizedFiles = normalize(inputFiles.size(), 0, 0,
                    Component.getMaxResolution()[0] + ":" + Component.getMaxResolution()[1],
                    inputFiles.toArray(new String[0]));

            String gridOutputPath = outPath + "/grid.mp4";
            String[] gridCommand = createGrid(videoComponents, gridOutputPath);
            String[] fullGridCommand = CMD.concat(new String[]{quotePath(exePath), "-y"}, gridCommand);
            CMD.run(fullGridCommand);

            String finalOutputPath = outPath + "/" + outputPath;
            List<Function<String[], String[]>> functions = new ArrayList<>(List.of(
                    input -> FFMPEG.input(normalizedFiles[0]),
                    input -> FFMPEG.input(gridOutputPath),
                    input -> new String[]{"-c:v", Format.getCodec(0, 0)},
                    input -> new String[]{"-c:a", Format.getCodec(1, 0)},
                    input -> FFMPEG.output(finalOutputPath)
            ));

            if (addSubtitles) {
                functions.add(input -> new String[]{"-vf", "subtitles=" + quotePath(inputFiles.get(0))});
            }

            String[] command = Pipeline.biLambda(functions, CMD::concat);
            String[] fullCommand = CMD.concat(new String[]{quotePath(exePath), "-y"}, command);
            CMD.run(fullCommand);
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