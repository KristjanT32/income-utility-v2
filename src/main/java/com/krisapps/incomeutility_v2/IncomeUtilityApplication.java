package com.krisapps.incomeutility_v2;

import com.krisapps.incomeutility_v2.dialogs.LoadingDialog;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityType;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.PopupManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;

public class IncomeUtilityApplication extends Application {

    static Stage window;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(IncomeUtilityApplication.class.getResource("main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 980, 500);
        stage.setMinWidth(980);
        stage.setMinHeight(500);
        stage.setTitle("KrisApps Income Utility v2.0");
        stage.setScene(scene);

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
                        LoadingDialog dialog = new LoadingDialog(LoadingDialog.LoadingOperationType.INDETERMINATE_PROGRESSBAR);
                        dialog.setPrimaryLabel("Cleaning up");
                        dialog.setSecondaryLabel("Saving data...");
                        dialog.show("Shutting down...", new Runnable() {
                            @Override
                            public void run() {
                                DataManager.getInstance().saveCurrentData();
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                                dialog.setSecondaryLabel("Closing, bye!");
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                                UtilityManager.getInstance().stopAll(SubUtilityType.ALL);
                                Platform.exit();
                                System.exit(0);
                            }
                        });
                    }
                });
            }
        });
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
