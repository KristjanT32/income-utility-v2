package com.krisapps.incomeutility_v2.dialogs.generic;

import com.krisapps.incomeutility_v2.dialogs.IncomeUtilityDialog;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.util.List;

public class DropdownDialog<T> extends IncomeUtilityDialog<T> {

    @FXML
    private ComboBox<T> comboBox;

    @FXML
    private Label primaryLabel;

    @FXML
    private Label descriptionLabel;

    public DropdownDialog(String title) {
        super("dropdown-dialog.fxml", title, "confirm_96.png");

        setResultConverter(r -> {
            if (r.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                return comboBox.getValue();
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

    public void setPrimaryLabel(String text) {
        primaryLabel.setText(text);
    }

    public void setDescription(String text) {
        descriptionLabel.setText(text);
    }

    public void setItems(List<? extends T> items) {
        comboBox.setItems(FXCollections.observableArrayList(items));
    }

    public void setSelectedItem(T item) {
        comboBox.getSelectionModel().select(item);
    }

    public void setCellFactory(Callback<ListView<T>, ListCell<T>> factory, ListCell<T> cell) {
        comboBox.setCellFactory(factory);
        comboBox.setButtonCell(cell);
    }

    public void setStringConverter(StringConverter<T> converter) {
        comboBox.setConverter(converter);
    }
}
