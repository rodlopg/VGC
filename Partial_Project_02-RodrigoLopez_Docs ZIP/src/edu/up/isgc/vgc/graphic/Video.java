package edu.up.isgc.vgc.graphic;
import edu.up.isgc.vgc.Component;

/**
 * Represents a Video component with editable properties.
 */
public class Video extends Component implements Edit {
    // Codec used for encoding the video
    private String codec;

    /**
     * Constructor for a Video object.
     * @param width Video width
     * @param height Video height
     * @param date Creation date
     * @param duration Video duration
     * @param type File type (e.g., MP4, AVI)
     * @param path File path
     * @param codec Video codec
     */
    public Video(int width, int height, String date, Double duration, String type, String path, String codec){
        super(width, height, date, duration, type, path);
        this.setCodec(codec);
    }

    /**
     * Prints the video attributes in a structured format.
     */
    @Override
    public void printAttributes(){
        System.out.println("|Path: " + getPath());
        System.out.println("||Resolution: " + getWidth() + ":" + getHeight());
        System.out.println("|||Date: " + getDate());
        System.out.println("||||Duration: " + getDuration());
        System.out.println("|||||Type: " + getType());
        System.out.println("||||||Codec: " + getCodec());
    }

    /**
     * Gets the codec of the video.
     * @return Video codec
     */
    public String getCodec() {
        return codec;
    }

    /**
     * Sets the codec of the video.
     * @param codec Video codec
     */
    public void setCodec(String codec) {
        this.codec = codec;
    }

    /**
     * Returns the format type as "Video".
     * @return String representing the format
     */
    @Override
    public String returnIFormat(){ return "Video"; }

    @Override
    public Component scale(){ return this; }

    @Override
    public Component cut(){ return this; }

    @Override
    public Component copy(){ return this; }
}
