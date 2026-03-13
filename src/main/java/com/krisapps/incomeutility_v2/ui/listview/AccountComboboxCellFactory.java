package com.krisapps.incomeutility_v2.ui.listview;

import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.ui.listview.cell.AccountComboboxCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.util.Callback;

public class AccountComboboxCellFactory implements Callback<ListView<Account>, ListCell<Account>> {
    @Override
    public ComboBoxListCell<Account> call(ListView<Account> accountListView) {
        return new AccountComboboxCell();
    }
}
