package com.krisapps.incomeutility_v2.ui.listview.cell;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.fiscal.cashew.CashewTransaction;
import com.krisapps.incomeutility_v2.types.transaction.TransactionCategory;
import com.krisapps.incomeutility_v2.types.transaction.TransactionType;
import com.krisapps.incomeutility_v2.ui.listview.AccountComboboxCellFactory;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.services.FiscalService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.function.Consumer;

public class EditableCashewTransactionCell extends ListCell<CashewTransaction> {

    private final Account parent;
    private final Consumer<CashewTransaction> onItemDataChange;
    private final Consumer<CashewTransaction> onItemDeleted;

    private boolean updating = false;

    @FXML
    private final HBox rootPane;

    @FXML
    private ComboBox<TransactionType> typeSelector;

    @FXML
    private ComboBox<String> categorySelector;

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
    private TextField commentField;

    @FXML
    private Button deleteButton;

    @FXML
    private FontIcon arrowIcon;

    public EditableCashewTransactionCell(Account parent, Consumer<CashewTransaction> onItemDataChange, Consumer<CashewTransaction> onItemDeleted) {
        try {
            FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/ui/transaction_cell_review.fxml"));
            loader.setController(this);
            rootPane = loader.load();
            this.parent = parent;
            this.onItemDataChange = onItemDataChange;
            this.onItemDeleted = onItemDeleted;

            setupCell();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupCell() {

        setOnKeyPressed((event) -> {
            if (!isSelected()) return;
            if (event.getCode().equals(KeyCode.DELETE)) {
                onItemDeleted.accept(getItem());
            }
        });

        typeSelector.getItems().setAll(TransactionType.values());
        typeSelector.setConverter(new StringConverter<TransactionType>() {
            @Override
            public String toString(TransactionType transactionType) {
                return transactionType.getDisplayName();
            }

            @Override
            public TransactionType fromString(String s) {
                return TransactionType.valueOf(s.toUpperCase());
            }
        });

        categorySelector.setConverter(new StringConverter<String>() {
            @Override
            public String toString(String s) {
                return DataManager.Formatting.humanize(s);
            }

            @Override
            public String fromString(String s) {
                return "";
            }
        });

        // Ensure the arrow icon is only visible if both account selectors are visible
        arrowIcon.visibleProperty().bind(fromSelector.visibleProperty().and(toSelector.visibleProperty()));
        arrowIcon.managedProperty().bind(arrowIcon.visibleProperty());

        typeSelector.valueProperty().addListener((obs, old, val) -> {
            getItem().setType(val);
            refreshType(getItem());
        });

        fromSelector.setButtonCell(new AccountComboboxButtonCell());
        toSelector.setButtonCell(new AccountComboboxButtonCell());

        fromSelector.setCellFactory(new AccountComboboxCellFactory());
        toSelector.setCellFactory(new AccountComboboxCellFactory());

        fromSelector.getItems().setAll(FiscalService.getInstance().getAccounts());
        toSelector.getItems().setAll(FiscalService.getInstance().getAccounts());

        categorySelector.getItems().clear();
        categorySelector.getItems().addAll(Arrays.stream(TransactionCategory.values()).map(Enum::name).toList());
        categorySelector.getItems().addAll(DataManager.getInstance().getCustomTransactionCategories());

        deleteButton.setOnAction((ev) -> {
            if (getItem() != null) {
                onItemDeleted.accept(getItem());
            }
        });

        amountField.textProperty().addListener((obs, old, val) -> {
            if (updating || getItem() == null) return;
            try {
                double amount = Double.parseDouble(val);
                getItem().setAmount(amount);

                if (typeSelector.getValue() != TransactionType.TRANSFER) {
                    if (amount < 0) {
                        typeSelector.setValue(TransactionType.WITHDRAWAL);
                    } else {
                        typeSelector.setValue(TransactionType.DEPOSIT);
                    }
                }
            } catch (NumberFormatException _) {

            }
        });

        fromSelector.valueProperty().addListener((obs, old, val) -> {
            if (updating || getItem() == null || val == null) return;
            getItem().setSourceAccountId(val.getId());
        });

        toSelector.valueProperty().addListener((obs, old, val) -> {
            if (updating || getItem() == null || val == null) return;
            getItem().setTargetAccountId(val.getId());
        });

        categorySelector.valueProperty().addListener((obs, old, val) -> {
            if (updating || getItem() == null) return;
            try {
                getItem().setCategory(TransactionCategory.valueOf(val));
            } catch (IllegalArgumentException e) {
                getItem().setCategory(TransactionCategory.CUSTOM);
                getItem().setCustomCategory(val);
            }
        });

        commentField.textProperty().addListener((obs, old, val) -> {
            if (updating || getItem() == null) return;
            getItem().setComment(val);
        });

        dateSelector.valueProperty().addListener((obs, old, val) -> {
            if (updating || getItem() == null) return;
            if (val != null) {
                getItem().setDate(val);
            }
        });

        timeField.textProperty().addListener((obs, old, val) -> {
            if (updating || getItem() == null) return;
            try {
                getItem().setTime(LocalTime.parse(val, DateTimeFormatter.ofPattern("HH:mm:ss")));

                
            } catch (DateTimeParseException _) {

            }
        });
    }

    @Override
    protected void updateItem(CashewTransaction transaction, boolean empty) {
        super.updateItem(transaction, empty);

        if (empty || transaction == null) {
            setText(null);
            setGraphic(null);
            return;
        } else {
            setText(null);
            setGraphic(rootPane);
        }

        updating = true;
        try {
            amountField.setText(DataManager.Formatting.formatMoney(
                    transaction.getAbsoluteAmount(),
                    parent.getCurrencyConfig()
            ));

            typeSelector.setValue(transaction.getType());
            categorySelector.setValue(transaction.getCategory().name());

            refreshType(transaction);

            commentField.setText(transaction.getComment());
            dateSelector.setValue(transaction.getTimestamp().toLocalDate());
            timeField.setText(DataManager.Formatting.formatLocalTime(transaction.getTimestamp().toLocalTime()));
        } finally {
            updating = false;
        }
    }

    private void refreshType(Transaction t) {
        if (t.getType().equals(TransactionType.TRANSFER)) {
            fromSelector.setVisible(true);
            fromSelector.setManaged(true);
            toSelector.setVisible(true);
            toSelector.setManaged(true);

            fromSelector.setValue(FiscalService.getInstance().getAccountById(t.getSourceAccountId()).orElse(null));
            toSelector.setValue(FiscalService.getInstance().getAccountById(t.getTargetAccountId()).orElse(null));
        } else {
            fromSelector.setVisible(false);
            fromSelector.setManaged(false);
            toSelector.setVisible(true);
            toSelector.setManaged(true);

            toSelector.setValue(FiscalService.getInstance().getAccountById(t.getTargetAccountId()).orElse(null));
        }
        requestLayout();
    }
}

