package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.ui.listview.EditableTransactionCellFactory;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;

public class AddMultipleTransactionsDialog extends IncomeUtilityDialog<ArrayList<Transaction>> {

    @FXML
    private ListView<Transaction> transactionView;

    @FXML
    private Button addTransactionButton;

    private ArrayList<Transaction> output = new ArrayList<>();

    public AddMultipleTransactionsDialog(Account parent, LocalDate selectedDate) {
        super("add-multiple-transactions.fxml", "Add multiple transactions", "overview_96.png");

        getDialogPane().getButtonTypes().add(new ButtonType("Import", ButtonBar.ButtonData.APPLY));
        setResizable(true);

        VBox box = new VBox();
        HBox.setHgrow(box, Priority.ALWAYS);

        box.setAlignment(Pos.CENTER);
        box.setSpacing(10);

        Label l = new Label("No transactions have been added yet.");
        l.getStyleClass().add("medium-label");

        Button add = new Button("Add transaction");
        add.setOnAction((ev) -> {
            addEmptyTransaction(parent, selectedDate);
        });

        addTransactionButton.setOnAction((ev) -> {
            addEmptyTransaction(parent, selectedDate);
        });

        box.getChildren().add(l);
        box.getChildren().add(add);
        transactionView.setPlaceholder(box);

        transactionView.setCellFactory(new EditableTransactionCellFactory(parent, (transaction -> {
            refreshItems(output);
        }), transaction -> {
            output.remove(transaction);

            refreshItems(output);
        }));

        refreshItems(output);

        setResultConverter((response) -> {
            if (response.getButtonData() == ButtonBar.ButtonData.APPLY) {
                return output;
            } else {
                close();
                return new ArrayList<>();
            }
        });
    }

    private void addEmptyTransaction(Account parent, LocalDate selectedDate) {
        Transaction t = new Transaction();
        t.setDate(selectedDate);
        t.setSourceAccountId(parent.getId());
        t.setTargetAccountId(parent.getId());
        output.add(t);
        refreshItems(output);
    }

    private void refreshItems(ArrayList<Transaction> transactions) {
        transactionView.getItems().setAll(transactions);
        transactionView.getItems().sort(Comparator.comparing(t -> t.getTimestamp() == null ? LocalDateTime.now() : t.getTimestamp()));
    }
}
