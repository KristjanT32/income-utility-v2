package com.krisapps.incomeutility_v2.ui.listview;

import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.pricer.Dish;
import com.krisapps.incomeutility_v2.ui.listview.cell.DishCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.util.function.Consumer;

public class DishCellFactory implements Callback<ListView<Dish>, ListCell<Dish>> {
    private final Consumer<Dish> onOpenRequest;
    private final Consumer<Dish> onEditRequest;
    private final Consumer<Dish> onDeleteRequest;
    private final CurrencyConfig currencyConfig;
    private final boolean canOpen;
    private final boolean canEdit;
    private final boolean canDelete;

    public DishCellFactory(Consumer<Dish> onOpenRequest, Consumer<Dish> onEditRequest, Consumer<Dish> onDeleteRequest, CurrencyConfig currencyConfig, boolean canOpen, boolean canEdit, boolean canDelete) {
        this.onOpenRequest = onOpenRequest;
        this.onEditRequest = onEditRequest;
        this.onDeleteRequest = onDeleteRequest;
        this.currencyConfig = currencyConfig;
        this.canOpen = canOpen;
        this.canEdit = canEdit;
        this.canDelete = canDelete;
    }

    @Override
    public ListCell<Dish> call(ListView<Dish> param) {
        return new DishCell(onOpenRequest, onEditRequest, onDeleteRequest, currencyConfig, canOpen, canEdit, canDelete);
    }
}
