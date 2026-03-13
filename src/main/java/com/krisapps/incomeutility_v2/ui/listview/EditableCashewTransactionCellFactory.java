package com.krisapps.incomeutility_v2.ui.listview;

import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.fiscal.cashew.CashewTransaction;
import com.krisapps.incomeutility_v2.ui.listview.cell.EditableCashewTransactionCell;
import com.krisapps.incomeutility_v2.ui.listview.cell.TransactionCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.util.function.Consumer;

public class EditableCashewTransactionCellFactory implements Callback<ListView<CashewTransaction>, ListCell<CashewTransaction>> {

    private final Account parent;
    private final Consumer<CashewTransaction> onItemDataChange;
    private final Consumer<CashewTransaction> onItemDeleted;

    public EditableCashewTransactionCellFactory(Account parent, Consumer<CashewTransaction> onItemDataChange, Consumer<CashewTransaction> onItemDeleted) {
        this.parent = parent;
        this.onItemDataChange = onItemDataChange;
        this.onItemDeleted = onItemDeleted;
    }

    @Override
    public ListCell<CashewTransaction> call(ListView<CashewTransaction> transactionListView) {
        return new EditableCashewTransactionCell(parent, onItemDataChange, onItemDeleted);
    }
}
