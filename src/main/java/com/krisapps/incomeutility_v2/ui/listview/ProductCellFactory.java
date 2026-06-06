package com.krisapps.incomeutility_v2.ui.listview;

import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.pricer.Product;
import com.krisapps.incomeutility_v2.ui.listview.cell.ProductCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.util.function.Consumer;

public class ProductCellFactory implements Callback<ListView<Product>, ListCell<Product>> {
    private final Consumer<Product> onAddRequest;
    private final Consumer<Product> onEditRequest;
    private final Consumer<Product> onDeleteRequest;
    private final CurrencyConfig currencyConfig;
    private final boolean showAddButton;
    private final boolean allowModify;

    public ProductCellFactory(Consumer<Product> onAddRequest, Consumer<Product> onEditRequest, Consumer<Product> onDeleteRequest, CurrencyConfig currencyConfig, boolean showAddButton, boolean allowModify) {
        this.onAddRequest = onAddRequest;
        this.onEditRequest = onEditRequest;
        this.onDeleteRequest = onDeleteRequest;
        this.currencyConfig = currencyConfig;
        this.showAddButton = showAddButton;
        this.allowModify = allowModify;
    }

    @Override
    public ListCell<Product> call(ListView<Product> param) {
        return new ProductCell(onAddRequest, onEditRequest, onDeleteRequest, currencyConfig, showAddButton, allowModify);
    }
}
