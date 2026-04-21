package com.krisapps.incomeutility_v2.ui.listview;

import com.krisapps.incomeutility_v2.types.data.CategoryExpenseSummary;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.ui.listview.cell.CategoryExpenseTotalCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

public class CategoryExpenseTotalCellFactory implements Callback<ListView<CategoryExpenseSummary>, ListCell<CategoryExpenseSummary>> {
    private final Account parent;

    public CategoryExpenseTotalCellFactory(Account parent) {
        this.parent = parent;
    }

    @Override
    public ListCell<CategoryExpenseSummary> call(ListView<CategoryExpenseSummary> param) {
        return new CategoryExpenseTotalCell(parent);
    }
}
