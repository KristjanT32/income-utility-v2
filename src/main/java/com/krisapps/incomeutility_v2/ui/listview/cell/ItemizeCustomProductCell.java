package com.krisapps.incomeutility_v2.ui.listview.cell;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.types.data.ItemizableItem;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ItemizeCustomProductCell extends ListCell<ItemizableItem> {
    @FXML
    private VBox rootPane;

    @FXML
    private Label productNameLabel;

    @FXML
    private Label priceLabel;

    @FXML
    private HBox itemInfoPanel;

    @FXML
    private HBox itemSetupPanel;

    @FXML
    private TextField nameField;

    @FXML
    private TextField priceField;

    @FXML
    private Button commitButton;

    @FXML
    private Button editButton;

    private final CurrencyConfig currencyConfig;

    public ItemizeCustomProductCell(CurrencyConfig currencyConfig) {
        this.currencyConfig = currencyConfig;

        loadFXML();
    }

    private void loadFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/ui/itemize_custom_product_cell.fxml"));
            loader.setController(this);
            rootPane = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public VBox getRootPane() {
        return rootPane;
    }

    @Override
    public void updateItem(ItemizableItem item, boolean empty) {
        super.updateItem(item, empty);
    }
}
