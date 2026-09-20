package com.krisapps.incomeutility_v2.ui.listview;

import com.krisapps.incomeutility_v2.types.data.ItemizableItem;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.ui.listview.cell.ItemizeCustomProductCell;
import com.krisapps.incomeutility_v2.ui.listview.cell.ItemizePricerProductCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

public class ItemizeItemCellFactory implements Callback<ListView<ItemizableItem>, ListCell<ItemizableItem>> {
    private CurrencyConfig currencyConfig;

    public ItemizeItemCellFactory(CurrencyConfig currencyConfig) {
        this.currencyConfig = currencyConfig;
    }

    @Override
    public ListCell<ItemizableItem> call(ListView<ItemizableItem> param) {
        return new ListCell<>() {
            private ItemizeCustomProductCell customCell;
            private ItemizePricerProductCell pricerCell;

            @Override
            protected void updateItem(ItemizableItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    if (item.isPricerProduct()) {
                        if (pricerCell == null) {
                            pricerCell = new ItemizePricerProductCell(currencyConfig);
                        }
                        pricerCell.updateItem(item, empty);
                        setGraphic(pricerCell.getRootPane());
                    } else {
                        if (customCell == null) {
                            customCell = new ItemizeCustomProductCell(currencyConfig);
                        }
                        customCell.updateItem(item, empty);
                        setGraphic(customCell.getRootPane());
                    }
                }
            }
        };
    }
}
