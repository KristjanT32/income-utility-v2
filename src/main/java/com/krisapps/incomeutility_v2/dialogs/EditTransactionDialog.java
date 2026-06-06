package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.transaction.TransactionCategory;
import com.krisapps.incomeutility_v2.types.transaction.TransactionType;
import com.krisapps.incomeutility_v2.ui.listview.AccountComboboxCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.cell.AccountComboboxButtonCell;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.Formatting;
import com.krisapps.incomeutility_v2.util.misc.Formats;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.UnaryOperator;

public class EditTransactionDialog extends IncomeUtilityDialog<Transaction> {

    private final DataManager data = DataManager.getInstance();
    private final Transaction outputTransaction;
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
    private VBox singleTargetTransactionPanel;
    @FXML
    private VBox dualTargetTransactionPanel;
    @FXML
    private ComboBox<TransactionType> transactionTypeSelector;
    @FXML
    private ComboBox<String> categorySelector;
    @FXML
    private ComboBox<Account> singleTargetSelector;
    @FXML
    private ComboBox<Account> fromSelector;
    @FXML
    private ComboBox<Account> toSelector;
    @FXML
    private DatePicker dateSelector;
    @FXML
    private TextField timeField;
    @FXML
    private TextField amountField;
    @FXML
    private TextArea commentField;
    @FXML
    private TextField customCategoryField;
    @FXML
    private Label singleTargetLabel;
    private final Account selectedAccount;

    public EditTransactionDialog(Transaction t, Account selectedAccount) {
        super("edit-transaction.fxml", "Edit transaction", "edit_96.png");
        this.outputTransaction = t.copy();
        this.selectedAccount = selectedAccount;

        ButtonType createButton = new ButtonType("Apply changes", ButtonBar.ButtonData.APPLY);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().setAll(createButton, cancelButton);

        amountField.setTextFormatter(new TextFormatter<>(numbersOnlyFormatter));
        timeField.textProperty().addListener((obs, _, val) -> {
            timeField.setStyle("-fx-text-fill: " + (timeField.getText().matches("(?:0[0-9]|1[0-9]|2[0-3])(?::[0-5][0-9])*") ? "black" : "red"));
            try {
                outputTransaction.setTime(LocalTime.parse(val, DateTimeFormatter.ofPattern("HH:mm:ss")));
            } catch (DateTimeParseException _) {

            }
        });

        dateSelector.valueProperty().addListener((obs, _, newVal) -> {
            outputTransaction.setDate(newVal);
        });

        dateSelector.setConverter(Formats.DATE_FORMAT);

        ObservableList<TransactionType> items = transactionTypeSelector.getItems();
        items.setAll(TransactionType.values());
        transactionTypeSelector.setItems(items);

        refreshCategorySelector();

        transactionTypeSelector.setValue(outputTransaction.getType());
        categorySelector.setValue(Formatting.capitalize(outputTransaction.getCategory().name()));
        customCategoryField.setText(outputTransaction.getCustomCategory());
        dateSelector.setValue(outputTransaction.getTimestamp().toLocalDate());
        timeField.setText(outputTransaction.getTimestamp().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        commentField.setText(outputTransaction.getComment());
        amountField.setText(Formatting.formatMoney(outputTransaction.getAbsoluteAmount()));

        HashSet<Account> accounts = data.getAccounts();
        switch (outputTransaction.getType()) {
            case DEPOSIT, WITHDRAWAL -> {
                refreshSingleAccountSelector(accounts, selectedAccount);
                singleTargetSelector.setValue(data.getAccount(outputTransaction.getTargetAccountId()).get());
            }
            case TRANSFER -> {
                refreshSourceAccountSelector(accounts, selectedAccount);
                refreshTargetAccountSelector(accounts);
                fromSelector.setValue(data.getAccount(outputTransaction.getSourceAccountId()).get());
                toSelector.setValue(data.getAccount(outputTransaction.getTargetAccountId()).get());
            }
        }

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
        categorySelector.setConverter(new StringConverter<String>() {
            @Override
            public String toString(String s) {
                return Formatting.humanize(s);
            }

            @Override
            public String fromString(String s) {
                return "";
            }
        });
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
        categorySelector.getItems().clear();
        categorySelector.getItems().addAll(Arrays.stream(TransactionCategory.values()).map(Enum::name).toList());
        categorySelector.getItems().addAll(data.getCustomTransactionCategories());
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
        });

        transactionTypeSelector.valueProperty().addListener((obs, _, newVal) -> {
            outputTransaction.setType(newVal);
            if (newVal == TransactionType.TRANSFER) {
                singleTargetTransactionPanel.setVisible(false);
                singleTargetTransactionPanel.setManaged(false);

                dualTargetTransactionPanel.setVisible(true);
                dualTargetTransactionPanel.setManaged(true);
            } else {
                if (transactionTypeSelector.getValue() == TransactionType.WITHDRAWAL) {
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
            if (newVal.trim().isBlank()) return;
            try {
                outputTransaction.setCategory(TransactionCategory.valueOf(newVal.toUpperCase().replace(' ', '_')));

                if (outputTransaction.getCategory().equals(TransactionCategory.CUSTOM)) {
                    customCategoryField.setVisible(true);
                    customCategoryField.setManaged(true);
                } else {
                    customCategoryField.setVisible(false);
                    customCategoryField.setManaged(false);
                }

            } catch (IllegalArgumentException e) {
                categorySelector.setValue(TransactionCategory.CUSTOM.name());
                outputTransaction.setCategory(TransactionCategory.CUSTOM);
                outputTransaction.setCustomCategory(newVal);
                customCategoryField.setText(newVal);
                customCategoryField.setVisible(true);
                customCategoryField.setManaged(true);
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
