package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.types.data.ItemizableItem;
import com.krisapps.incomeutility_v2.types.pricer.Product;
import com.krisapps.incomeutility_v2.ui.listview.ItemizeItemCellFactory;
import com.krisapps.incomeutility_v2.util.DataManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class ItemizeTransactionDialog extends IncomeUtilityDialog<HashMap<ItemizableItem, Integer>> {

    @FXML
    private ListView<ItemizableItem> itemList;

    @FXML
    private Button addCustomItemButton;

    @FXML
    private Button addPricerItemButton;

    private final int transactionId;
    private final ArrayList<ItemizableItem> items;
    private final DataManager data = DataManager.getInstance();

    public ItemizeTransactionDialog(int transactionId, List<ItemizableItem> currentItems) {
        super("itemize-transaction.fxml", "Itemize transaction");
        this.transactionId = transactionId;
        this.items = new ArrayList<>(currentItems);

        getDialogPane().getButtonTypes().setAll(
                new ButtonType("Apply", ButtonBar.ButtonData.APPLY),
                new ButtonType(!currentItems.isEmpty() ? "Discard changes" : "Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        initUI();
    }

    private void initUI() {
        itemList.setCellFactory(new ItemizeItemCellFactory(DataManager.getInstance().getPricerCurrencyConfiguration()));
        itemList.getItems().addAll(items);

        addCustomItemButton.setOnAction(_ -> {

        });

        addPricerItemButton.setOnAction(_ -> {
            ProductPickerDialog dialog = new ProductPickerDialog("Select an existing product to add to the transaction", data.getProducts());
            Optional<Product> selection = dialog.showAndWait();

            selection.ifPresent(product -> items.add(ItemizableItem.ofPricerProduct(product, transactionId)));
        });
    }
}
