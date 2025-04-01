package edu.up.isgc.vgc.gui;

import edu.up.isgc.vgc.Component;
import edu.up.isgc.vgc.graphic.AIImage;
import edu.up.isgc.vgc.graphic.Image;
import edu.up.isgc.vgc.graphic.Video;
import edu.up.isgc.vgc.tools.CMD;
import edu.up.isgc.vgc.tools.EXIF;
import edu.up.isgc.vgc.tools.OpenAI;
import edu.up.isgc.vgc.tools.ffmpeg.FFMPEG;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
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
    private StackPane progressIndicator;
    private VBox selectedFilesContainer;

    @Override
    public void start(Stage primaryStage) {
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(25));
        mainContainer.setStyle("-fx-background-color: " + DARK_PRIMARY + ";");
        mainContainer.setOpacity(0);

        Label headerLabel = new Label("VGC - Visual Generation Console");
        headerLabel.setStyle("-fx-text-fill: " + ACCENT_COLOR + "; -fx-font-size: 20px; -fx-font-weight: bold;");

        VBox inputSection = createSection("Video Generation");
        moodInput = createStyledTextField("Enter mood (e.g., serene, adventurous)");
        Button generateButton = createStyledButton("Generate Video");
        progressIndicator = createProgressSpinner();
        progressIndicator.setVisible(false);

        selectedFilesContainer = new VBox(5);
        selectedFilesContainer.setPadding(new Insets(10, 0, 0, 0));
        selectedFilesContainer.setStyle("-fx-background-color: #454545; -fx-background-radius: 5;");

        inputSection.getChildren().addAll(moodInput, generateButton, progressIndicator, selectedFilesContainer);
        mainContainer.getChildren().addAll(headerLabel, inputSection);

        Scene scene = new Scene(mainContainer, 600, 400);
        scene.setFill(Color.web(DARK_PRIMARY));
        primaryStage.setTitle("VGC - Visual Generation Console");
        primaryStage.setScene(scene);
        primaryStage.show();

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), mainContainer);
        fadeIn.setToValue(1);
        fadeIn.play();

        generateButton.setOnAction(e -> handleGeneration(primaryStage));
    }

    private VBox createSection(String title) {
        VBox section = new VBox(10);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: " + DARK_SECONDARY + "; -fx-background-radius: 8;");
        section.setEffect(new DropShadow(10, Color.BLACK));

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

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), btn);
        scaleIn.setToX(1.05);
        scaleIn.setToY(1.05);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(200), btn);
        scaleOut.setToX(1.0);
        scaleOut.setToY(1.0);

        TranslateTransition pressIn = new TranslateTransition(Duration.millis(50), btn);
        pressIn.setToY(2);

        TranslateTransition pressOut = new TranslateTransition(Duration.millis(50), btn);
        pressOut.setToY(0);

        btn.setOnMouseEntered(e -> {
            scaleIn.playFromStart();
            btn.setEffect(new DropShadow(10, Color.web(ACCENT_COLOR + "80")));
        });
        btn.setOnMouseExited(e -> {
            scaleOut.playFromStart();
            btn.setEffect(null);
        });
        btn.setOnMousePressed(e -> pressIn.playFromStart());
        btn.setOnMouseReleased(e -> pressOut.playFromStart());

        return btn;
    }

    private StackPane createProgressSpinner() {
        StackPane spinner = new StackPane();
        spinner.setPrefSize(40, 40);

        Circle outerCircle = new Circle(15, Color.TRANSPARENT);
        outerCircle.setStroke(Color.web(ACCENT_COLOR));
        outerCircle.setStrokeWidth(3);
        outerCircle.getStrokeDashArray().addAll(5d, 5d);
        outerCircle.setStrokeLineCap(StrokeLineCap.ROUND);

        Circle innerCircle = new Circle(10, Color.TRANSPARENT);
        innerCircle.setStroke(Color.web(ACCENT_COLOR));
        innerCircle.setStrokeWidth(3);
        innerCircle.getStrokeDashArray().addAll(3d, 6d);
        innerCircle.setStrokeLineCap(StrokeLineCap.ROUND);

        spinner.getChildren().addAll(outerCircle, innerCircle);

        RotateTransition rt = new RotateTransition(Duration.seconds(2), spinner);
        rt.setByAngle(360);
        rt.setCycleCount(Animation.INDEFINITE);
        rt.play();

        return spinner;
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
                        "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            showAlert("Input Error", "Please select at least one media file");
            return;
        }

        Platform.runLater(() -> {
            selectedFilesContainer.getChildren().clear();
            for (File file : selectedFiles) {
                Label fileLabel = new Label(file.getName());
                fileLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-size: 12px;");
                fileLabel.setOpacity(0);
                selectedFilesContainer.getChildren().add(fileLabel);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), fileLabel);
                fadeIn.setToValue(1);
                fadeIn.play();
            }
        });

        progressIndicator.setVisible(true);

        new Thread(() -> {
            try {
                String[] generatedImages = OpenAI.generatePostcards(mood, 2);
                if (generatedImages.length < 2) {
                    throw new Exception("Failed to generate two postcards");
                }

                Component postCard01 = AIImage.createFromPath(generatedImages[0]);
                Component postCard02 = AIImage.createFromPath(generatedImages[1]);

                List<Component> components = processFiles(selectedFiles);

                Platform.runLater(() -> {
                    try {
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
            dialogPane.setOpacity(0);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), dialogPane);
            fadeIn.setToValue(1);
            fadeIn.play();

            Label contentLabel = (Label) dialogPane.lookup(".content.label");
            if (contentLabel != null) {
                contentLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + ";");
            }

            alert.showAndWait();
        });
    }
}