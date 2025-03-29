package edu.up.isgc.vgc.graphic;

import edu.up.isgc.vgc.Component;
import edu.up.isgc.vgc.tools.CMD;
import java.io.File;

public class AIImage extends Image {
    private static final String AI_PATH = new File("Outputs/genimages").getAbsolutePath();

    public AIImage(int width, int height, String creationDate, Double duration, String type, String filePath) {
        super(width, height, creationDate, duration, type, CMD.normalizePath(filePath));
    }

    @Override
    public String returnIFormat() {
        return "AIImage";
    }

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