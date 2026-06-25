package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class IncomeUtilityDialog<T> extends Dialog<T> {

    protected VBox rootPane;

    /**
     * Constructs a generic IncomeUtilityDialog.
     *
     * @param dialogFileName The name of the dialog file (with extension), relative to the <code>dialogs</code> folder in the resource directory.
     * @param title          The title of the dialog window.
     */
    public IncomeUtilityDialog(String dialogFileName, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/dialogs/%s".formatted(dialogFileName)));
            loader.setController(this);
            rootPane = loader.load();
            getDialogPane().getStylesheets().add(IncomeUtilityApplication.class.getResource("stylesheets/core-ui.css").toExternalForm());
            getDialogPane().getStylesheets().add(IncomeUtilityApplication.class.getResource("stylesheets/main.css").toExternalForm());

            getDialogPane().setContent(rootPane);
            ((Stage) getDialogPane().getScene().getWindow()).getIcons().add(new Image(IncomeUtilityApplication.class.getResource("icons/income_utility.png").toExternalForm()));

            getDialogPane().getButtonTypes().clear();
            ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
            getDialogPane().getButtonTypes().add(closeButton);

            Node b = getDialogPane().lookupButton(closeButton);
            b.setVisible(false);
            b.setManaged(false);

            initModality(Modality.APPLICATION_MODAL);
            setTitle(title);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constructs a generic IncomeUtilityDialog.
     * @param dialogFileName The name of the dialog file (with extension), relative to the <code>dialogs</code> folder in the resource directory.
     * @param title The title of the dialog window.
     * @param iconFileName The name of the icon file (with extension), relative to the <code>icons</code> folder in the resource directory.
     */
    public IncomeUtilityDialog(String dialogFileName, String title, String iconFileName) {
        try {
            FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/dialogs/%s".formatted(dialogFileName)));
            loader.setController(this);
            rootPane = loader.load();
            getDialogPane().getStylesheets().add(IncomeUtilityApplication.class.getResource("stylesheets/core-ui.css").toExternalForm());
            getDialogPane().getStylesheets().add(IncomeUtilityApplication.class.getResource("stylesheets/main.css").toExternalForm());

            getDialogPane().setContent(rootPane);
            ((Stage) getDialogPane().getScene().getWindow()).getIcons().add(new Image(IncomeUtilityApplication.class.getResource("icons/" + iconFileName).toExternalForm()));

            getDialogPane().getButtonTypes().clear();
            ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
            getDialogPane().getButtonTypes().add(closeButton);

            Node b = getDialogPane().lookupButton(closeButton);
            b.setVisible(false);
            b.setManaged(false);

            initModality(Modality.APPLICATION_MODAL);
            setTitle(title);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setAllowResize(boolean allowResize) {
        ((Stage) getDialogPane().getScene().getWindow()).setResizable(allowResize);
    }
}
