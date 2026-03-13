package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.fiscal.cashew.CashewTransaction;
import com.krisapps.incomeutility_v2.ui.listview.EditableCashewTransactionCellFactory;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;

public class TransactionReviewDialog extends IncomeUtilityDialog<ArrayList<CashewTransaction>> {

    @FXML
    private ListView<CashewTransaction> cashewTransactionView;

    private ArrayList<CashewTransaction> output = new ArrayList<>();
    private ArrayList<UUID> excludedTransactions = new ArrayList<>();

    public TransactionReviewDialog(Account parent, ArrayList<CashewTransaction> transactions) {
        super("review-imported-transactions.fxml", "Converted transactions", "overview_96.png");

        getDialogPane().getButtonTypes().add(new ButtonType("Import", ButtonBar.ButtonData.APPLY));
        setResizable(true);

        Label l = new Label("No transactions have been imported yet.");
        l.getStyleClass().add("medium-label");
        cashewTransactionView.setPlaceholder(l);

        cashewTransactionView.setCellFactory(new EditableCashewTransactionCellFactory(parent, (transaction -> {
            refreshItems(transactions);
        }), transaction -> {
            excludedTransactions.add(transaction.getId());
            transactions.remove(transaction);
            output = transactions;

            refreshItems(transactions);
        }));

        refreshItems(transactions);
        output = transactions;

        setResultConverter((response) -> {
            if (response.getButtonData() == ButtonBar.ButtonData.APPLY) {
                return output;
            } else {
                close();
                return new ArrayList<>();
            }
        });
    }

    private void refreshItems(ArrayList<CashewTransaction> transactions) {
        cashewTransactionView.getItems().setAll(transactions.stream().filter(t -> !excludedTransactions.contains(t.getId())).toList());
        cashewTransactionView.getItems().sort(Comparator.comparing(Transaction::getTimestamp));
    }
}
