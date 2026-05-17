package com.krisapps.incomeutility_v2.ui.listview.cell;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.util.PopupManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CustomCategoryCell extends ListCell<Pair<Integer, String>> {

    @FXML
    private VBox rootPane;

    @FXML
    private Label categoryNameLabel;

    @FXML
    private Button deleteButton;

    private final BiConsumer<Integer, String> onEditRequest;
    private final Consumer<Integer> onDeleteRequest;

    public CustomCategoryCell(BiConsumer<Integer, String> onEditRequest, Consumer<Integer> onDeleteRequest) {
        this.onEditRequest = onEditRequest;
        this.onDeleteRequest = onDeleteRequest;
        loadFXML();
    }

    private void loadFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/ui/custom_category_cell.fxml"));
            loader.setController(this);
            rootPane = loader.load();

            categoryNameLabel.setOnMouseClicked((e) -> {
                String newValue = PopupManager.showInputDialog(
                        "Edit custom category",
                        "Please enter the new value for the category in the box below.",
                        "New value: ",
                        getItem().getValue()
                );
                if (newValue != null && !newValue.isBlank()) {
                    onEditRequest.accept(getItem().getKey(), newValue);
                }
            });

            deleteButton.setOnAction((e) -> {
                onDeleteRequest.accept(getItem().getKey());
            });

            categoryNameLabel.setCursor(Cursor.TEXT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void updateItem(Pair<Integer, String> item, boolean empty) {
        super.updateItem(item, empty);

        if (!empty) {
            categoryNameLabel.setText(item.getValue());
            categoryNameLabel.setStyle("-fx-text-fill: black");
            setGraphic(rootPane);
        } else {
            setText(null);
            setGraphic(null);
        }
    }
}
