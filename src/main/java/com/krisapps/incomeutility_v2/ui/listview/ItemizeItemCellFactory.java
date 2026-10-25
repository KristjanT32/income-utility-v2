package com.krisapps.incomeutility_v2.ui.listview;

import com.krisapps.incomeutility_v2.types.data.ItemizableItem;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.ui.listview.cell.TransactionItemCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ItemizeItemCellFactory implements Callback<ListView<ItemizableItem>, ListCell<ItemizableItem>> {
    private final CurrencyConfig currencyConfig;
    private final BiConsumer<ItemizableItem, ItemizableItem> onItemUpdateRequest;
    private final Consumer<ItemizableItem> onItemDeleteRequest;

    public ItemizeItemCellFactory(CurrencyConfig currencyConfig, BiConsumer<ItemizableItem, ItemizableItem> onItemUpdateRequest, Consumer<ItemizableItem> onItemDeleteRequest) {
        this.currencyConfig = currencyConfig;
        this.onItemUpdateRequest = onItemUpdateRequest;
        this.onItemDeleteRequest = onItemDeleteRequest;
    }

    @Override
    public ListCell<ItemizableItem> call(ListView<ItemizableItem> param) {
        return new TransactionItemCell(currencyConfig, onItemUpdateRequest, onItemDeleteRequest);
    }
}
