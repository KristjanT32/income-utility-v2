package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.ui.listview.AccountComboboxCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.cell.AccountComboboxButtonCell;
import com.krisapps.incomeutility_v2.util.services.CashewService;
import com.krisapps.incomeutility_v2.util.services.FiscalService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import java.io.IOException;
import java.util.UUID;

public class AccountMappingDialog extends IncomeUtilityDialog<UUID> {

    @FXML
    private VBox rootPane;

    @FXML
    private ComboBox<Account> accountSelector;

    @FXML
    private Label walletLabel;

    private String externalWalletName;
    private final FiscalService fiscal = FiscalService.getInstance();

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
    }

    public void setExternalAccountName(String name) {
        this.externalWalletName = name;
        Platform.runLater(() -> {
            walletLabel.setText(externalWalletName);
        });
    }
}
