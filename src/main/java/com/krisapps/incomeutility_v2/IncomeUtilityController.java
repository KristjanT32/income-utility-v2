package com.krisapps.incomeutility_v2;

import com.krisapps.incomeutility_v2.dialogs.AccountInfoDialog;
import com.krisapps.incomeutility_v2.dialogs.AddAccountWizard;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityType;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.UtilityManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.*;

public class IncomeUtilityController {

    public final static ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(4);
    public final UtilityManager utilities = UtilityManager.create();
    public final DataManager data = DataManager.getInstance();
    @FXML
    private VBox root;

    @FXML
    private TilePane utilitiesView;

    @FXML
    private VBox mimoCell;

    @FXML
    private VBox pricerCell;

    @FXML
    private VBox subscriptionsCell;

    @FXML
    private GridPane accountView;
    private int nextRow = 0;
    private int nextColumn = 0;

    @FXML
    public void initialize() {
        IncomeUtilityApplication.updateTitle("Starting application...", true);

        registerHandlers();
        refreshAccountView();
        data.initialize();

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

    private void addAccountNode(Node node) {
        if (nextRow >= accountView.getRowCount()) {
            accountView.addRow(nextRow, node);
            return;
        }
        if (nextColumn >= accountView.getColumnCount()) {
            nextRow++;
            nextColumn = 0;
            addAccountNode(node);
            return;
        }
        accountView.add(node, nextColumn++, nextRow);
    }

    public void promptCreateNewAccount() {
        AddAccountWizard wizard = new AddAccountWizard();
        Optional<Account> account = wizard.showAndWait();

        account.ifPresent(data::addAccount);
    }
}
