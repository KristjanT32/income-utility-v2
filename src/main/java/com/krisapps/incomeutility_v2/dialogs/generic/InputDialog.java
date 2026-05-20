package com.krisapps.incomeutility_v2.dialogs.generic;

import com.krisapps.incomeutility_v2.dialogs.IncomeUtilityDialog;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class InputDialog extends IncomeUtilityDialog<String> {

    @FXML
    private TextField inputField;

    @FXML
    private Label primaryLabel;

    @FXML
    private Label descriptionLabel;


    public InputDialog(String title) {
        super("input-dialog.fxml", title, "info_96.png");

        setResultConverter(r -> {
            if (r.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                return inputField.getText();
            } else {
                return null;
            }
        });

        getDialogPane().getButtonTypes().addAll(
                new ButtonType("Confirm", ButtonBar.ButtonData.APPLY),
                new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        getDialogPane().setMaxWidth(500);
        getDialogPane().setMaxHeight(200);
    }

    public void setTextFormatter(TextFormatter<?> formatter) {
        this.inputField.setTextFormatter(formatter);
    }

    public void setPrimaryLabel(String text) {
        primaryLabel.setText(text);
    }

    public void setDescription(String text) {
        descriptionLabel.setText(text);
    }

    public void setPrompt(String text) {
        inputField.setPromptText(text);
    }

    public void setInitialValue(String text) {
        inputField.setText(text);
    }
}
