package com.krisapps.incomeutility_v2.subutilities.money_flow;

import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.TransactionService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.util.HashSet;

/* Controller class for the Money In Money Out utility */
public class MoneyFlowUtilityController {

    private static final DataManager data = DataManager.getInstance();
    private static final TransactionService transactor = TransactionService.getInstance();

    //<editor-fold desc="UI">
    @FXML
    private Label startingBalanceLabel;

    @FXML
    private Label inflowLabel;

    @FXML
    private Label outflowLabel;

    @FXML
    private Label changeLabel;

    @FXML
    private ComboBox<Account> accountSelector;

    @FXML
    private DatePicker datePicker;
    //</editor-fold>




    private static void log(String message) {
        DataManager.log("[Money In Money Out] " + message);
    }


    @FXML
    public void initialize() {
        log("Initializing fiscal data...");
        initUI();
        refreshUI();
    }

    public void initUI() {
        HashSet<Account> accounts = data.getAccounts();
        accountSelector.setConverter(new StringConverter<Account>() {
            @Override
            public String toString(Account account) {
                if (account == null) {
                    return "Select account";
                }
                return account.getName();
            }

            @Override
            public Account fromString(String s) {
                return null;
            }
        });

        accountSelector.getItems().setAll(accounts);
    }

    public void refreshUI() {

    }
}
