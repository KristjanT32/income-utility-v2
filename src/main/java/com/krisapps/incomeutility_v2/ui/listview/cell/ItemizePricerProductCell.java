package com.krisapps.incomeutility_v2.ui.listview.cell;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.types.data.ItemizableItem;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.pricer.Product;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ItemizePricerProductCell extends ListCell<ItemizableItem> {
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
    private ComboBox<Product> itemPicker;

    @FXML
    private Button commitButton;

    private final CurrencyConfig currencyConfig;

    public ItemizePricerProductCell(CurrencyConfig currencyConfig) {
        this.currencyConfig = currencyConfig;

        loadFXML();
    }

    private void loadFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/ui/itemize_pricer_product_cell.fxml"));
            loader.setController(this);
            rootPane = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void updateItem(ItemizableItem item, boolean empty) {
        super.updateItem(item, empty);
    }
}
