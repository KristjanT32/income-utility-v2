package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.transaction.TransactionCategory;
import com.krisapps.incomeutility_v2.types.transaction.TransactionType;
import com.krisapps.incomeutility_v2.ui.listview.AccountComboboxCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.cell.AccountComboboxButtonCell;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.Formatting;
import com.krisapps.incomeutility_v2.util.PopupManager;
import com.krisapps.incomeutility_v2.util.misc.Formats;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.UnaryOperator;

public class AddSingleTransactionDialog extends IncomeUtilityDialog<Transaction> {

    private final DataManager data = DataManager.getInstance();
    private final Transaction outputTransaction = new Transaction(null, 0.0d, null, null, null, null, null, null);
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

    public AddSingleTransactionDialog(Account selectedAccount) {
        super("add-transaction.fxml", "Register transaction");
        this.selectedAccount = selectedAccount;

        ButtonType createButton = new ButtonType("Add", ButtonBar.ButtonData.APPLY);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().setAll(createButton, cancelButton);

        amountField.setTextFormatter(new TextFormatter<>(numbersOnlyFormatter));
        timeField.textProperty().addListener((obs, _, val) -> {
            timeField.setStyle("-fx-text-fill: " + (timeField.getText().matches("(?:0[0-9]|1[0-9]|2[0-3])(?::[0-5][0-9])*") ? "black" : "red"));
            outputTransaction.setTimestamp(assembleDate(dateSelector.getValue(), timeField.getText()));
        });

        dateSelector.valueProperty().addListener((obs, _, newVal) -> {
            outputTransaction.setTimestamp(assembleDate(dateSelector.getValue(), timeField.getText()));
        });

        dateSelector.setConverter(Formats.DATE_FORMAT);

        ObservableList<TransactionType> items = transactionTypeSelector.getItems();
        items.setAll(TransactionType.values());
        transactionTypeSelector.setItems(items);

        HashSet<Account> accounts = data.getAccounts();

        refreshSingleAccountSelector(accounts, selectedAccount);
        refreshSourceAccountSelector(accounts, selectedAccount);
        refreshTargetAccountSelector(accounts);
        refreshCategorySelector();

        transactionTypeSelector.setValue(TransactionType.WITHDRAWAL);
        categorySelector.setValue(TransactionCategory.WITHDRAWAL.name());
        dateSelector.setValue(LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault()));
        timeField.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        outputTransaction.setType(TransactionType.WITHDRAWAL);
        outputTransaction.setCategory(TransactionCategory.WITHDRAWAL);

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
    }

    private LocalDateTime assembleDate(LocalDate date, String time) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalTime corrected = null;


        if (time.length() == 2) {
            // "12"
            corrected = LocalTime.of(Integer.parseInt(time), 0, 0);
        } else if (time.length() == 5) {
            // 12:00
            int hour = Integer.parseInt(time.split(":")[0]);
            int minute = Integer.parseInt(time.split(":")[1]);
            corrected = LocalTime.of(hour, minute, 0);
        } else if (time.length() == 8) {
            // 12:00:00
            String[] split = time.split(":");
            int hour = Integer.parseInt(split[0]);
            int min = Integer.parseInt(split[1]);
            int sec = Integer.parseInt(split[2]);
            corrected = LocalTime.of(hour, min, sec);
        }

        return LocalDateTime.of(date != null ? date : LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault()), corrected != null ? corrected : LocalTime.now());
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
        categorySelector.getItems().clear();
        categorySelector.getItems().addAll(Arrays.stream(TransactionCategory.values()).map(Enum::name).toList());
        categorySelector.getItems().addAll(DataManager.getInstance().getCustomTransactionCategories());
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
                        transactionTypeSelector.setValue(TransactionType.WITHDRAWAL);
                    }
                });
            }
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
