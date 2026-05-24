package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.types.fiscal.Account;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.function.UnaryOperator;

public class AddAccountWizard extends IncomeUtilityDialog<Account> {

    private final Account outputAccount = new Account();
    private final UnaryOperator<TextFormatter.Change> numbersOnlyFormatter = (change) -> {
        if (change.getControlNewText().isEmpty()) {
            return change;
        }

        try {
            Double.parseDouble(change.getControlNewText());
            return change;
        } catch (NumberFormatException ignored) {
        }

        return null;
    };
    @FXML
    private VBox rootPane;
    @FXML
    private TextField nameField;
    @FXML
    private TextField balanceField;
    @FXML
    private ComboBox<Account.Type> typeSelector;

    public AddAccountWizard() {
        super("add-account.fxml", "Create new account", "account_96.png");

        ButtonType createButton = new ButtonType("Create", ButtonBar.ButtonData.APPLY);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().setAll(createButton, cancelButton);

        ObservableList<Account.Type> items = typeSelector.getItems();
        items.setAll(Account.Type.values());
        typeSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(Account.Type object) {
                return object.getDisplayName();
            }

            @Override
            public Account.Type fromString(String string) {
                return null;
            }
        });
        typeSelector.setItems(items);
        typeSelector.valueProperty().addListener((o, oldVal, newVal) -> {
            outputAccount.setType(newVal);
        });

        nameField.textProperty().addListener((o, oldVal, newVal) -> {
            outputAccount.setName(nameField.getText());
        });

        balanceField.setTextFormatter(new TextFormatter<>(numbersOnlyFormatter));
        balanceField.textProperty().addListener((o, oldVal, newVal) -> {
            outputAccount.setInitialBalance(Double.parseDouble(balanceField.getText()));
        });

        typeSelector.getSelectionModel().select(Account.Type.BANK_ACCOUNT);

        setResultConverter((action) -> {
            if (action.getButtonData() == ButtonBar.ButtonData.APPLY) {
                return outputAccount;
            } else {
                return null;
            }
        });
    }
}
