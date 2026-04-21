package com.krisapps.incomeutility_v2.ui.listview;

import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.ui.listview.cell.TransactionCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.util.function.Consumer;

public class TransactionCellFactory implements Callback<ListView<Transaction>, ListCell<Transaction>> {

    private final Account parent;
    private final Consumer<Transaction> onItemDataChange;
    private boolean isReadOnly = false;

    public TransactionCellFactory(Account parent, Consumer<Transaction> onItemDataChange) {
        this.parent = parent;
        this.onItemDataChange = onItemDataChange;
        this.isReadOnly = false;
    }

    public TransactionCellFactory(Account parent, Consumer<Transaction> onItemDataChange, boolean readOnly) {
        this.parent = parent;
        this.onItemDataChange = onItemDataChange;
        this.isReadOnly = readOnly;
    }

    @Override
    public ListCell<Transaction> call(ListView<Transaction> transactionListView) {
        return new TransactionCell(parent, onItemDataChange, isReadOnly);
    }
}
