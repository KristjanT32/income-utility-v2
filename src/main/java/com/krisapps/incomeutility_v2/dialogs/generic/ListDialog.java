package com.krisapps.incomeutility_v2.dialogs.generic;

import com.krisapps.incomeutility_v2.dialogs.IncomeUtilityDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.util.List;

public class ListDialog<T> extends IncomeUtilityDialog<Void> {

    @FXML
    private ListView<T> listview;

    @FXML
    private Label label;

    @FXML
    private Label sublabel;

    public ListDialog(String title) {
        super("list-dialog.fxml", title, "overview_96.png");
        Platform.runLater(() -> {
            label.managedProperty().bind(label.visibleProperty());
            label.visibleProperty().bind(label.textProperty().isNotEmpty());
            sublabel.managedProperty().bind(sublabel.visibleProperty());
            sublabel.visibleProperty().bind(sublabel.textProperty().isNotEmpty());
        });
    }

    public void setListViewCellFactory(Callback<ListView<T>, ListCell<T>> cellFactory) {
        Platform.runLater(() -> {
            listview.setCellFactory(cellFactory);
        });
    }

    public void setItems(List<T> items) {
        Platform.runLater(() -> {
            listview.setItems(FXCollections.observableList(items));
        });
    }

    public void setLabel(String text) {
        Platform.runLater(() -> {
            label.setText(text);
        });
    }

    public void setSubLabel(String text) {
        Platform.runLater(() -> {
            sublabel.setText(text);
        });
    }
}
