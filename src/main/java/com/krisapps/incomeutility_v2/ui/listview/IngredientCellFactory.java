package com.krisapps.incomeutility_v2.ui.listview;

import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.pricer.DishIngredient;
import com.krisapps.incomeutility_v2.types.pricer.Product;
import com.krisapps.incomeutility_v2.ui.listview.cell.IngredientCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class IngredientCellFactory implements Callback<ListView<DishIngredient>, ListCell<DishIngredient>> {
    private final BiConsumer<Product, Double> onQuantityChangeRequest;
    private final Consumer<Product> onDeleteRequest;
    private final CurrencyConfig currencyConfig;

    public IngredientCellFactory(BiConsumer<Product, Double> onQuantityChangeRequest, Consumer<Product> onDeleteRequest, CurrencyConfig currencyConfig) {
        this.onQuantityChangeRequest = onQuantityChangeRequest;
        this.onDeleteRequest = onDeleteRequest;
        this.currencyConfig = currencyConfig;
    }

    @Override
    public ListCell<DishIngredient> call(ListView<DishIngredient> param) {
        return new IngredientCell(onQuantityChangeRequest, onDeleteRequest, currencyConfig);
    }
}
