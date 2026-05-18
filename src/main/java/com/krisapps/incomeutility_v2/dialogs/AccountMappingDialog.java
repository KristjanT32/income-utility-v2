package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.ui.listview.AccountComboboxCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.cell.AccountComboboxButtonCell;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.services.FiscalService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.UUID;

public class AccountMappingDialog extends IncomeUtilityDialog<UUID> {

    private final FiscalService fiscal = FiscalService.getInstance();
    @FXML
    private VBox rootPane;
    @FXML
    private ComboBox<Account> accountSelector;
    @FXML
    private Label walletLabel;
    private String externalWalletName;

    public AccountMappingDialog() {
        super("map-external-wallet.fxml", "Create external wallet mapping");

        ButtonType mapButton = new ButtonType("Apply and import", ButtonBar.ButtonData.APPLY);
        ButtonType cancelButton = new ButtonType("Skip", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().setAll(mapButton, cancelButton);

        Node b = getDialogPane().lookupButton(mapButton);
        b.setDisable(accountSelector.getValue() == null);

        accountSelector.setCellFactory(new AccountComboboxCellFactory());
        accountSelector.setButtonCell(new AccountComboboxButtonCell());
        accountSelector.getItems().setAll(fiscal.getAccounts());
        accountSelector.valueProperty().addListener((obs, old, val) -> {
            b.setDisable(accountSelector.getValue() == null);
        });

        setResultConverter((response) -> {
            if (response.getButtonData() == ButtonBar.ButtonData.APPLY) {
                return accountSelector.getValue().getId();
            } else {
                return null;
            }
        });

        setOnShown((e) -> {
            selectAccountWithSimilarName();
        });
    }

    private void selectAccountWithSimilarName() {
        for (String accountName: fiscal.getAccounts().stream().map(Account::getName).map(String::toLowerCase).toList()) {
            if (accountName.equals(externalWalletName.toLowerCase())) {
                selectAccountByName(accountName);
                break;
            }

            if (accountName.contains(externalWalletName.toLowerCase())) {
                selectAccountByName(accountName);
                break;
            }
        }
    }

    private void selectAccountByName(String account) {
        for (int i = 0; i < accountSelector.getItems().size(); i++) {
            if (accountSelector.getItems().get(i).getName().toLowerCase().equals(account)) {
                accountSelector.getSelectionModel().select(i);
                DataManager.log("Auto-selected local account '%s' for Cashew account '%s'".formatted(accountSelector.getItems().get(i), account));
                break;
            }
        }
    }

    public void setExternalAccountName(String name) {
        this.externalWalletName = name;
        Platform.runLater(() -> {
            walletLabel.setText(externalWalletName);
        });
    }
}
