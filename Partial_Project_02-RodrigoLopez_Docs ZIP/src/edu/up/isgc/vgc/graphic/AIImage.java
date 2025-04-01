package edu.up.isgc.vgc.graphic;

import edu.up.isgc.vgc.Component;
import edu.up.isgc.vgc.tools.CMD;
import java.io.File;

/**
 * AIImage extends Image and represents an AI-generated image.
 * It includes a predefined output directory and provides methods for creation.
 */
public class AIImage extends Image {
    // Path where AI-generated images are stored
    private static final String AI_PATH = new File("Outputs/genimages").getAbsolutePath();

    /**
     * Constructor for AIImage.
     * @param width Image width
     * @param height Image height
     * @param creationDate Date of creation
     * @param duration Duration (not relevant for static images)
     * @param type Image type (e.g., PNG, JPG)
     * @param filePath Path to the image file
     */
    public AIImage(int width, int height, String creationDate, Double duration, String type, String filePath) {
        super(width, height, creationDate, duration, type, CMD.normalizePath(filePath));
    }

    /**
     * Returns the format type as "AIImage".
     * @return String representing the format
     */
    @Override
    public String returnIFormat() {
        return "AIImage";
    }

    /**
     * Creates an AIImage instance from a given file path.
     * @param imagePath Path to the image
     * @return AIImage instance
     */
    public static AIImage createFromPath(String imagePath) {
        File imageFile = new File(imagePath);
        return new AIImage(
                1024,
                1024,
                Component.generateNow(),
                0.0,
                "png",
                imageFile.getAbsolutePath()
        );
    }
}
