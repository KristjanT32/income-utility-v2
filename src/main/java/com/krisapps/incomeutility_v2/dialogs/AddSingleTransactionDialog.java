package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.organization.TransactionCategory;
import com.krisapps.incomeutility_v2.ui.listview.AccountComboboxCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.cell.AccountComboboxButtonCell;
import com.krisapps.incomeutility_v2.ui.listview.cell.AccountComboboxCell;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.PopupManager;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.UnaryOperator;

public class AddSingleTransactionDialog extends Dialog<Transaction> {

    @FXML
    private VBox rootPane;

    @FXML
    private VBox singleTargetTransactionPanel;

    @FXML
    private VBox dualTargetTransactionPanel;

    @FXML
    private ComboBox<Transaction.Type> transactionTypeSelector;

    @FXML
    private ComboBox<String> categorySelector;

    @FXML
    private ComboBox<Account> singleTargetSelector;

    @FXML
    private ComboBox<Account> fromSelector;

    @FXML
    private ComboBox<Account> toSelector;

    @FXML
    private TextField amountField;

    @FXML
    private TextArea commentField;

    @FXML
    private TextField customCategoryField;

    @FXML
    private Label singleTargetLabel;


    private final DataManager data = DataManager.getInstance();
    private final Transaction outputTransaction = new Transaction(null, 0.0d, null, null, null, null, null);
    private Account selectedAccount;

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

