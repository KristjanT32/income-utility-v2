package com.krisapps.incomeutility_v2;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class IncomeUtilityApplication extends Application {

    static Stage window;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(IncomeUtilityApplication.class.getResource("main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("KrisApps Income Utility v2.0");
        stage.setScene(scene);

        window = stage;
        stage.show();
    }

    /**
     * Updates the window title.
     * @param title The text to set the window title to.
     * @param removePrefix If <code>true</code>, 'KrisApps Income Utility: ' will not be appended to the beginning of the title.
     */
    public static void updateTitle(String title, boolean removePrefix){
        if (window == null) return;
        window.setTitle(removePrefix ? title : "KrisApps Income Utility: " + title);
    }
}
