package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.dialogs.generic.LoadingDialog;
import com.krisapps.incomeutility_v2.types.DateFilteringMode;
import com.krisapps.incomeutility_v2.types.SearchMode;
import com.krisapps.incomeutility_v2.types.TransactionTypeFilter;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.transaction.TransactionCategory;
import com.krisapps.incomeutility_v2.ui.listview.TransactionCellFactory;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.Formatting;
import com.krisapps.incomeutility_v2.util.PopupManager;
import com.krisapps.incomeutility_v2.util.misc.Formats;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TransactionBrowserDialog extends IncomeUtilityDialog<Void> {

    @FXML
    private Button searchButton;

    @FXML
    private Button resetFiltersButton;

    @FXML
    private ComboBox<DateFilteringMode> dateFilterSelector;

    @FXML
    private ComboBox<TransactionCategory> baseCategoryFilter;

    @FXML
    private ComboBox<String> exactCustomCategoryFilter;

    @FXML
    private ComboBox<TransactionTypeFilter> typeFilter;

    @FXML
    private ComboBox<SearchMode> searchModeSelector;

    @FXML
    private ComboBox<Account> accountFilter;

    @FXML
    private TextField commentFilter;

    @FXML
    private TextField customCategoryFilter;

    @FXML
    private DatePicker dateFilter1;

    @FXML
    private TextField timeFilter1;

    @FXML
    private DatePicker dateFilter2;

    @FXML
    private TextField timeFilter2;

    @FXML
    private FontIcon arrowIcon;

    @FXML
    private Label searchInfoLabel;

    @FXML
    private Label customCategoryLabel;

    @FXML
    private Label commentLabel;

    @FXML
    private ListView<Transaction> resultList;

    private static final DataManager data = DataManager.getInstance();

    SimpleBooleanProperty customCategoryExactMatchEnabled = new SimpleBooleanProperty(false);
    SimpleBooleanProperty commentExactMatchEnabled = new SimpleBooleanProperty(false);

    public TransactionBrowserDialog() {
        super("transaction-browser.fxml", "Browse transactions", "overview_96.png");

        getDialogPane().getButtonTypes().clear();
        getDialogPane().getButtonTypes().setAll(new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE));
        getDialogPane().setMinWidth(1050);
        getDialogPane().setMinHeight(700);
        initModality(Modality.WINDOW_MODAL);
        setAllowResize(true);
        getDialogPane().getScene().addEventFilter(KeyEvent.KEY_PRESSED, (e) -> {
            if (e.getCode().equals(KeyCode.F5)) {
                performSearch();
            } else if (e.getCode().equals(KeyCode.F) && e.isControlDown()) {
                commentFilter.requestFocus();
            }
        });

        initUI();
    }

    private List<Transaction> queryTransactions() {
        return data.getTransactions(
                typeFilter.getValue().asType(),
                baseCategoryFilter.getValue(),
                dateFilterSelector.getValue(),
                dateFilter1.getValue(),
                timeFilter1.getText().isEmpty() ? null : LocalTime.parse(timeFilter1.getText(), DateTimeFormatter.ofPattern("HH:mm:ss")),
                dateFilter2.getValue(),
                timeFilter2.getText().isEmpty() ? null : LocalTime.parse(timeFilter2.getText(), DateTimeFormatter.ofPattern("HH:mm:ss")),
                commentExactMatchEnabled.get() ? commentFilter.getText() : (commentFilter.getText().isEmpty() ? null : "%" + commentFilter.getText() + "%"),
                customCategoryExactMatchEnabled.get() ? exactCustomCategoryFilter.getValue() : (customCategoryFilter.getText().isEmpty() ? null : "%" + customCategoryFilter.getText() + "%"),
                accountFilter.getValue() == null ? null : accountFilter.getValue().getId(),
                searchModeSelector.getValue()
        );
    }

    private void performSearch() {
        LoadingDialog dlg = new LoadingDialog(LoadingDialog.LoadingOperationType.INDETERMINATE_SPINNER);
        dlg.setPrimaryLabel("Looking for transactions, hold on");
        dlg.setSecondaryLabel("Executing SQL query...");
        dlg.show("Searching...", () -> {
            long start = System.currentTimeMillis();
            List<Transaction> results = queryTransactions();
            Platform.runLater(() -> {
                searchInfoLabel.setText("%s results • query took %s ms".formatted(results.size(), System.currentTimeMillis() - start));
                resultList.getItems().clear();
                resultList.getItems().setAll(results);

                if (results.isEmpty()) {
                    PopupManager.showPopup("Query returned no results", "No transactions matched the specified criteria.", Alert.AlertType.INFORMATION);
                }
            });
        });
    }

    private void initUI() {
        searchInfoLabel.setText("");
        typeFilter.getItems().setAll(TransactionTypeFilter.values());
        typeFilter.getSelectionModel().select(TransactionTypeFilter.ANY);
        typeFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(TransactionTypeFilter object) {
                if (object != null) {
                    return object.getDisplayName();
                } else {
                    return "Select type...";
                }
            }

            @Override
            public TransactionTypeFilter fromString(String string) {
                return null;
            }
        });

        baseCategoryFilter.getItems().setAll(TransactionCategory.values());
        baseCategoryFilter.setConverter(new StringConverter<TransactionCategory>() {
            @Override
            public String toString(TransactionCategory object) {
                if (object != null) {
                    return Formatting.humanize(object.name());
                } else {
                    return "Select category...";
                }
            }

            @Override
            public TransactionCategory fromString(String string) {
                return null;
            }
        });
        baseCategoryFilter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(TransactionCategory item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText("Select category...");
                } else {
                    setText(Formatting.humanize(item.name()));
                }
            }
        });

        dateFilter1.managedProperty().bind(dateFilter1.visibleProperty());
        dateFilter2.managedProperty().bind(dateFilter2.visibleProperty());
        timeFilter1.managedProperty().bind(timeFilter1.visibleProperty());
        timeFilter2.managedProperty().bind(timeFilter2.visibleProperty());
        arrowIcon.managedProperty().bind(arrowIcon.visibleProperty());
        arrowIcon.visibleProperty().bind(dateFilter1.visibleProperty().and(dateFilter2.visibleProperty()));

        dateFilter1.setConverter(Formats.DATE_FORMAT);
        dateFilter2.setConverter(Formats.DATE_FORMAT);
        timeFilter1.textProperty().addListener((obs, old, val) -> {
            timeFilter1.setStyle("-fx-text-fill: " + (timeFilter1.getText().matches(Formats.TIME_VALIDATION_EXPRESSION) ? "black" : "red"));
        });
        timeFilter2.textProperty().addListener((obs, old, val) -> {
            timeFilter1.setStyle("-fx-text-fill: " + (timeFilter1.getText().matches(Formats.TIME_VALIDATION_EXPRESSION) ? "black" : "red"));
        });

        dateFilterSelector.getItems().setAll(DateFilteringMode.values());
        dateFilterSelector.setConverter(new StringConverter<DateFilteringMode>() {
            @Override
            public String toString(DateFilteringMode object) {
                return object.getDisplayName();
            }

            @Override
            public DateFilteringMode fromString(String string) {
                return null;
            }
        });
        dateFilterSelector.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                if (val.equals(DateFilteringMode.NONE)) {
                    dateFilter1.setVisible(false);
                    dateFilter2.setVisible(false);
                    timeFilter1.setVisible(false);
                    timeFilter2.setVisible(false);
                } else if (val.equals(DateFilteringMode.RANGE)) {
                    dateFilter1.setVisible(true);
                    dateFilter2.setVisible(true);
                    timeFilter1.setVisible(true);
                    timeFilter2.setVisible(true);
                } else {
                    dateFilter1.setVisible(true);
                    timeFilter1.setVisible(true);
                    dateFilter2.setVisible(false);
                    timeFilter2.setVisible(false);
                }
            }
        });
        dateFilterSelector.getSelectionModel().select(DateFilteringMode.NONE);

        searchModeSelector.getItems().setAll(SearchMode.values());
        searchModeSelector.getSelectionModel().select(SearchMode.AND);
        searchModeSelector.setConverter(new StringConverter<SearchMode>() {
            @Override
            public String toString(SearchMode object) {
                return object.name();
            }

            @Override
            public SearchMode fromString(String string) {
                return null;
            }
        });
        searchModeSelector.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                searchModeSelector.setTooltip(new Tooltip(
                        "Changes the search mode.\nCurrent: " + val.name() + " - " + val.getDescription()
                ));
            }
        });

        Label l = new Label("No results to show.");
        l.getStyleClass().add("medium-label");
        resultList.setPlaceholder(l);
        resultList.setCellFactory(new TransactionCellFactory(null, _ -> {
        }));

        accountFilter.getItems().setAll(data.getAccounts());
        accountFilter.setConverter(new StringConverter<Account>() {
            @Override
            public String toString(Account object) {
                if (object != null) {
                    return object.getName();
                } else {
                    return "Select account...";
                }
            }

            @Override
            public Account fromString(String string) {
                return null;
            }
        });
        accountFilter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setText("Select account...");
                } else {
                    setText(item.getName());
                }
            }
        });

        exactCustomCategoryFilter.getItems().setAll(data.getCustomTransactionCategories());
        exactCustomCategoryFilter.managedProperty().bind(exactCustomCategoryFilter.visibleProperty());
        exactCustomCategoryFilter.visibleProperty().bind(customCategoryExactMatchEnabled);
        exactCustomCategoryFilter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setText("Select custom category...");
                } else {
                    setText(item);
                }
            }
        });

        customCategoryFilter.managedProperty().bind(customCategoryFilter.visibleProperty());
        customCategoryFilter.visibleProperty().bind(customCategoryExactMatchEnabled.not());

        customCategoryLabel.setOnMouseClicked(_ -> {
            customCategoryExactMatchEnabled.set(!customCategoryExactMatchEnabled.get());
            customCategoryLabel.setText(customCategoryExactMatchEnabled.get() ? "Custom category is: " : "Custom category contains: ");
        });

        commentLabel.setOnMouseClicked(_ -> {
            commentExactMatchEnabled.set(!commentExactMatchEnabled.get());
            commentLabel.setText(commentExactMatchEnabled.get() ? "Comment is: " : "Comment contains: ");
        });


        resetFiltersButton.setOnAction(_ -> {
            accountFilter.getSelectionModel().clearSelection();
            typeFilter.setValue(TransactionTypeFilter.ANY);
            baseCategoryFilter.setValue(null);
            dateFilterSelector.setValue(DateFilteringMode.NONE);
            dateFilter1.setValue(null);
            dateFilter2.setValue(null);
            timeFilter1.setText("");
            timeFilter2.setText("");
            commentFilter.setText("");
            customCategoryFilter.setText("");
            exactCustomCategoryFilter.setValue("");
            customCategoryExactMatchEnabled.set(false);
            commentExactMatchEnabled.set(false);
        });

        searchButton.setOnAction(_ -> {
            performSearch();
        });
    }
}
