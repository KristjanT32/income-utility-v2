package com.krisapps.incomeutility_v2.dialogs.generic;

import com.krisapps.incomeutility_v2.IncomeUtilityController;
import com.krisapps.incomeutility_v2.dialogs.IncomeUtilityDialog;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

public class LoadingDialog extends IncomeUtilityDialog<Void> {

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
    private final LoadingOperationType type;
    private Task<Void> operation;
    public LoadingDialog(LoadingOperationType type) {
        super("loading.fxml", "");
        this.type = type;
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

    public enum LoadingOperationType {
        INDETERMINATE_SPINNER,
        INDETERMINATE_PROGRESSBAR,
        DETERMINATE
    }

}
