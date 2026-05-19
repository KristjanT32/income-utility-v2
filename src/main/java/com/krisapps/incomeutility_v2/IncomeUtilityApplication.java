package com.krisapps.incomeutility_v2;

import com.krisapps.incomeutility_v2.dialogs.LoadingDialog;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityType;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.PopupManager;
import com.krisapps.incomeutility_v2.util.UtilityManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IncomeUtilityApplication extends Application {

    static Stage window;

    /**
     * Updates the window title.
     *
     * @param title        The text to set the window title to.
     * @param removePrefix If <code>true</code>, 'KrisApps Income Utility: ' will not be appended to the beginning of the title.
     */
    public static void updateTitle(String title, boolean removePrefix) {
        if (window == null) return;
        window.setTitle(removePrefix ? title : "KrisApps Income Utility: " + title);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(IncomeUtilityApplication.class.getResource("main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 980, 500);
        stage.setMinWidth(980);
        stage.setMinHeight(500);
        stage.setTitle("KrisApps Income Utility v2.0");
        stage.setScene(scene);
        stage.getIcons().add(new Image(IncomeUtilityApplication.class.getResource("icons/income_utility.png").toExternalForm()));

        window = stage;
        window.setOnCloseRequest((r) -> {
            if (UtilityManager.getInstance().hasOpenUtilities()) {
                PopupManager.showConfirmation(
                        "Shutdown application",
                        "Are you sure you wish to close the application?\nAll open sub-utilities will also be closed.",
                        new ButtonType("Yes, close everything", ButtonBar.ButtonData.APPLY),
                        new ButtonType("No, cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                ).ifPresent(response -> {
                    if (response.getButtonData() == ButtonBar.ButtonData.APPLY) {
                        shutdown();
                    }
                });
            } else {
                Platform.exit();
                System.exit(0);
            }
        });
        stage.show();
    }

    public static void restart() {
        try {
            String javaBin = System.getProperty("java.home")
                    + File.separator + "bin"
                    + File.separator + "java";

            File currentJar = new File(
                    IncomeUtilityApplication.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            );

            if (!currentJar.getName().endsWith(".jar")) {
                DataManager.log("Restarting is not possible, as the application is not run from a .jar file.");
                return;
            }

            List<String> command = new ArrayList<>();
            command.add(javaBin);
            command.add("-jar");
            command.add(currentJar.getPath());

            new ProcessBuilder(command).start();

            shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void shutdown() {
        LoadingDialog dialog = new LoadingDialog(LoadingDialog.LoadingOperationType.INDETERMINATE_PROGRESSBAR);
        dialog.setPrimaryLabel("Cleaning up");
        dialog.show("Shutting down...", new Runnable() {
            @Override
            public void run() {
                DataManager.getInstance().saveCurrentConfigurationData();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                UtilityManager.getInstance().stopAll(SubUtilityType.ALL);
                while (DataManager.getInstance().isSaving()) {
                    dialog.setPrimaryLabel("Saving data");
                    dialog.setSecondaryLabel("Waiting for I/O operations to finish...");
                }

                dialog.setPrimaryLabel("Resolving tension");
                dialog.setSecondaryLabel("Closing, bye!");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                Platform.exit();
                System.exit(0);
            }
        });
    }
}
