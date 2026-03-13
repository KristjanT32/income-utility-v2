package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.types.fiscal.Account;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.function.UnaryOperator;

public class EditAccountWizard extends IncomeUtilityDialog<Account> {

    private Account outputAccount = null;
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

    public EditAccountWizard(Account account) {
        super("edit-account.fxml", "Edit account details", "account_96.png");
        this.outputAccount = account;

        ButtonType createButton = new ButtonType("Apply", ButtonBar.ButtonData.APPLY);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().setAll(createButton, cancelButton);

        ObservableList<Account.Type> items = typeSelector.getItems();
        items.setAll(Account.Type.values());
        typeSelector.setItems(items);
        typeSelector.setValue(outputAccount.getType());
        typeSelector.valueProperty().addListener((o, oldVal, newVal) -> {
            outputAccount.setType(newVal);
        });

        nameField.setText(account.getName());
        nameField.textProperty().addListener((o, oldVal, newVal) -> {
            outputAccount.setName(nameField.getText());
        });

        balanceField.setTextFormatter(new TextFormatter<>(numbersOnlyFormatter));
        balanceField.setText(String.valueOf(account.getInitialBalance()));
        balanceField.textProperty().addListener((o, oldVal, newVal) -> {
            outputAccount.setInitialBalance(Double.parseDouble(balanceField.getText()));
        });

        setResultConverter((action) -> {
            if (action.getButtonData() == ButtonBar.ButtonData.APPLY) {
                return outputAccount;
            } else {
                return null;
            }
        });
    }
}
