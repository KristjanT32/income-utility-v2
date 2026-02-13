package com.krisapps.incomeutility_v2.util;

import com.krisapps.incomeutility_v2.IncomeUtilityController;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PopupManager {

    public enum PopupType {
        NOT_IMPLEMENTED,
        INSUFFICIENT_BALANCE,
    }

    @SuppressWarnings("ConstantConditions")
    public static Optional<ButtonType> showPredefinedPopup(PopupType type) {
        Alert alert = new Alert(null);
        alert.getDialogPane().getStylesheets().add(IncomeUtilityController.class.getResource("stylesheets/core-ui.css").toExternalForm());
        switch (type) {
            case NOT_IMPLEMENTED -> {
                alert.setTitle("Uh-oh!");
                alert.setContentText("This feature hasn't been implemented yet. Sorry!");
                alert.setAlertType(Alert.AlertType.WARNING);
            }
            case INSUFFICIENT_BALANCE -> {
                alert.setTitle("Insufficient balance");
                alert.setContentText("The transaction cannot be completed as the source account does not have enough funds available.");
                alert.setAlertType(Alert.AlertType.ERROR);
            }
        }
        alert.setHeaderText(null);
        return alert.showAndWait();
    }

    public static Optional<ButtonType> showConfirmation(String title, String message, ButtonType optionA, ButtonType optionB) {
        if (!optionA.getButtonData().isCancelButton() && !optionB.getButtonData().isCancelButton()) {
            throw new IllegalArgumentException("Cannot show a confirmation dialog without a cancel option - please ensure that either optionA or optionB is a cancellation ButtonType");
        }

        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.getDialogPane().getStylesheets().add(IncomeUtilityController.class.getResource("stylesheets/core-ui.css").toExternalForm());
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.getButtonTypes().clear();
        a.getButtonTypes().addAll(optionA, optionB);
        return a.showAndWait();
    }

    public static Optional<ButtonType> showPopup(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(null);
        alert.getDialogPane().getStylesheets().add(IncomeUtilityController.class.getResource("stylesheets/core-ui.css").toExternalForm());
        alert.setTitle(title);
        alert.setContentText(message);
        alert.setAlertType(type);
        alert.setHeaderText(null);
        return alert.showAndWait();
    }

    public static String showInputDialog(String title, String message, String inputLabel, @Nullable String inputValue) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);

        a.getDialogPane().getStylesheets().add(IncomeUtilityController.class.getResource("stylesheets/core-ui.css").toExternalForm());
        a.setTitle(title);
        a.setHeaderText(null);

        VBox root = new VBox();
        HBox.setHgrow(root, Priority.ALWAYS);

        Label msgLabel = new Label(message);
        Label inputBoxLabel = new Label(inputLabel);
        TextField inputField = new TextField();

        VBox inputContainer = new VBox();
        inputContainer.getChildren().add(inputBoxLabel);
        inputContainer.getChildren().add(inputField);
        inputContainer.setSpacing(5);

        inputBoxLabel.setStyle("-fx-font-weight: bold");

        HBox.setHgrow(inputContainer, Priority.ALWAYS);
        inputContainer.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(inputField, Priority.ALWAYS);

        root.getChildren().add(msgLabel);
        root.getChildren().add(inputContainer);
        root.setSpacing(5);

        inputField.setText(inputValue == null ? "" : inputValue);

        a.getDialogPane().setContent(root);

        Optional<ButtonType> response = a.showAndWait();
        if (response.isPresent()) {
            if (response.get() == ButtonType.OK) {
                return inputField.getText();
            }
        }
        return null;
    }


}
