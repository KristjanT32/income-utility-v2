package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.ui.listview.AccountComboboxCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.cell.AccountComboboxButtonCell;
import com.krisapps.incomeutility_v2.util.services.CashewService;
import com.krisapps.incomeutility_v2.util.services.FiscalService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import java.io.IOException;
import java.util.UUID;

public class AccountMappingDialog extends Dialog<UUID> {

    @FXML
    private VBox rootPane;

    @FXML
    private ComboBox<Account> accountSelector;

    @FXML
    private Label walletLabel;

    private String externalWalletName;
    private final FiscalService fiscal = FiscalService.getInstance();

    public AccountMappingDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/dialogs/map-external-wallet.fxml"));
            loader.setController(this);
            rootPane = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ButtonType mapButton = new ButtonType("Map", ButtonBar.ButtonData.APPLY);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        getDialogPane().getButtonTypes().setAll(mapButton, cancelButton);

        getDialogPane().setContent(rootPane);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Create external wallet mapping");

        accountSelector.setCellFactory(new AccountComboboxCellFactory());
        accountSelector.setButtonCell(new AccountComboboxButtonCell());
        accountSelector.getItems().setAll(fiscal.getAccounts());

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
