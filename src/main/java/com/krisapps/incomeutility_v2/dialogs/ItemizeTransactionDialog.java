package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.types.data.ItemizableItem;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.pricer.Product;
import com.krisapps.incomeutility_v2.ui.listview.ItemizeItemCellFactory;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.Formatting;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemizeTransactionDialog extends IncomeUtilityDialog<ArrayList<ItemizableItem>> {

    @FXML
    private ListView<ItemizableItem> itemList;

    @FXML
    private Button addCustomItemButton;

    @FXML
    private Button addPricerItemButton;

    @FXML
    private Label itemizedTotalLabel;

    @FXML
    private Label unitemizedTotalLabel;

    private final Transaction transaction;
    private final ArrayList<ItemizableItem> items;
    private final DataManager data = DataManager.getInstance();

    public ItemizeTransactionDialog(Transaction t, List<ItemizableItem> currentItems) {
        super("itemize-transaction.fxml", "Itemize transaction");
        this.transaction = t;
        this.items = new ArrayList<>(currentItems);

        getDialogPane().getButtonTypes().setAll(
                new ButtonType("Apply", ButtonBar.ButtonData.APPLY),
                new ButtonType(!currentItems.isEmpty() ? "Discard changes" : "Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        setResultConverter(button -> {
            if (button.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                return items;
            } else {
                return null;
            }
        });

        initUI();
    }

    private void refreshUI() {
        double itemizedTotal = items.stream().mapToDouble(ItemizableItem::price).sum();
        itemizedTotalLabel.setText(Formatting.formatMoney(itemizedTotal));
        unitemizedTotalLabel.setText(Formatting.formatMoney(transaction.getAbsoluteAmount() - itemizedTotal));
        itemList.getItems().setAll(items);
        itemList.refresh();
    }

    private void initUI() {
        itemList.setCellFactory(new ItemizeItemCellFactory(DataManager.getInstance().getPricerCurrencyConfiguration(), (oldItem, newItem) -> {
            items.removeIf(i -> i.isEmpty() || i.is(oldItem));
            items.add(newItem);
            refreshUI();
        }, (item) -> {
            items.removeIf(i -> i.is(item));
            refreshUI();
        }));

        addCustomItemButton.setOnAction(_ -> {
            items.add(new ItemizableItem(-1, transaction.getDatabaseId(), -1, null, null));
            itemList.getItems().setAll(items);
        });

        addPricerItemButton.setOnAction(_ -> {
            ProductPickerDialog dialog = new ProductPickerDialog("Product picker");
            dialog.setPrimaryLabel("Select an existing product to add to the transaction");
            dialog.setItems(data.getProducts());
            Optional<Product> selection = dialog.showAndWait();

            selection.ifPresent(product -> items.add(ItemizableItem.ofPricerProduct(product, transaction.getDatabaseId())));
            itemList.getItems().setAll(items);
        });
        refreshUI();
    }
}
