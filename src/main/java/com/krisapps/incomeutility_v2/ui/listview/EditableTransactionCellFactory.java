package com.krisapps.incomeutility_v2.ui.listview;

import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.ui.listview.cell.EditableTransactionCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.util.function.Consumer;

public class EditableTransactionCellFactory implements Callback<ListView<Transaction>, ListCell<Transaction>> {

    private final Account parent;
    private final Consumer<Transaction> onItemDataChange;
    private final Consumer<Transaction> onItemDeleted;

    public EditableTransactionCellFactory(Account parent, Consumer<Transaction> onItemDataChange, Consumer<Transaction> onItemDeleted) {
        this.parent = parent;
        this.onItemDataChange = onItemDataChange;
        this.onItemDeleted = onItemDeleted;
    }

    @Override
    public ListCell<Transaction> call(ListView<Transaction> transactionListView) {
        return new EditableTransactionCell(parent, onItemDataChange, onItemDeleted);
    }
}
