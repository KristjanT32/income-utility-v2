package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import java.io.IOException;
import java.util.function.UnaryOperator;

public class AddAccountWizard extends Dialog<Account> {

    @FXML
    private VBox rootPane;

    @FXML
    private TextField nameField;

    @FXML
    private TextField balanceField;

    @FXML
    private ComboBox<Account.Type> typeSelector;

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

    public AddAccountWizard() {
        try {
            FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/dialogs/add-account.fxml"));
            loader.setController(this);
            rootPane = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ButtonType createButton = new ButtonType("Create", ButtonBar.ButtonData.APPLY);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        getDialogPane().getButtonTypes().setAll(createButton, cancelButton);

        getDialogPane().setContent(rootPane);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Create new account");

        ObservableList<Account.Type> items = typeSelector.getItems();
        items.setAll(Account.Type.values());
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

        setResultConverter((action) -> {
            if (action.getButtonData() == ButtonBar.ButtonData.APPLY) {
                return outputAccount;
            } else {
                return null;
            }
        });
    }
}
