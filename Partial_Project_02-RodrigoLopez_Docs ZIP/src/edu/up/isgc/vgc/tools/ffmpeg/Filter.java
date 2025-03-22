package edu.up.isgc.vgc.tools.ffmpeg;

import edu.up.isgc.vgc.tools.CMD;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a video filter for FFMPEG.
 */
public class Filter {
    private static int fAmount = 0; // Tracks the number of filters
    private int amount, iFormat, stream, identifier;
    private String filter;

    public Filter(int amount, int iFormat, int stream, String filter) {
        this.setAmount(amount);
        this.setiFormat(iFormat);
        this.setStream(stream);
        this.setIdentifier(fAmount);
        Filter.setfAmount(Filter.getfAmount() + 1);
        this.setFilter(filter);
    }

    // Getters and setters
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public int getiFormat() { return iFormat; }
    public void setiFormat(int iFormat) { this.iFormat = iFormat; }
    public int getStream() { return stream; }
    public void setStream(int stream) { this.stream = stream; }
    public int getIdentifier() { return identifier; }
    public void setIdentifier(int identifier) { this.identifier = identifier; }
    public static int getfAmount() { return fAmount; }
    public static void setfAmount(int fAmount) { Filter.fAmount = fAmount; }
    public String getFilter() { return filter; }
    public void setFilter(String filter) { this.filter = filter; }

    /**
     * Generates a stream identifier for FFMPEG.
     */
    public static String getStream(int index, int iFormat, int stream) {
        String sIndex = Integer.toString(index);
        String sStream = Integer.toString(stream);
        String[] preResult = new String[]{sIndex, Format.getFile(iFormat), sStream};
        return "[" + CMD.join(preResult, ":") + "]";
    }

    /**
     * Creates a simple filter command.
     */
    public static String[] simple(int iFormat, String[] input) {
        return CMD.concat(new String[]{"-" + Format.getFile(iFormat) + "f"}, input);
    }

    /**
     * Adds a filter to a complex filter chain.
     */
    public static String addToComplex(int identifier, int index, int iFormat, int stream, String filter) {
        return getStream(index, iFormat, stream) + filter + "[" + Format.getFile(iFormat) + identifier + index + "]";
    }

    /**
     * Creates a complex filter command.
     */
    public static String[] complex(Filter[] filters) {
        List<String> list = new ArrayList<>();
        for (Filter f : filters) {
            for (int j = 0; j < f.getAmount(); j++) {
                list.add(Filter.addToComplex(f.getIdentifier(), j, f.getiFormat(), f.getStream(), f.getFilter()));
            }
        }
        String[] resultFilter = list.toArray(new String[0]);
        String finalFilter = Filter.formatFilter(CMD.join(resultFilter, ","));
        System.out.println(finalFilter);
        return new String[]{"-filter_complex", finalFilter};
    }

    /**
     * Formats a filter string for FFMPEG.
     */
    public static String formatFilter(String filter) {
        String[] filterArray = new String[]{"\"", filter, "\""};
        return CMD.join(filterArray, "");
    }

    /**
     * Generates a scale filter for video resizing.
     */
    public static String sVideo(String newSize, int forceRatio, int interpolation) {
        String size = "scale=" + newSize;
        String ratio = "force_original_aspect_ratio=" + (forceRatio == 0 ? "decrease" : "increase");
        String interp = (interpolation == 1 ? "flags=bicubic" : null);
        String[] parameters = new String[]{size, ratio, interp};
        return CMD.join(parameters, ":");
    }

    /**
     * Generates a setpts filter for adjusting timestamps.
     */
    public static String setPTS() {
        return "setpts=PTS-STARTPTS";
    }

    /**
     * Generates an fps filter for frame rate adjustment.
     */
    public static String fps(int fps) {
        return "fps=" + fps;
    }
}