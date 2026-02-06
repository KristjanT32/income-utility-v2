package com.krisapps.incomeutility_v2;

import com.krisapps.incomeutility_v2.dialogs.AddAccountWizard;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityType;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.util.DataManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class IncomeUtilityController {

    public final UtilityManager utilities = UtilityManager.create();
    public final DataManager data = DataManager.getInstance();
    public final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(4);

    @FXML
    private VBox root;

    @FXML
    private TilePane utilitiesView;

    @FXML
    private TilePane accountView;

    @FXML
    private VBox mimoCell;

    @FXML
    private VBox pricerCell;

    @FXML
    private VBox subscriptionsCell;

    @FXML
    public void initialize() {
        IncomeUtilityApplication.updateTitle("Starting application...", true);

        registerHandlers();
        loadAccounts();

        IncomeUtilityApplication.updateTitle("Dashboard", false);
    }

    public void registerHandlers() {
        mimoCell.setOnMouseClicked((e) -> {
            if (!e.isShiftDown()) {
                utilities.openUtility(SubUtilityType.MONEY_IN_MONEY_OUT);
            } else {
                utilities.focusAll(SubUtilityType.MONEY_IN_MONEY_OUT);
            }
        });
    }

    public void loadAccounts() {
        HashSet<Account> accounts = data.getAccounts();

        accounts.stream().sorted(Comparator.comparingDouble(Account::getBalance).reversed()).forEach(account -> {
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

            HBox.setHgrow(name, Priority.ALWAYS);

            container.getChildren().add(name);
            container.getStyleClass().add("account-cell");
            container.setPadding(new Insets(10, 10, 10, 10));

            accountView.getChildren().add(container);
        });
    }

    public void promptCreateNewAccount() {
        AddAccountWizard wizard = new AddAccountWizard();
        Optional<Account> account = wizard.showAndWait();

        account.ifPresent(data::addAccount);
    }
}
