package edu.up.isgc.vgc.tools.ffmpeg;

import edu.up.isgc.vgc.tools.CMD;

public class Filter {
    public static String getStream(int index, int iFormat, int stream){
        String sIndex = Integer.toString(index);
        String sStream = Integer.toString(stream);
        String[] preResult = new String[]{sIndex, Format.getFile(iFormat), sStream};
        return "[" + CMD.join(preResult, ":") + "]";
    }

    public static String[] simple(int iFormat, String[] input) {
        return CMD.concat(new String[]{"-"+ Format.getFile(iFormat) +"f"}, input);
    }

    public static String[] complex(int amount, int iFormat, int stream, String filter){
        String[] resultFilter = new String[amount];
        for(int i = 0; i < amount; i++){
            resultFilter[i] = Filter.addToComplex(i, iFormat, stream, filter);
        }

        return new String[]{"-filter_complex", CMD.join(resultFilter, ";")};
    }

    public static String addToComplex(int index, int iFormat, int stream, String filter){
        return getStream(index, iFormat, stream) + filter + "[" + iFormat + index + "]";
    }

    public static String sVideo(String newSize, int forceRatio, int interpolation){
        String size = "scale=" + newSize;
        String ratio = "force_original_aspect_ratio=" + (forceRatio == 0 ? "decrease" : "increase");
        String interp = (interpolation == 1 ? "flags=bicubic" : null);
        String[] parameters = new String[]{size, ratio, interp};
        return CMD.join(parameters, ":");
    }

    public static String setPTS(){
        return "setpts=PTS-STARTPTS";
    }

    public static String fps(int fps){
        return "fps=" + fps;
    }
}
