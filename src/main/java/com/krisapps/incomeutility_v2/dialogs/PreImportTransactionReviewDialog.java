package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.fiscal.cashew.CashewTransaction;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.ArrayList;

public class PreImportTransactionReviewDialog extends IncomeUtilityDialog<ArrayList<CashewTransaction>>{

    @FXML
    private ListView<CashewTransaction> cashewTransactionView;

    private ArrayList<CashewTransaction> output = new ArrayList<>();

    public PreImportTransactionReviewDialog(ArrayList<CashewTransaction> transactions) {
        super("review-imported-transactions.fxml", "Converted transactions");

        getDialogPane().getButtonTypes().add(new ButtonType("Import", ButtonBar.ButtonData.APPLY));

        Label l = new Label("No transactions have been imported yet.");
        l.getStyleClass().add("medium-label");
        cashewTransactionView.setPlaceholder(l);
        cashewTransactionView.getItems().setAll(transactions);
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
}
