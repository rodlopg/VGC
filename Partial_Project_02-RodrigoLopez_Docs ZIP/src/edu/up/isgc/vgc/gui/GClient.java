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
import java.util.List;
import java.util.Set;

public class GClient extends Application {
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "avi", "mov", "mkv", "flv");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "bmp");
    private TextField moodInput;
    private ProgressIndicator progressIndicator;
    private static final String DARK_PRIMARY = "#2D2D2D";
    private static final String DARK_SECONDARY = "#3A3A3A";
    private static final String ACCENT_COLOR = "#00E676";
    private static final String TEXT_COLOR = "#FFFFFF";

    @Override
    public void start(Stage primaryStage) {
        // Main container
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(25));
        mainContainer.setStyle("-fx-background-color: " + DARK_PRIMARY + ";");

        // Header
        Label headerLabel = new Label("VGC - Visual Generation Console");
        headerLabel.setStyle("-fx-text-fill: " + ACCENT_COLOR + "; -fx-font-size: 20px; -fx-font-weight: bold;");

        // Input Section
        VBox inputSection = createSection("Media Input");
        Button inputButton = createStyledButton("Select Media Files");
        inputSection.getChildren().add(inputButton);

        // AI Generation Section
        VBox aiSection = createSection("AI Generation");
        moodInput = createStyledTextField("Enter mood (e.g., serene, adventurous)");
        Button generateAIButton = createStyledButton("Generate AI Postcard");
        progressIndicator = new ProgressIndicator();
        progressIndicator.setStyle("-fx-progress-color: " + ACCENT_COLOR + ";");
        progressIndicator.setVisible(false);
        aiSection.getChildren().addAll(moodInput, generateAIButton, progressIndicator);

        // Export Settings
        VBox exportSection = createSection("Export Settings");
        HBox formatControls = new HBox(15);
        ComboBox<String> videoFormatComboBox = createStyledComboBox("mp4", "avi", "mov", "mkv");
        ComboBox<String> audioFormatComboBox = createStyledComboBox("aac", "mp3", "opus");
        CheckBox subtitlesCheckBox = createStyledCheckBox("Add Subtitles");
        formatControls.getChildren().addAll(
                createLabeledControl("Video Format:", videoFormatComboBox),
                createLabeledControl("Audio Format:", audioFormatComboBox),
                subtitlesCheckBox
        );
        exportSection.getChildren().add(formatControls);

        // Share Controls
        HBox shareControls = new HBox(10);
        shareControls.setAlignment(Pos.CENTER);
        Button githubButton = createStyledButton("GitHub Export");
        Button emailButton = createStyledButton("Email Export");
        shareControls.getChildren().addAll(githubButton, emailButton);

        // Assemble main container
        mainContainer.getChildren().addAll(
                headerLabel,
                inputSection,
                new Separator(),
                aiSection,
                new Separator(),
                exportSection,
                new Separator(),
                shareControls
        );

        // Event Handlers
        inputButton.setOnAction(e -> handleFileSelection(primaryStage, videoFormatComboBox, subtitlesCheckBox));
        generateAIButton.setOnAction(e -> handleAIGeneration());
        githubButton.setOnAction(e -> uploadToGitHub(""));
        emailButton.setOnAction(e -> shareViaEmail(""));

        // Scene setup
        Scene scene = new Scene(mainContainer, 600, 700);
        scene.setFill(Color.web(DARK_PRIMARY));

        // Remove the applyGlobalStyles call
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
        btn.setStyle("-fx-background-color: linear-gradient(to bottom, " + ACCENT_COLOR + ", #00C853); " +
                "-fx-text-fill: " + TEXT_COLOR + "; -fx-font-weight: bold; " +
                "-fx-background-radius: 5; -fx-padding: 10 20;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: linear-gradient(to bottom, #00C853, " + ACCENT_COLOR + ");"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: linear-gradient(to bottom, " + ACCENT_COLOR + ", #00C853);"));
        return btn;
    }

    private TextField createStyledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #454545; -fx-text-fill: " + TEXT_COLOR + "; " +
                "-fx-border-color: #606060; -fx-border-radius: 4; -fx-padding: 8;");
        return tf;
    }

    private ComboBox<String> createStyledComboBox(String... items) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll(items);
        cb.setValue(items[0]);
        cb.setStyle("-fx-background-color: #454545; -fx-text-fill: " + TEXT_COLOR + ";");
        return cb;
    }

    private CheckBox createStyledCheckBox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-text-fill: " + TEXT_COLOR + ";");
        return cb;
    }

    private VBox createLabeledControl(String labelText, Control control) {
        VBox container = new VBox(5);
        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: " + TEXT_COLOR + ";");
        container.getChildren().addAll(label, control);
        return container;
    }


    // Original functional methods remain unchanged below
    private void handleFileSelection(Stage stage, ComboBox<String> formatComboBox, CheckBox subtitlesCheckBox) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Files");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Media Files",
                        "*.mp4", "*.avi", "*.mov", "*.mkv", "*.flv",
                        "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);
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
            String outputVideoPath = "output." + formatComboBox.getValue();
            FFMPEG.generateVideo(outputVideoPath, Component.getComponents(), subtitlesCheckBox.isSelected());
        }
    }

    private void handleAIGeneration() {
        String mood = moodInput.getText().trim();
        if (mood.isEmpty()) {
            showAlert("Input Error", "Please describe the mood for generation");
            return;
        }

        progressIndicator.setVisible(true);
        new Thread(() -> {
            try {
                String[] generatedImages = OpenAI.generatePostcards(mood, 2);
                if (generatedImages.length < 2) {
                    throw new Exception("Failed to generate two postcards");
                }

                Platform.runLater(() -> {
                    AIImage.createFromPath(generatedImages[0]);
                    AIImage.createFromPath(generatedImages[1]);
                    progressIndicator.setVisible(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    showAlert("Generation Error", "AI content creation failed: " + e.getMessage());
                });
            }
        }).start();
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            // Style the alert dialog
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-background-color: " + DARK_SECONDARY + ";");

            // Style the content text
            Label contentLabel = (Label) dialogPane.lookup(".content.label");
            if (contentLabel != null) {
                contentLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + ";");
            }

            alert.showAndWait();
        });
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