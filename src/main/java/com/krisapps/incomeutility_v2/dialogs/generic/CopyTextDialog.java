package com.krisapps.incomeutility_v2.dialogs.generic;

import com.krisapps.incomeutility_v2.dialogs.IncomeUtilityDialog;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;

import java.util.Map;

public class CopyTextDialog extends IncomeUtilityDialog<Void> {

    @FXML
    private TextArea textArea;

    @FXML
    private Label primaryLabel;

    public CopyTextDialog(String title) {
        super("copyable-dialog.fxml", title);

        getDialogPane().getButtonTypes().setAll(new ButtonType("Copy and close", ButtonBar.ButtonData.APPLY));

        setResultConverter((button) -> {
            if (button != null && button.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, textArea.getText()));
            }
            return null;
        });
    }

    public void setContent(String content) {
        textArea.setText(content);
    }

    public void setPrimaryLabel(String text) {
        primaryLabel.setText(text);
    }
}
