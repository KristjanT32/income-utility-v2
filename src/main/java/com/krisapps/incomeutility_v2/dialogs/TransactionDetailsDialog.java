package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.transaction.TransactionType;
import com.krisapps.incomeutility_v2.util.DataManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.Optional;

public class TransactionDetailsDialog extends Dialog<Void> {

    @FXML
    private VBox rootPane;

    @FXML
    private Label typeLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private Label timeLabel;

    @FXML
    private Label amountLabel;

    @FXML
    private Label accountLabel;

    @FXML
    private Label categoryLabel;

    @FXML
    private Label commentLabel;

    @FXML
    private Label transferFromLabel;

    @FXML
    private Label transferToLabel;

    @FXML
    private HBox transferPanel;

    @FXML
    private FontIcon typeIcon;

    @FXML
    private Button editButton;



    private final String WITHDRAWAL_ICON = "fltrmz-presence-dnd-10";
    private final String DEPOSIT_ICON = "fltral-add-circle-24";
    private final String TRANSFER_ICON = "fltral-arrow-swap-24";
    private Transaction transaction;

    private final DataManager dataManager = DataManager.getInstance();

    public TransactionDetailsDialog(Transaction t, Account selectedAccount) {
        try {
            FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/dialogs/transaction-details.fxml"));
            loader.setController(this);
            rootPane = loader.load();
            transaction = t;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ButtonType cancelButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().setAll(cancelButton);

        getDialogPane().setContent(rootPane);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Transaction details");

        editButton.setOnAction((ev) -> {
            EditTransactionDialog editDialog = new EditTransactionDialog(transaction.copy(), selectedAccount);
            Optional<Transaction> updated = editDialog.showAndWait();
            updated.ifPresent(value -> {
                DataManager.getInstance().updateTransaction(value.getId(), value);
                close();
            });
        });

        transferPanel.managedProperty().bindBidirectional(transferPanel.visibleProperty());
        amountLabel.managedProperty().bindBidirectional(amountLabel.visibleProperty());

        updateUI();
    }

    private void updateUI() {
        typeLabel.setText(transaction.getType().getDisplayName());
        typeLabel.getStyleClass().removeAll("flair-withdrawal", "flair-deposit", "flair-transfer");
        switch (transaction.getType()) {
            case DEPOSIT -> {
                typeIcon.setIconLiteral(DEPOSIT_ICON);
                typeLabel.getStyleClass().add("flair-deposit");
            }
            case WITHDRAWAL -> {
                typeIcon.setIconLiteral(WITHDRAWAL_ICON);
                typeLabel.getStyleClass().add("flair-withdrawal");
            }
            case TRANSFER -> {
                typeIcon.setIconLiteral(TRANSFER_ICON);
                typeLabel.getStyleClass().add("flair-transfer");
            }
        }

        if (transaction.getType().equals(TransactionType.TRANSFER)) {
            Optional<Account> from = dataManager.getAccount(transaction.getSourceAccountId());
            transferFromLabel.setText(from.isPresent() ? from.get().getName() : "Unknown account");

            Optional<Account> to = dataManager.getAccount(transaction.getTargetAccountId());
            transferToLabel.setText(to.isPresent() ? to.get().getName() : "Unknown account");

            amountLabel.setText(DataManager.Formatting.formatMoney(
                    transaction.getAbsoluteAmount(),
                    to.isPresent() ? to.get().getCurrencyConfig() : CurrencyConfig.DEFAULT
            ));

            transferPanel.setVisible(true);
            accountLabel.setVisible(false);
            accountLabel.setManaged(false);
        } else {
            Optional<Account> acc = dataManager.getAccount(transaction.getTargetAccountId());
            accountLabel.setText(acc.isPresent() ? acc.get().getName() : "Unknown account");

            amountLabel.setText(
                    DataManager.Formatting.formatMoney(
                    transaction.getAbsoluteAmount(),
                    acc.isPresent() ? acc.get().getCurrencyConfig() : CurrencyConfig.DEFAULT
            ));

            transferPanel.setVisible(false);
            accountLabel.setVisible(true);
            accountLabel.setManaged(true);
        }

        dateLabel.setText(DataManager.Formatting.formatLocalDate(transaction.getTimestamp().toLocalDate()));
        timeLabel.setText(DataManager.Formatting.formatLocalTime(transaction.getTimestamp().toLocalTime()));
        commentLabel.setText(transaction.getComment().isEmpty() ? "No comments added." : transaction.getComment());
        categoryLabel.setText(!transaction.getCustomCategory().isEmpty() ? transaction.getCustomCategory() : DataManager.Formatting.capitalize(transaction.getCategory().name()));
    }
}
