package edu.up.isgc.vgc.tools.ffmpeg;

import edu.up.isgc.vgc.tools.CMD;

public class Filter {
    public static String join(String[] array, String delimiter){
        StringBuilder result = new StringBuilder();

        for(int i = 0; i < array.length; i++){
            result.append(array[i]);

            if(i < array.length - 1){ result.append(delimiter); }
        }
        return result.toString();
    }

    public static String[] getStream(int index, int iFormat, int stream){
        String sIndex = Integer.toString(index);
        String sStream = Integer.toString(stream);
        return new String[]{"[", sIndex, ":", Format.getFile(iFormat), ":", sStream, "]"};
    }

    public static String[] simple(int iFormat, String[] input) {
        return CMD.concat(new String[]{"-"+ Format.getFile(iFormat) +"f"}, input);
    }

    public static String[] complex(){
        return new String[]{"-filter_complex", };
    }

    public static String sVideo(String newSize, int forceRatio, int interpolation){
        String size = "scale=" + newSize;
        String ratio = ":force_original_aspect_ratio=" + (forceRatio == 0 ? "decrease" : "increase");
        String interp = (interpolation == 1 ? ":flags=bicubic" : null);
        String[] parameters = new String[]{size, ratio, interp};
        return Filter.join(parameters, ":");
    }

    public static String setPTS(){
        return "setpts=PTS-STARTPTS";
    }

    public static String fps(int fps){
        return "fps=" + fps;
    }
}
