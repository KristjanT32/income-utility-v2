package com.krisapps.incomeutility_v2.ui.listview;

import com.krisapps.incomeutility_v2.ui.listview.cell.CustomCategoryCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import javafx.util.Pair;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CustomCategoryCellFactory implements Callback<ListView<Pair<Integer, String>>, ListCell<Pair<Integer, String>>> {
    private final BiConsumer<Integer, String> onEditRequest;
    private final Consumer<Integer> onDeleteRequest;

    public CustomCategoryCellFactory(BiConsumer<Integer, String> onEditRequest, Consumer<Integer> onDeleteRequest) {
        this.onEditRequest = onEditRequest;
        this.onDeleteRequest = onDeleteRequest;
    }

    @Override
    public ListCell<Pair<Integer, String>> call(ListView<Pair<Integer, String>> param) {
        return new CustomCategoryCell(onEditRequest, onDeleteRequest);
    }
}
