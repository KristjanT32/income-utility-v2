package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.IncomeUtilityController;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class LoadingDialog extends Dialog<Void> {

    @FXML
    private VBox rootPane;

    @FXML
    private Label primaryLabel;

    @FXML
    private Label secondaryLabel;

    @FXML
    private ProgressIndicator spinner;

    @FXML
    private ProgressBar progressbar;

    public enum LoadingOperationType {
        INDETERMINATE_SPINNER,
        INDETERMINATE_PROGRESSBAR,
        DETERMINATE
    }

    private LoadingOperationType type;
    private Task<Void> operation;

    public LoadingDialog(LoadingOperationType type) {
        FXMLLoader fxmlLoader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/dialogs/loading.fxml"));
        fxmlLoader.setController(this);
        try {
            rootPane = fxmlLoader.load();
            this.type = type;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getDialogPane().setContent(rootPane);

        // Trickery to be able to close the dialog manually
        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        Node b = getDialogPane().lookupButton(ButtonType.CANCEL);
        b.setVisible(false);
        b.setManaged(false);
    }

    public void setProgress(double progress) {
        Platform.runLater(() -> {
            this.progressbar.setProgress(progress);
        });
    }

    public void setPrimaryLabel(String text) {
        Platform.runLater(() -> {
            this.primaryLabel.setText(text);
        });
    }

    public void setSecondaryLabel(String text) {
        Platform.runLater(() -> {
            this.secondaryLabel.setText(text);
        });
    }

    public void show(String title, Runnable task) {
        this.operation = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    task.run();
                    return null;
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return null;
            }
        };

        this.operation.setOnSucceeded(event -> this.close());

        this.operation.setOnFailed((event -> this.close()));

        setTitle(title);

        this.progressbar.setVisible(false);
        this.progressbar.setManaged(false);
        this.spinner.setVisible(false);
        this.spinner.setManaged(false);

        switch (type) {
            case INDETERMINATE_SPINNER -> {
                this.spinner.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                this.spinner.setVisible(true);
                this.spinner.setManaged(true);
            }
            case INDETERMINATE_PROGRESSBAR -> {
                this.progressbar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                this.progressbar.setVisible(true);
                this.progressbar.setManaged(true);
            }
            case DETERMINATE -> {
                this.progressbar.setProgress(0d);
                this.progressbar.setVisible(true);
                this.progressbar.setManaged(true);
            }
        }

        IncomeUtilityController.scheduler.submit(this.operation);
        showAndWait();
    }

}
