package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.types.pricer.Product;
import com.krisapps.incomeutility_v2.ui.listview.SimpleProductCellFactory;
import com.krisapps.incomeutility_v2.util.DataManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.List;

public class ProductPickerDialog extends IncomeUtilityDialog<Product> {

    @FXML
    private Label primaryLabel;

    @FXML
    private ListView<Product> itemList;

    public ProductPickerDialog(String title) {
        super("product-picker.fxml", title, "confirm_96.png");

        setResultConverter(r -> {
            if (r.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                return itemList.getSelectionModel().getSelectedItem();
            } else {
                return null;
            }
        });

        getDialogPane().getButtonTypes().addAll(
                new ButtonType("Confirm selection", ButtonBar.ButtonData.APPLY),
                new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        getDialogPane().setMaxWidth(500);
        getDialogPane().setMaxHeight(300);

        itemList.setCellFactory(new SimpleProductCellFactory(_ -> {
        }, DataManager.getInstance().getPricerCurrencyConfiguration(), false));
    }

    public void setPrimaryLabel(String text) {
        primaryLabel.setText(text);
    }

    public void setItems(List<? extends Product> items) {
        itemList.setItems(FXCollections.observableArrayList(items));
    }
}
