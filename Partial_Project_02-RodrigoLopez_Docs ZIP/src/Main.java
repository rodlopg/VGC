import edu.up.isgc.vgc.gui.GClient;
import javafx.application.Application;
import javafx.stage.Stage;

import static javafx.application.Application.launch;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        GClient client = new GClient();
        client.start(primaryStage);  // Pass the stage to your client
    }

    public static void main(String[] args) {
        launch(args);  // Standard JavaFX entry point
    }
}