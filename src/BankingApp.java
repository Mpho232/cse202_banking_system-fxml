import dao.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class BankingApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        DatabaseManager.initializeDatabase();

        URL fxmlLocation = getClass().getResource("/view/banking_view.fxml");
        if (fxmlLocation == null) {
            throw new IllegalStateException("Could not find /view/banking_view.fxml on classpath.");
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();

        Scene scene = new Scene(root, 1120, 760);
        primaryStage.setTitle("CSE202 Banking System — Admin & Customer Portal");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}