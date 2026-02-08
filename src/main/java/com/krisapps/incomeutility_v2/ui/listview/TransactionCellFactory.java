package com.krisapps.incomeutility_v2.ui.listview;

import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.ui.listview.cell.TransactionCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

public class TransactionCellFactory implements Callback<ListView<Transaction>, ListCell<Transaction>> {
    @Override
    public ListCell<Transaction> call(ListView<Transaction> transactionListView) {
        return new TransactionCell();
    }
}
