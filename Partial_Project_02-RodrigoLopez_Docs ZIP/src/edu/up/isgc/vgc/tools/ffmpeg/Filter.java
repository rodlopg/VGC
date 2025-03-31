package edu.up.isgc.vgc.tools.ffmpeg;

import edu.up.isgc.vgc.tools.CMD;
import java.util.ArrayList;
import java.util.List;

public class Filter {
    private static int fAmount = 0;
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

    public static String getStream(int index, int iFormat, int stream) {
        String sIndex = Integer.toString(index);
        String sStream = Integer.toString(stream);
        String[] preResult = new String[]{sIndex, Format.getFile(iFormat), sStream};
        return "[" + CMD.join(preResult, ":") + "]";
    }

    public static String[] simple(int iFormat, String[] input) {
        if(iFormat >= 0) return CMD.concat(new String[]{"-" + Format.getFile(iFormat) + "f"}, input);
        else return CMD.concat(new String[]{"-f"}, input);
    }

    public static String[] simple(String[] input) { return simple(-1, input); }

    public static String addToComplex(int identifier, int index, int iFormat, int stream, String filter) {
        return getStream(index, iFormat, stream) + filter + "[out" + identifier + "]";
    }

    public static String[] complex(Filter[] filters) {
        List<String> filterChain = new ArrayList<>();
        for (Filter filter : filters) {
            StringBuilder chain = new StringBuilder();
            for (int j = 0; j < filter.getAmount(); j++) {
                chain.append(addToComplex(
                        filter.getIdentifier(),
                        j,
                        filter.getiFormat(),
                        filter.getStream(),
                        filter.getFilter()
                ));
            }
            filterChain.add(chain.toString());
        }
        String finalFilter = String.join(";", filterChain);
        finalFilter = finalFilter.replaceAll(";{2,}", ";");
        return new String[]{"-filter_complex", finalFilter};
    }

    public static String formatFilter(String filter) {
        return filter.replaceAll(";+$", "");
    }

    public static String sVideo(int width, int height, int hQuality, int forceRatio, int interpolation) {
        String size = "scale=" + width + ":" + height;
        String ratio = "", interp = "", hqScaling = "";

        if(forceRatio >= 0) ratio = "force_original_aspect_ratio=" + (forceRatio == 0 ? "decrease" : "increase");
        if(interpolation == 1) interp = "flags=bicubic";
        else if(hQuality == 1) hqScaling = "flags=lanczos";
        return CMD.join(new String[]{size, ratio, interp, hqScaling}, ":");
    }

    public static String sVideo(int width, int height, int hQuality){
        return sVideo(width, height, hQuality, -1, -1);
    }

    public static String setPTS() {
        return "setpts=PTS-STARTPTS";
    }

    public static String fps(int fps) {
        return "fps=" + fps;
    }
}