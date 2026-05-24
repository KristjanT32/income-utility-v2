package com.krisapps.incomeutility_v2.util;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.IncomeUtilityController;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

@SuppressWarnings("ConstantConditions")
public class PopupManager {

    static Image INFO_ICON = new Image(IncomeUtilityApplication.class.getResource("icons/info_96.png").toExternalForm());
    static Image CONFIRMATION_ICON = new Image(IncomeUtilityApplication.class.getResource("icons/confirm_96.png").toExternalForm());
    static Image WARNING_ICON = new Image(IncomeUtilityApplication.class.getResource("icons/warning_96.png").toExternalForm());
    static Image ERROR_ICON = new Image(IncomeUtilityApplication.class.getResource("icons/error_96.png").toExternalForm());

    @SuppressWarnings("ConstantConditions")
    public static Optional<ButtonType> showPredefinedPopup(PopupType type) {
        Alert alert = new Alert(null);
        alert.getDialogPane().getStylesheets().add(IncomeUtilityController.class.getResource("stylesheets/core-ui.css").toExternalForm());
        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image(IncomeUtilityApplication.class.getResource("icons/info_96.png").toExternalForm()));
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
        ((Stage) a.getDialogPane().getScene().getWindow()).getIcons().add(CONFIRMATION_ICON);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.getButtonTypes().clear();
        a.getButtonTypes().addAll(optionA, optionB);
        return a.showAndWait();
    }

    public static Optional<ButtonType> showConfirmationAsync(String title, String message, ButtonType optionA, ButtonType optionB) {
        if (Platform.isFxApplicationThread()) {
            return showConfirmation(title, message, optionA, optionB);
        } else {
            FutureTask<Optional<ButtonType>> task = new FutureTask<>(() -> showConfirmation(title, message, optionA, optionB));
            Platform.runLater(task);

            try {
                return task.get();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Optional<ButtonType> showChoicePopup(String title, String message, ButtonType... options) {
        if (Arrays.stream(options).noneMatch(type -> type.getButtonData().isCancelButton())) {
            throw new IllegalArgumentException("Cannot show a confirmation dialog without a cancel option - please ensure that either optionA or optionB is a cancellation ButtonType");
        }

        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.getDialogPane().getStylesheets().add(IncomeUtilityController.class.getResource("stylesheets/core-ui.css").toExternalForm());
        ((Stage) a.getDialogPane().getScene().getWindow()).getIcons().add(CONFIRMATION_ICON);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.getButtonTypes().clear();
        a.getButtonTypes().addAll(options);
        return a.showAndWait();
    }

    public static void showPopup(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(null);
            alert.getDialogPane().getStylesheets().add(IncomeUtilityController.class.getResource("stylesheets/core-ui.css").toExternalForm());
            alert.setTitle(title);
            alert.setContentText(message);
            alert.setAlertType(type);
            alert.setHeaderText(null);

            switch (type) {
                case NONE -> {
                }
                case INFORMATION -> ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(INFO_ICON);
                case WARNING -> ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(WARNING_ICON);
                case CONFIRMATION ->
                        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(CONFIRMATION_ICON);
                case ERROR -> ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(ERROR_ICON);
            }

            alert.showAndWait();
        });
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
        inputField.requestFocus();

        a.getDialogPane().setContent(root);

        Optional<ButtonType> response = a.showAndWait();
        if (response.isPresent()) {
            if (response.get() == ButtonType.OK) {
                return inputField.getText();
            }
        }
        return null;
    }

    public static <T> void showListDialog(String title, String message, List<T> listItems, @Nullable Callback<ListView<T>, ListCell<T>> cellFactory) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);

        a.getDialogPane().getStylesheets().add(IncomeUtilityController.class.getResource("stylesheets/core-ui.css").toExternalForm());
        a.setTitle(title);
        a.setHeaderText(null);

        VBox root = new VBox();
        HBox.setHgrow(root, Priority.ALWAYS);

        ListView<T> list = new ListView<T>();
        list.getItems().setAll(listItems);

        if (cellFactory != null) {
            list.setCellFactory(cellFactory);
        }


        HBox.setHgrow(list, Priority.ALWAYS);
        VBox.setVgrow(list, Priority.SOMETIMES);
        list.setMaxHeight(Double.MAX_VALUE);

        root.getChildren().add(list);
        root.setSpacing(5);

        a.getDialogPane().setContent(root);
        a.getDialogPane().setMinWidth(1200);
        a.getDialogPane().requestLayout();
        a.showAndWait();
    }

    public enum PopupType {
        NOT_IMPLEMENTED,
        INSUFFICIENT_BALANCE,
    }


}
