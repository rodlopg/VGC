package edu.up.isgc.vgc.tools.ffmpeg;

import edu.up.isgc.vgc.tools.CMD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Filter {
    private int amount, iFormat, stream;
    private String filter;

    public Filter(int amount, int iFormat, int stream, String filter) {
        this.setAmount(amount);
        this.setiFormat(iFormat);
        this.setStream(stream);
        this.setFilter(filter);
    }

    public static String getStream(int index, int iFormat, int stream){
        String sIndex = Integer.toString(index);
        String sStream = Integer.toString(stream);
        String[] preResult = new String[]{sIndex, Format.getFile(iFormat), sStream};
        return "[" + CMD.join(preResult, ":") + "]";
    }

    public static String[] simple(int iFormat, String[] input) {
        return CMD.concat(new String[]{"-"+ Format.getFile(iFormat) +"f"}, input);
    }

    public static String[] complex(Filter[] filters){
        List<String> list = new ArrayList<String>();

        for(Filter f : filters){
            for(int j = 0; j < f.getAmount(); j++){
                list.add(Filter.addToComplex(j, f.getiFormat(), f.getStream(), f.getFilter()));
            }
        }

        String[] resultFilter = list.toArray(new String[0]);

        String finalFilter = Filter.formatFilter(CMD.join(resultFilter, ";"));
        System.out.println(finalFilter);
        return new String[]{"-filter_complex", finalFilter};
    }

    public static String addToComplex(int index, int iFormat, int stream, String filter){
        return getStream(index, iFormat, stream) + filter + "[" + Format.getFile(iFormat) + index + "]";
    }

    public static String formatFilter(String filter){
        String[] fFilter = new String[]{"\"", filter, "\""};
        return CMD.join(fFilter, "");
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

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getiFormat() {
        return iFormat;
    }

    public void setiFormat(int iFormat) {
        this.iFormat = iFormat;
    }

    public int getStream() {
        return stream;
    }

    public void setStream(int stream) {
        this.stream = stream;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }
}
