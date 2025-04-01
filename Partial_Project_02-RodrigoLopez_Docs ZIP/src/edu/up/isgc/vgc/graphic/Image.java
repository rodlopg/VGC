package edu.up.isgc.vgc.graphic;
import edu.up.isgc.vgc.Component;

/**
 * Represents an image component with editable properties.
 */
public class Image extends Component implements Edit {
    /**
     * Constructor for an Image object.
     * @param width Image width
     * @param height Image height
     * @param date Creation date
     * @param duration Duration (0 for images)
     * @param type File type (e.g., PNG, JPG)
     * @param path File path
     */
    public Image(int width, int height, String date, Double duration, String type, String path){
        super(width, height, date, duration, type, path);
    }

    /**
     * Prints the image attributes in a structured format.
     */
    @Override
    public void printAttributes(){
        System.out.println("|Path: " + getPath());
        System.out.println("||Resolution: " + getWidth() + ":" + getHeight());
        System.out.println("|||Date: " + getDate());
        System.out.println("||||Duration: " + getDuration());
        System.out.println("|||||Type: " + getType());
    }

    /**
     * Returns the format type as "Image".
     * @return String representing the format
     */
    @Override
    public String returnIFormat(){ return "Image"; }

    @Override
    public Component scale(){ return this; }

    @Override
    public Component cut(){ return this; }

    @Override
    public Component copy(){ return this; }
}
