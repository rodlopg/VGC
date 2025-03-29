package edu.up.isgc.vgc.gui;

import edu.up.isgc.vgc.Component;
import edu.up.isgc.vgc.graphic.AIImage;
import edu.up.isgc.vgc.graphic.Image;
import edu.up.isgc.vgc.graphic.Video;
import edu.up.isgc.vgc.tools.CMD;
import edu.up.isgc.vgc.tools.EXIF;
import edu.up.isgc.vgc.tools.OpenAI;
import edu.up.isgc.vgc.tools.ffmpeg.FFMPEG;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GClient extends Application {
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "avi", "mov", "mkv", "flv");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "bmp");
    private static final String DARK_PRIMARY = "#2D2D2D";
    private static final String DARK_SECONDARY = "#3A3A3A";
    private static final String ACCENT_COLOR = "#00E676";
    private static final String TEXT_COLOR = "#FFFFFF";

    private TextField moodInput;
    private ProgressIndicator progressIndicator;

    @Override
    public void start(Stage primaryStage) {
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(25));
        mainContainer.setStyle("-fx-background-color: " + DARK_PRIMARY + ";");

        // Header
        Label headerLabel = new Label("VGC - Visual Generation Console");
        headerLabel.setStyle("-fx-text-fill: " + ACCENT_COLOR + "; -fx-font-size: 20px; -fx-font-weight: bold;");

        // Unified input section
        VBox inputSection = createSection("Video Generation");
        moodInput = createStyledTextField("Enter mood (e.g., serene, adventurous)");

        Button generateButton = createStyledButton("Generate Video");
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);

        inputSection.getChildren().addAll(moodInput, generateButton, progressIndicator);

        // Assemble main container
        mainContainer.getChildren().addAll(
                headerLabel,
                inputSection
        );

        // Event handler for unified generation
        generateButton.setOnAction(e -> handleGeneration(primaryStage));

        Scene scene = new Scene(mainContainer, 600, 400);
        scene.setFill(Color.web(DARK_PRIMARY));
        primaryStage.setTitle("VGC - Visual Generation Console");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createSection(String title) {
        VBox section = new VBox(10);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: " + DARK_SECONDARY + "; -fx-background-radius: 8;");

        Label sectionLabel = new Label(title);
        sectionLabel.setStyle("-fx-text-fill: " + ACCENT_COLOR + "; -fx-font-size: 16px;");

        section.getChildren().add(sectionLabel);
        return section;
    }

    private Button createStyledButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + ACCENT_COLOR + "; " +
                "-fx-text-fill: " + TEXT_COLOR + "; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 5; " +
                "-fx-padding: 10 20;");
        return btn;
    }

    private TextField createStyledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #454545; " +
                "-fx-text-fill: " + TEXT_COLOR + "; " +
                "-fx-border-color: #606060; " +
                "-fx-border-radius: 4; " +
                "-fx-padding: 8;");
        return tf;
    }

    private void handleGeneration(Stage stage) {
        String mood = moodInput.getText().trim();
        if (mood.isEmpty()) {
            showAlert("Input Error", "Please enter a mood description");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Media Files");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Media Files",
                        "*.mp4", "*.avi", "*.mov", "*.mkv", "*.flv",
                        "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            showAlert("Input Error", "Please select at least one media file");
            return;
        }

        progressIndicator.setVisible(true);
        new Thread(() -> {
            try {
                // Generate AI content first
                String[] generatedImages = OpenAI.generatePostcards(mood, 2);
                if (generatedImages.length < 2) {
                    throw new Exception("Failed to generate two postcards");
                }

                // Process selected files
                List<Component> components = processFiles(selectedFiles);

                Platform.runLater(() -> {
                    try {
                        // Create final video with all components
                        FFMPEG.generateVideo("final_output.mp4", components, false);
                        progressIndicator.setVisible(false);
                        showAlert("Success", "Video generated successfully!");
                    } catch (Exception e) {
                        progressIndicator.setVisible(false);
                        showAlert("Generation Error", e.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    showAlert("Generation Error", e.getMessage());
                });
            }
        }).start();
    }

    private List<Component> processFiles(List<File> files) {
        List<Component> components = new ArrayList<>();
        for (File file : files) {
            if (file.exists() && file.length() != 0) {
                try {
                    String filePath = file.getAbsolutePath().replace("\\", "/");
                    int width = Integer.parseInt(EXIF.getWidth(filePath));
                    int height = Integer.parseInt(EXIF.getHeight(filePath));
                    String date = EXIF.getDate(filePath);
                    String type = EXIF.getType(filePath);

                    if (VIDEO_EXTENSIONS.contains(type)) {
                        Double duration = Double.parseDouble(EXIF.getDuration(filePath));
                        String codec = EXIF.getCodec(filePath);
                        components.add(new Video(width, height, date, duration, type, filePath, codec));
                    } else if (IMAGE_EXTENSIONS.contains(type)) {
                        components.add(new Image(width, height, date, 0.0, type, filePath));
                    }
                } catch (Exception e) {
                    System.err.println("Error processing file: " + file.getName());
                }
            }
        }
        return components;
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-background-color: " + DARK_SECONDARY + ";");

            Label contentLabel = (Label) dialogPane.lookup(".content.label");
            if (contentLabel != null) {
                contentLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + ";");
            }

            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}