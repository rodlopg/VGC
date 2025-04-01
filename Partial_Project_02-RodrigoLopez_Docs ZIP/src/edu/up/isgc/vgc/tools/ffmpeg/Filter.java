package edu.up.isgc.vgc.tools.ffmpeg;

import edu.up.isgc.vgc.tools.CMD;
import java.util.ArrayList;
import java.util.List;

public class Filter {
    // Static variable to keep track of the total number of filters
    private static int fAmount = 0;

    // Instance variables representing various filter properties
    private int amount, iFormat, stream, identifier;
    private String filter;

    // Constructor to initialize the filter object with given properties
    public Filter(int amount, int iFormat, int stream, String filter) {
        this.setAmount(amount); // Set the amount of filters
        this.setiFormat(iFormat); // Set the input format
        this.setStream(stream); // Set the stream ID
        this.setIdentifier(fAmount); // Assign a unique identifier based on the static counter
        Filter.setfAmount(Filter.getfAmount() + 1); // Increment the static filter count
        this.setFilter(filter); // Set the filter string
    }

    // Getter and setter methods for each property

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

    // Method to construct the stream format for a given index, input format, and stream
    public static String getStream(int index, int iFormat, int stream) {
        String sIndex = Integer.toString(index); // Convert index to string
        String sStream = Integer.toString(stream); // Convert stream ID to string
        // Create the stream format in the form "[index:inputFormat:stream]"
        String[] preResult = new String[]{sIndex, Format.getFile(iFormat), sStream};
        return "[" + CMD.join(preResult, ":") + "]";
    }

    // Method to return a simple filter for a given input format and string array
    public static String[] simple(int iFormat, String[] input) {
        if(iFormat >= 0)
            return CMD.concat(new String[]{"-" + Format.getFile(iFormat) + "f"}, input);
        else
            return CMD.concat(new String[]{"-f"}, input);
    }

    // Overloaded method to return a simple filter with default input format (-1)
    public static String[] simple(String[] input) {
        return simple(-1, input);
    }

    // Method to generate the complex filter format for a specific filter object
    public static String addToComplex(int identifier, int index, int iFormat, int stream, String filter) {
        // Get the stream and filter, then construct the complex filter string
        return getStream(index, iFormat, stream) + filter + "[out" + identifier + "]";
    }

    // Method to generate a complex filter chain from an array of Filter objects
    public static String[] complex(Filter[] filters) {
        List<String> filterChain = new ArrayList<>();
        // Iterate over each filter and build its part of the chain
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
        // Join the filter parts with a semicolon and clean up extra semicolons
        String finalFilter = String.join(";", filterChain);
        finalFilter = finalFilter.replaceAll(";{2,}", ";");
        return new String[]{"-filter_complex", finalFilter}; // Return the complete filter complex string
    }

    // Method to format a filter string by removing trailing semicolons
    public static String formatFilter(String filter) {
        return filter.replaceAll(";+$", "");
    }

    // Method to generate a scale filter with width, height, and optional quality and interpolation settings
    public static String sVideo(int width, int height, int hQuality, int forceRatio, int interpolation) {
        String size = "scale=" + width + ":" + height; // Define the scale filter with width and height
        String ratio = "", interp = "", hqScaling = "";

        // If forced aspect ratio is specified, add it to the filter
        if(forceRatio >= 0)
            ratio = "force_original_aspect_ratio=" + (forceRatio == 0 ? "decrease" : "increase");

        // If bicubic interpolation is requested, set the interpolation filter
        if(interpolation == 1)
            interp = "flags=bicubic";

            // If high-quality scaling is requested, set the high-quality scaling flag
        else if(hQuality == 1)
            hqScaling = "flags=lanczos";

        // Join all parts of the video filter (size, ratio, interpolation, and scaling) and return it
        return CMD.join(new String[]{size, ratio, interp, hqScaling}, ":");
    }

    // Overloaded method for sVideo to set default values for forceRatio and interpolation
    public static String sVideo(int width, int height, int hQuality) {
        return sVideo(width, height, hQuality, -1, -1);
    }

    // Method to generate a setPTS filter for setting presentation timestamps
    public static String setPTS() {
        return "setpts=PTS-STARTPTS";
    }

    // Method to generate a filter for setting frames per second
    public static String fps(int fps) {
        return "fps=" + fps;
    }
}
