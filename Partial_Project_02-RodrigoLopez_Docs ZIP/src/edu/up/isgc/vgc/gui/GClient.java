package edu.up.isgc.vgc.gui;

import edu.up.isgc.vgc.Component;
import edu.up.isgc.vgc.graphic.Image;
import edu.up.isgc.vgc.graphic.Video;
import edu.up.isgc.vgc.tools.CMD;
import edu.up.isgc.vgc.tools.EXIF;
import edu.up.isgc.vgc.tools.ffmpeg.FFMPEG;
import edu.up.isgc.vgc.ai.OpenAIIntegration;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Main JavaFX application for the project.
 */
public class GClient extends Application {
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "avi", "mov", "mkv", "flv");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "bmp");

    @Override
    public void start(Stage primaryStage) {
        // Create UI components
        Button inputButton = new Button("Input Files");
        ComboBox<String> videoFormatComboBox = new ComboBox<>();
        videoFormatComboBox.getItems().addAll("mp4", "avi", "mov", "mkv");
        videoFormatComboBox.setValue("mp4");

        ComboBox<String> audioFormatComboBox = new ComboBox<>();
        audioFormatComboBox.getItems().addAll("aac", "mp3", "opus");
        audioFormatComboBox.setValue("aac");

        CheckBox subtitlesCheckBox = new CheckBox("Add Subtitles");
        Button githubButton = new Button("Connect to GitHub");
        Button emailButton = new Button("Share via Email");

        // Set button actions
        inputButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Files");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Accepted Files", "*.mp4", "*.avi", "*.mov", "*.mkv", "*.flv", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
            );

            List<File> selectedFiles = fileChooser.showOpenMultipleDialog(primaryStage);
            if (selectedFiles != null) {
                for (File file : selectedFiles) {
                    if (file.exists() && file.length() != 0) {
                        String filePath = file.getAbsolutePath();
                        int width = Integer.parseInt(EXIF.getWidth(filePath));
                        int height = Integer.parseInt(EXIF.getHeight(filePath));
                        String date = EXIF.getDate(filePath);
                        String type = EXIF.getType(filePath);

                        if (type.equals("Video")) {
                            Double duration = Double.parseDouble(EXIF.getDuration(filePath));
                            String codec = EXIF.getCodec(filePath);
                            Component newVid = new Video(width, height, date, duration, type, filePath, codec);
                        } else {
                            Component newImg = new Image(width, height, date, 0.0, type, filePath);
                        }

                        System.out.println("Selected: " + file.getAbsolutePath());
                    } else {
                        System.out.println("Null File, sorry Bruv");
                    }
                }

                Component.sortComponents();
                System.out.println("Sorted Components: " + Component.getComponents());

                for (Component component : Component.getComponents()) {
                    System.out.println("Date: " + component.getDate());
                }

                // Generate final video
                String outputVideoPath = "output." + videoFormatComboBox.getValue();
                FFMPEG.generateVideo(Component.getComponents(), outputVideoPath, videoFormatComboBox.getValue(), audioFormatComboBox.getValue(), subtitlesCheckBox.isSelected());
            }
        });

        githubButton.setOnAction(e -> {
            // Implement GitHub upload
            String outputVideoPath = "output." + videoFormatComboBox.getValue();
            uploadToGitHub(outputVideoPath);
        });

        emailButton.setOnAction(e -> {
            // Implement email sharing
            String outputVideoPath = "output." + videoFormatComboBox.getValue();
            shareViaEmail(outputVideoPath);
        });

        // Layout
        VBox root = new VBox(10, inputButton, new Label("Video Format:"), videoFormatComboBox,
                new Label("Audio Format:"), audioFormatComboBox, subtitlesCheckBox,
                githubButton, emailButton);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-alignment: center;");

        // Scene
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("VGC");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void uploadToGitHub(String filePath) {
        // Implement GitHub API interaction
        System.out.println("Uploading " + filePath + " to GitHub...");
    }

    private void shareViaEmail(String filePath) {
        // Implement email sharing logic
        System.out.println("Sharing " + filePath + " via email...");
    }

    public static void main(String[] args) {
        launch(args);
    }
}