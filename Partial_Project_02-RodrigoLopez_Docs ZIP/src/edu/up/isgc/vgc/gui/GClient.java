package edu.up.isgc.vgc.gui;

import edu.up.isgc.vgc.Component;
import edu.up.isgc.vgc.graphic.Image;
import edu.up.isgc.vgc.graphic.Video;
import edu.up.isgc.vgc.tools.CMD;
import edu.up.isgc.vgc.tools.EXIF;
import edu.up.isgc.vgc.tools.ffmpeg.FFMPEG;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import java.io.File;
import java.util.List;
import java.util.Set;

public class GClient extends Application {
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "avi", "mov", "mkv", "flv");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "bmp");

    @Override
    public void start(Stage primaryStage) {
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
                        String filePath = file.getAbsolutePath().replace("\\", "/");
                        int width = Integer.parseInt(EXIF.getWidth(filePath));
                        int height = Integer.parseInt(EXIF.getHeight(filePath));
                        String date = EXIF.getDate(filePath);
                        String type = EXIF.getType(filePath);

                        if (VIDEO_EXTENSIONS.contains(type)) {
                            Double duration = Double.parseDouble(EXIF.getDuration(filePath));
                            String codec = EXIF.getCodec(filePath);
                            Component newVid = new Video(width, height, date, duration, type, filePath, codec);
                        } else if (IMAGE_EXTENSIONS.contains(type)) {
                            Component newImg = new Image(width, height, date, 0.0, type, filePath);
                        } else {
                            System.out.println("ERROR: Unsupported type: " + type);
                        }
                    }
                }

                Component.sortComponents();
                String outputVideoPath = "output." + videoFormatComboBox.getValue();
                FFMPEG.generateVideo(outputVideoPath, Component.getComponents(), subtitlesCheckBox.isSelected());
            }
        });

        VBox root = new VBox(10, inputButton, new Label("Video Format:"), videoFormatComboBox,
                new Label("Audio Format:"), audioFormatComboBox, subtitlesCheckBox,
                githubButton, emailButton);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-alignment: center;");

        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("VGC");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void uploadToGitHub(String filePath) {
        System.out.println("Uploading " + filePath + " to GitHub...");
    }

    private void shareViaEmail(String filePath) {
        System.out.println("Sharing " + filePath + " via email...");
    }

    public static void main(String[] args) {
        launch(args);
    }
}