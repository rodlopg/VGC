package edu.up.isgc.vgc;

import java.util.ArrayList;
import java.util.Collections;

public abstract class Component implements Comparable<Component>{
    private static int[] maxResolution = new int[]{0,0};
    private int width;
    private int height;
    private Double duration;
    private String type, path, date;
    private static int compAmount = 0;
    private static ArrayList<Component> components = new ArrayList<Component>();

    public Component(int width, int height, String date, Double duration, String type, String path) {
        this.setWidth(width);
        this.setHeight(height);
        this.setDate(date);
        this.setDuration(duration);
        this.setType(type);
        this.setPath(path);
        Component.addComponent(this);
    }

    public static int[] getMaxResolution() {
        return Component.maxResolution;
    }

    public static void setMaxResolution(int[] maxResolution) {
        Component.maxResolution = maxResolution;
    }

    public abstract void printAttributes();

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public static int getCompAmount() {
        return Component.compAmount;
    }

    public static void setCompAmount(int compAmount) {
        Component.compAmount = compAmount;
    }

    public static ArrayList<Component> getComponents() {
        return components;
    }

    public static void setComponents(ArrayList<Component> components) {
        Component.components = components;
    }

    private static void addComponent(Component component) {
        components.add(component);
        Component.setCompAmount(Component.getComponents().size());
        if(component.getWidth() > maxResolution[0] && component.getHeight() > maxResolution[1]) {
            Component.setMaxResolution(new int[]{component.getWidth(), component.getHeight()});
        }
    }

    @Override
    public int compareTo(Component other) {
        return CharSequence.compare(this.getDate(), other.getDate());
    }

    // This will automatically sort your components by sortKey
    public static void sortComponents() {
        Collections.sort(Component.getComponents());
    }
}
