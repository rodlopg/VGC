package edu.up.isgc.vgc.gui;
import edu.up.isgc.vgc.Component;
import edu.up.isgc.vgc.graphic.Image;
import edu.up.isgc.vgc.graphic.Video;
import edu.up.isgc.vgc.tools.CMD;
import edu.up.isgc.vgc.tools.EXIF;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.List;
import java.util.function.Function;

import edu.up.isgc.vgc.tools.*;

public class GClient extends Application{
    @Override
    public void start(Stage primaryStage) {
        // Create a button with label "Input Videos"
        Button inputButton = new Button("Input Files");

        // Set button action
        inputButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Files");

            // Add file filters for common video formats
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Accepted Files", "*.mp4", "*.avi", "*.mov", "*.mkv", "*.flv", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
            );


            // Allow multiple file selection
            List<File> selectedFiles = fileChooser.showOpenMultipleDialog(primaryStage);

            if (selectedFiles != null) {
                for (File file : selectedFiles) {
                    if(file.exists() && file.length() != 0){
                        String filePath = file.getAbsolutePath();

                        int width = Integer.parseInt(EXIF.getWidth(filePath));
                        int height = Integer.parseInt(EXIF.getHeight(filePath));
                        String date = EXIF.getDate(filePath);
                        String type = EXIF.getType(filePath);

                        //Here the output is received
                        //String type = getFileType(file);
                        if (type.equals("Video")) {
                            Double duration = Double.parseDouble(EXIF.getDuration(filePath));
                            String codec = EXIF.getCodec(filePath);
                            Component newVid = new Video(width, height, date, duration, type, filePath, codec);
                        } else {
                            Component newImg = new Image(width, height, date, 0.0, type, filePath);
                        }
                        System.out.println("Selected: " + file.getAbsolutePath());
                    }else{
                        System.out.println("Null File, sorry Bruv");
                    }
                }
                Component.sortComponents();
                System.out.println("Sorted Components: " + Component.getComponents());
                for (Component component : Component.getComponents()) {
                    System.out.println("Date: " + component.getDate());
                }
                String test = "TEST";
                int testInt = 0;
                List<Function<Object, Object>> functions = List.of(
                        (Object input) -> FFMPEG.sVideo(test),
                        (Object input) -> FFMPEG.cCodec(testInt, test+"02"),
                        (Object input) -> FFMPEG.sVideo(test+"03")
                );

                System.out.println("Command: " + String.join(" ", Pipeline.biLambda(functions, CMD::concat)));
            }
        });

        // Layout
        VBox root = new VBox(10, inputButton);
        root.setStyle("-fx-padding: 20; -fx-alignment: center;");

        // Scene
        Scene scene = new Scene(root, 300, 200);
        primaryStage.setTitle("VGC");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "avi", "mov", "mkv", "flv");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "bmp");

    public static String getFileType(File file) {
        String name = file.getName().toLowerCase();
        int dotIndex = name.lastIndexOf(".");
        if (dotIndex == -1) return "Unknown"; // No extension

        String extension = name.substring(dotIndex + 1);

        if (VIDEO_EXTENSIONS.contains(extension)) return "Video";
        if (IMAGE_EXTENSIONS.contains(extension)) return "Image";

        return "Unknown";
    }

}
