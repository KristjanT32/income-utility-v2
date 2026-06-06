package com.krisapps.incomeutility_v2.ui.listview;

import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.pricer.Product;
import com.krisapps.incomeutility_v2.ui.listview.cell.SimpleProductCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.util.function.Consumer;

public class SimpleProductCellFactory implements Callback<ListView<Product>, ListCell<Product>> {
    private final Consumer<Product> onAddRequest;
    private final CurrencyConfig currencyConfig;
    private final boolean showAddButton;

    public SimpleProductCellFactory(Consumer<Product> onAddRequest, CurrencyConfig currencyConfig, boolean showAddButton) {
        this.onAddRequest = onAddRequest;
        this.currencyConfig = currencyConfig;
        this.showAddButton = showAddButton;
    }

    @Override
    public ListCell<Product> call(ListView<Product> param) {
        return new SimpleProductCell(onAddRequest, currencyConfig, showAddButton);
    }
}
