package com.krisapps.incomeutility_v2;

import com.krisapps.incomeutility_v2.dialogs.AccountInfoDialog;
import com.krisapps.incomeutility_v2.dialogs.AddAccountWizard;
import com.krisapps.incomeutility_v2.dialogs.ImportFromCashewDialog;
import com.krisapps.incomeutility_v2.dialogs.generic.DropdownDialog;
import com.krisapps.incomeutility_v2.dialogs.generic.LoadingDialog;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityType;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.cashew.CashewTransaction;
import com.krisapps.incomeutility_v2.ui.listview.AccountComboboxCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.cell.AccountComboboxButtonCell;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.PopupManager;
import com.krisapps.incomeutility_v2.util.UtilityManager;
import com.krisapps.incomeutility_v2.util.services.TransactionService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class IncomeUtilityController {

    public final static ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(4);
    public final UtilityManager utilities = UtilityManager.create();
    public final DataManager data = DataManager.getInstance();
    @FXML
    private VBox root;

    @FXML
    private GridPane utilitiesView;

    @FXML
    private VBox mimoCell;

    @FXML
    private VBox pricerCell;

    @FXML
    private VBox breakdownCell;

    @FXML
    private VBox pantryCell;

    @FXML
    private VBox subscriptionsCell;

    @FXML
    private VBox settingsCell;

    @FXML
    private GridPane accountView;
    private int nextRow = 0;
    private int nextColumn = 0;

    @FXML
    public void initialize() {
        IncomeUtilityApplication.updateTitle("Starting application...", true);
        data.initialize();

        registerHandlers();
        refreshAccountView();

        IncomeUtilityApplication.updateTitle("Dashboard", false);
    }

    public static <V> Future<?> submitAsynchronousTask(Task<V> task) {
        return scheduler.submit(task);
    }

    public void registerHandlers() {
        mimoCell.setOnMouseClicked((e) -> {
            if (!e.isShiftDown()) {
                utilities.openUtility(SubUtilityType.MONEY_IN_MONEY_OUT);
            } else {
                utilities.focusAll(SubUtilityType.MONEY_IN_MONEY_OUT);
            }
        });

        pricerCell.setOnMouseClicked((e) -> {
            if (!e.isShiftDown()) {
                utilities.openUtility(SubUtilityType.PRICER);
            } else {
                utilities.focusAll(SubUtilityType.PRICER);
            }
        });

        breakdownCell.setOnMouseClicked((e) -> {
            if (!e.isShiftDown()) {
                utilities.openUtility(SubUtilityType.BREAKDOWN);
            } else {
                utilities.focusAll(SubUtilityType.BREAKDOWN);
            }
        });

        pantryCell.setOnMouseClicked((e) -> {
            if (!e.isShiftDown()) {
                utilities.openUtility(SubUtilityType.PANTRY);
            } else {
                utilities.focusAll(SubUtilityType.PANTRY);
            }
        });

        settingsCell.setOnMouseClicked((e) -> {
            if (!e.isShiftDown()) {
                utilities.openUtility(SubUtilityType.SETTINGS);
            } else {
                utilities.focusAll(SubUtilityType.SETTINGS);
            }
        });
    }

    public void refreshAccountView() {
        HashSet<Account> accounts = data.getAccounts();
        accountView.getChildren().clear();
        nextRow = 0;
        nextColumn = 0;

        accounts.stream().sorted(Comparator.comparing(Account::getName)).forEach(account -> {
            VBox container = new VBox();
            container.setFillWidth(true);
            container.setAlignment(Pos.CENTER);
            container.setMaxWidth(Double.MAX_VALUE);
            container.setMaxHeight(Double.MAX_VALUE);

            HBox.setHgrow(container, Priority.ALWAYS);
            VBox.setVgrow(container, Priority.ALWAYS);

            Label name = new Label(account.getName());
            name.getStyleClass().add("header");
            name.setAlignment(Pos.CENTER);
            name.setMaxWidth(Double.MAX_VALUE);
            name.setMaxHeight(Double.MAX_VALUE);

            HBox.setHgrow(name, Priority.ALWAYS);
            VBox.setVgrow(name, Priority.ALWAYS);

            container.getChildren().add(name);
            container.getStyleClass().add("account-cell");
            container.setPadding(new Insets(20, 20, 20, 20));
            container.setOnMouseClicked((event) -> {
                AccountInfoDialog dialog = new AccountInfoDialog(account);
                dialog.showAndWait();
                refreshAccountView();
            });

            addAccountNode(container);
        });

    }

    public void promptImportTransactions() {
        Optional<UUID> lastActiveId = data.getLastActiveAccount();
        Optional<Account> account = Optional.empty();

        if (lastActiveId.isPresent() && data.accountExists(lastActiveId.get())) {
            account = data.getAccount(lastActiveId.get());
        } else {

            if (data.getAccounts().isEmpty()) {
                PopupManager.showPopup("No accounts exist", "You need to create an account to import transactions.", Alert.AlertType.WARNING);
                return;
            }

            DropdownDialog<Account> accountPicker = new DropdownDialog<>("Select account");
            accountPicker.setPrimaryLabel("Which account would you like to import transactions to?");
            accountPicker.setDescription("To proceed with importing, you need to pick an account.");
            accountPicker.setCellFactory(new AccountComboboxCellFactory(), new AccountComboboxButtonCell());
            accountPicker.setItems(data.getAccounts().stream().toList());

            account = accountPicker.showAndWait();
        }

        if (account.isEmpty()) {
            PopupManager.showPopup("No account selected", "You need to select an account to import transactions.", Alert.AlertType.WARNING);
            return;
        }

        ImportFromCashewDialog dialog = new ImportFromCashewDialog(account.get());
        Optional<Pair<Account, ArrayList<CashewTransaction>>> imported = dialog.showAndWait();

        LoadingDialog pushDialog = new LoadingDialog(LoadingDialog.LoadingOperationType.INDETERMINATE_PROGRESSBAR);
        pushDialog.setPrimaryLabel("Hold on");
        pushDialog.setSecondaryLabel("Saving imported transactions");
        pushDialog.show("Just a second", () -> {
            imported.ifPresent(accountArrayListPair -> TransactionService.getInstance().pushTransactionsTo(accountArrayListPair.getKey(), accountArrayListPair.getValue()));
        });
    }

    private void addAccountNode(Node node) {
        if (nextRow >= accountView.getRowCount()) {
            accountView.addRow(nextRow, node);
            return;
        }
        if (nextColumn >= accountView.getColumnCount()) {
            nextRow++;
            nextColumn = 1;
            addAccountNode(node);
            return;
        }
        accountView.add(node, nextColumn++, nextRow);
    }

    public void promptCreateNewAccount() {
        AddAccountWizard wizard = new AddAccountWizard();
        Optional<Account> account = wizard.showAndWait();

        account.ifPresent(a -> {
            data.addAccount(a);
            refreshAccountView();
        });
    }
}