    public AddSingleTransactionDialog(Account selectedAccount) {
        try {
            FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/dialogs/add-transaction.fxml"));
            loader.setController(this);
            rootPane = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ButtonType createButton = new ButtonType("Add", ButtonBar.ButtonData.APPLY);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        getDialogPane().getButtonTypes().setAll(createButton, cancelButton);

        getDialogPane().setContent(rootPane);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Register transaction");

        this.selectedAccount = selectedAccount;

        amountField.setTextFormatter(new TextFormatter<>(numbersOnlyFormatter));

        ObservableList<Transaction.Type> items = transactionTypeSelector.getItems();
        items.setAll(Transaction.Type.values());
        transactionTypeSelector.setItems(items);

        HashSet<Account> accounts = data.getAccounts();

        refreshSingleAccountSelector(accounts, selectedAccount);
        refreshSourceAccountSelector(accounts, selectedAccount);
        refreshTargetAccountSelector(accounts);
        refreshCategorySelector();

        transactionTypeSelector.setValue(Transaction.Type.WITHDRAWAL);
        categorySelector.setValue(TransactionCategory.WITHDRAWAL.name());

        outputTransaction.setType(Transaction.Type.WITHDRAWAL);
        outputTransaction.setCategory(TransactionCategory.WITHDRAWAL);

        setResultConverter((action) -> {
            if (action.getButtonData() == ButtonBar.ButtonData.APPLY) {
                return outputTransaction;
            } else {
                return null;
            }
        });
    }

    @FXML
    public void initialize() {
        registerEventHandlers();
        refreshUI();
    }

    private void refreshUI() {

    }

    private void refreshSingleAccountSelector(HashSet<Account> accounts, Account selectedAccount) {
        ObservableList<Account> singleTargetItems = singleTargetSelector.getItems();
        singleTargetItems.setAll(accounts);
        singleTargetSelector.setItems(singleTargetItems);
        singleTargetSelector.setCellFactory(new AccountComboboxCellFactory());
        singleTargetSelector.setButtonCell(new AccountComboboxButtonCell());
        singleTargetSelector.setValue(selectedAccount);
    }

    private void refreshSourceAccountSelector(HashSet<Account> accounts, Account selectedAccount) {
        ObservableList<Account> items = fromSelector.getItems();
        items.clear();
        items.setAll(accounts.stream().filter(a -> !a.equals(toSelector.getValue())).toList());
        fromSelector.setItems(items);
        fromSelector.setCellFactory(new AccountComboboxCellFactory());
        fromSelector.setButtonCell(new AccountComboboxButtonCell());
        fromSelector.setValue(selectedAccount);
    }

    private void refreshTargetAccountSelector(HashSet<Account> accounts) {
        ObservableList<Account> items = toSelector.getItems();
        items.clear();
        items.setAll(accounts.stream().filter(a -> !a.equals(fromSelector.getValue())).toList());
        toSelector.setItems(items);
        toSelector.setCellFactory(new AccountComboboxCellFactory());
        toSelector.setButtonCell(new AccountComboboxButtonCell());
    }

    private void refreshCategorySelector() {
        ObservableList<String> categories = categorySelector.getItems();
        categories.setAll(Arrays.stream(TransactionCategory.values()).map(category -> DataManager.Formatting.capitalize(category.name().replace('_', ' '))).toList());
        categories.addAll(data.getCustomTransactionCategories());
        categorySelector.setItems(categories);
    }

    private void registerEventHandlers() {
        singleTargetSelector.valueProperty().addListener((_, _, newValue) -> {
            outputTransaction.setTargetAccountId(newValue.getId());
        });

        fromSelector.valueProperty().addListener((_, prevValue, newValue) -> {
            outputTransaction.setSourceAccountId(newValue != null ? newValue.getId() : null);
            refreshTargetAccountSelector(data.getAccounts());
        });

        toSelector.valueProperty().addListener((_, prevValue, newValue) -> {
            outputTransaction.setTargetAccountId(newValue != null ? newValue.getId() : null);
            if (fromSelector.getValue().equals(newValue)) {
                PopupManager.showConfirmation("Convert to withdrawal?",
                        "You have set the target of the transaction to the same account as the source.\n\nWould you like to convert this transaction to a withdrawal?",
                        new ButtonType("Convert to withdrawal", ButtonBar.ButtonData.APPLY),
                        new ButtonType("No, leave as is", ButtonBar.ButtonData.CANCEL_CLOSE)
                ).ifPresent(response -> {
                    if (response.getButtonData() == ButtonBar.ButtonData.APPLY) {
                        singleTargetSelector.setValue(fromSelector.getValue());
                        transactionTypeSelector.setValue(Transaction.Type.WITHDRAWAL);
                    }
                });
            }
        });

        transactionTypeSelector.valueProperty().addListener((obs, _, newVal) -> {
            outputTransaction.setType(newVal);
            if (newVal == Transaction.Type.TRANSFER) {
                singleTargetTransactionPanel.setVisible(false);
                singleTargetTransactionPanel.setManaged(false);

                dualTargetTransactionPanel.setVisible(true);
                dualTargetTransactionPanel.setManaged(true);
            } else {
                if (transactionTypeSelector.getValue() == Transaction.Type.WITHDRAWAL) {
                    singleTargetLabel.setText("From");
                } else {
                    singleTargetLabel.setText("To");
                }

                singleTargetTransactionPanel.setVisible(true);
                singleTargetTransactionPanel.setManaged(true);

                dualTargetTransactionPanel.setVisible(false);
                dualTargetTransactionPanel.setManaged(false);
            }
        });

        categorySelector.valueProperty().addListener((o, oldVal, newVal) -> {
            outputTransaction.setCategory(TransactionCategory.valueOf(newVal.toUpperCase().replace(' ', '_')));
            if (TransactionCategory.valueOf(newVal.toUpperCase().replace(' ', '_')) == TransactionCategory.CUSTOM) {
                customCategoryField.setVisible(true);
                customCategoryField.setManaged(true);
            } else {
                customCategoryField.setVisible(false);
                customCategoryField.setManaged(false);
            }
            getDialogPane().requestLayout();
            getDialogPane().getScene().getWindow().sizeToScene();
        });

        amountField.textProperty().addListener((_, _, newValue) -> {
            if (newValue == null) {
                outputTransaction.setAmount(0.0d);
            } else {
                outputTransaction.setAmount(Double.parseDouble(newValue));
            }
        });

        commentField.textProperty().addListener((_, _, newValue) -> {
            if (newValue == null) {
                outputTransaction.setComment("");
            } else {
                outputTransaction.setComment(newValue);
            }
        });

        customCategoryField.textProperty().addListener((_, _, newValue) -> {
            if (newValue == null) {
                outputTransaction.setCustomCategory("");
            } else {
                outputTransaction.setCustomCategory(newValue);
            }
        });
    }
}
