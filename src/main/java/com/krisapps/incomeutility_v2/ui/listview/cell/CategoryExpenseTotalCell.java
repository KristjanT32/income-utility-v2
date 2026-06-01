package com.krisapps.incomeutility_v2.ui.listview.cell;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.dialogs.generic.ListDialog;
import com.krisapps.incomeutility_v2.types.data.CategorySummary;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.ui.listview.TransactionCellFactory;
import com.krisapps.incomeutility_v2.util.DataManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class CategoryExpenseTotalCell extends ListCell<CategorySummary> {

    @FXML
    private VBox rootPane;

    @FXML
    private Label totalLabel;

    @FXML
    private Label entryCountLabel;

    @FXML
    private Label categoryNameLabel;

    @FXML
    private Button showTransactionsButton;

    private final Account parent;

    public CategoryExpenseTotalCell(Account parent) {
        this.parent = parent;
        loadFXML();
    }

    private void loadFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/ui/category_summary_cell.fxml"));
            loader.setController(this);
            rootPane = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void updateItem(CategorySummary item, boolean empty) {
        super.updateItem(item, empty);

        showTransactionsButton.setOnAction(ev -> {
            ListDialog<Transaction> transactionListDialog = new ListDialog<>("Category transactions");
            transactionListDialog.setListViewCellFactory(new TransactionCellFactory(parent, (_) -> {}, true));
            transactionListDialog.setItems(item.getTransactions());
            transactionListDialog.setLabel("Transactions marked");
            transactionListDialog.setSubLabel(item.getCategoryName());
            transactionListDialog.show();
        });

        if (!empty) {
            categoryNameLabel.setText(item.getCategoryName());
            entryCountLabel.setText("Transactions in this category: " + item.getTransactionCount());

            totalLabel.setText(DataManager.Formatting.formatMoney(item.sumTransactions(), parent.getCurrencyConfig()));

            categoryNameLabel.setStyle("-fx-text-fill: black");
            entryCountLabel.setStyle("-fx-text-fill: black");
            totalLabel.setStyle("-fx-text-fill: black");

            setGraphic(rootPane);
        } else {
            setText(null);
            setGraphic(null);
        }
    }
}
