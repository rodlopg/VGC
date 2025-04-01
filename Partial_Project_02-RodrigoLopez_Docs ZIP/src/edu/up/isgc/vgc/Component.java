package edu.up.isgc.vgc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public abstract class Component implements Comparable<Component> {

    // Static fields for max resolution, component count, and collection of components
    private static int[] maxResolution = new int[]{1920, 1080};  // [width, height] of the max resolution encountered
    private static int compAmount = 0;  // Total number of components
    private static ArrayList<Component> components = new ArrayList<Component>();  // List of components
    private static Component[] postCards = new Component[2];  // Array to hold post cards (maximum of 2)
    private static int postCardsAmount = 0;  // Amount of post cards added

    // Instance variables representing the attributes of a component
    private int width;  // Width of the component
    private int height;  // Height of the component
    private Double duration;  // Duration associated with the component
    private String type;  // Type of the component (e.g., image, video, etc.)
    private String path;  // Path where the component is stored
    private String date;  // Date associated with the component (e.g., creation date)

    // Constructor to initialize a Component with its attributes
    public Component(int width, int height, String date, Double duration, String type, String path) {
        this.setWidth(width);
        this.setHeight(height);
        this.setDate(date);
        this.setDuration(duration);
        this.setType(type);
        this.setPath(path);

        // If the component is not of type "AIImage", add it to the components list
        if (!Objects.equals(this.returnIFormat(), "AIImage")) {
            Component.addComponent(this);
        } else {
            // If the component is "AIImage", store it as a post card and increment the post card counter
            Component.getPostCards()[postCardsAmount % 2] = this;
            Component.setPostCardAmount(postCardsAmount + 1);
        }
    }

    // Getters and setters for the attributes of the component

    public static int[] getMaxResolution() {
        return maxResolution;
    }

    public static void setMaxResolution(int[] maxResolution) {
        Component.maxResolution = maxResolution;
    }

    // Abstract method for printing component-specific attributes
    public abstract void printAttributes();

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public Double getDuration() { return duration; }
    public void setDuration(Double duration) { this.duration = duration; }

    public static int getCompAmount() { return compAmount; }
    public static void setCompAmount(int compAmount) { Component.compAmount = compAmount; }

    public static ArrayList<Component> getComponents() { return components; }
    public static void setComponents(ArrayList<Component> components) { Component.components = components; }

    public static Component[] getPostCards() { return postCards; }
    public static void setPostCardAmount(int amount) { Component.postCardsAmount = amount; }

    // Abstract method to return the format type of the component (e.g., "AIImage", etc.)
    public abstract String returnIFormat();

    // Private static method to add a component to the collection of components
    private static void addComponent(Component component) {
        components.add(component);  // Add the component to the list
        setCompAmount(components.size());  // Update the total component count
        Component.sortComponents();
    }

    // Implementing compareTo method for Comparable interface, comparing by date
    @Override
    public int compareTo(Component other) {
        return this.getDate().compareTo(other.getDate());
    }

    // Static method to sort the components list by date
    public static void sortComponents() {
        Collections.sort(components);  // Sort components using the compareTo method
    }

    // Static method to generate the current date and time in EXIF format
    public static String generateNow(){
        LocalDateTime now = LocalDateTime.now();

        // EXIF format pattern: "yyyy:MM:dd HH:mm:ss"
        DateTimeFormatter exifFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

        // Format the current date and time into the EXIF format and return it
        return now.format(exifFormatter);
    }

    public abstract Component copyTo(String newPath);
}
