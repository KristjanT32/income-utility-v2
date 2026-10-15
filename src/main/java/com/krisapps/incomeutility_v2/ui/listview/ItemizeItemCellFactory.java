package com.krisapps.incomeutility_v2.ui.listview;

import com.krisapps.incomeutility_v2.types.data.ItemizableItem;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.ui.listview.cell.TransactionItemCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

public class ItemizeItemCellFactory implements Callback<ListView<ItemizableItem>, ListCell<ItemizableItem>> {
    private final CurrencyConfig currencyConfig;

    public ItemizeItemCellFactory(CurrencyConfig currencyConfig) {
        this.currencyConfig = currencyConfig;
    }

    @Override
    public ListCell<ItemizableItem> call(ListView<ItemizableItem> param) {
        return new TransactionItemCell(currencyConfig);
    }
}
