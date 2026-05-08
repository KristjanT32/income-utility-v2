package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.PopupManager;
import com.krisapps.incomeutility_v2.util.services.FiscalService;
import com.krisapps.incomeutility_v2.util.services.TransactionService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

public class AccountInfoDialog extends IncomeUtilityDialog<Void>{

    @FXML
    private Label nameLabel;

    @FXML
    private Label typeLabel;

    @FXML
    private Label initialBalanceLabel;

    @FXML
    private Label currentBalanceLabel;

    @FXML
    private Label totalTransactionsLabel;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    private Account account;

    final FiscalService fiscal = FiscalService.getInstance();

    public AccountInfoDialog(Account account) {
        super("account-details.fxml", "Account details", "account_96.png");
        this.account = account;

        refreshUI(account);

        editButton.setOnAction((ev) -> {
            EditAccountWizard dlg = new EditAccountWizard(account);
            dlg.showAndWait().ifPresent((response -> {
                DataManager.getInstance().updateAccount(account.getId(), response);
                this.account = response;
                refreshUI(account);
            }));
        });

        deleteButton.setOnAction((ev) -> {
            PopupManager.showConfirmation("Delete account?",
                    "Are you sure you wish to delete '" + account.getName() + "' ?\n\nThis will also delete all its associated transactions.\nIncoming transfers from this account will be converted to deposits and outgoing transfers to this account will be converted to withdrawals.",
                    new ButtonType("Yes, delete", ButtonBar.ButtonData.APPLY),
                    new ButtonType("No, cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
            ).ifPresent((response) -> {
                if (response.getButtonData() == ButtonBar.ButtonData.APPLY) {
                    DataManager.getInstance().deleteAccount(account.getId());
                    LoadingDialog dialog = new LoadingDialog(LoadingDialog.LoadingOperationType.INDETERMINATE_PROGRESSBAR);
                    dialog.setPrimaryLabel("Deleting account");
                    dialog.setSecondaryLabel("Deleting transactions...");
                    dialog.show("Please wait", () -> {
                        TransactionService.getInstance().deleteTransactionsFor(account);
                    });
                }
                close();
            });
        });
    }

    private void refreshUI(Account account) {
        nameLabel.setText(account.getName());
        typeLabel.setText(account.getType().getDisplayName());
        initialBalanceLabel.setText(DataManager.Formatting.formatMoney(account.getInitialBalance(), account.getCurrencyConfig()));
        currentBalanceLabel.setText(DataManager.Formatting.formatMoney(fiscal.getCurrentBalance(account), account.getCurrencyConfig()));
        totalTransactionsLabel.setText(String.valueOf(fiscal.getTransactions(account).size()));
    }
}
