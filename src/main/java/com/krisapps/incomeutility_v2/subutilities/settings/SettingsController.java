package com.krisapps.incomeutility_v2.subutilities.settings;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.dialogs.generic.DropdownDialog;
import com.krisapps.incomeutility_v2.subutilities.SubUtility;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityController;
import com.krisapps.incomeutility_v2.types.data.ConfigurationData;
import com.krisapps.incomeutility_v2.ui.listview.CustomCategoryCellFactory;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.Logging;
import com.krisapps.incomeutility_v2.util.PopupManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class SettingsController extends SubUtilityController {

    @FXML
    private Button backButton;

    @FXML
    private Button addCategoryButton;

    @FXML
    private ListView<Pair<Integer, String>> categoryList;

    @FXML
    private TextField dbFilePathField;

    @FXML
    private Button dbFilePicker;

    @FXML
    private ToggleButton pricerCurrencyToggle;

    @FXML
    private TextField pricerCurrencyField;

    private SubUtility self;
    private final DataManager dataman = DataManager.getInstance();
    private ConfigurationData currentData;

    private final FileChooser dbPicker = new FileChooser();

    @FXML
    public void initialize() {
        currentData = dataman.getConfigurationData();
        initUI();
    }

    @Override
    public void onStartup(SubUtility utility) {
        this.self = utility;

        self.getInstance().getScene().addEventFilter(KeyEvent.KEY_PRESSED, (event) -> {
            if (event.getCode().equals(KeyCode.ESCAPE)) {
                categoryList.getItems().removeIf(item -> item.getKey() == -1);
                self.log("Cleared all pending category additions");
            }
        });
    }

    @Override
    public void onShutdown() {
        dataman.saveCurrentConfigurationData();
    }

    @Override
    public void onPromptCommand(String command, String[] args) {
        switch (command) {
            case "initdb" -> {
                Optional<ButtonType> response = PopupManager.showConfirmation("Are you sure you wish to reinitialize the database?",
                        "Reinitializing will create all required tables and set them up.\n\nThis might be necessary if the schema changes.",
                        new ButtonType("Reinitialize", ButtonBar.ButtonData.APPLY),
                        new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                );

                response.ifPresent(r -> {
                    if (r.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                        dataman.reinitializeCurrentDatabase();
                        PopupManager.showPopup("Initialization completed!", "The database was successfully re-initialized.", Alert.AlertType.INFORMATION);
                    }
                });
            }
            case "drop" -> {
                DropdownDialog<String> tableNamePicker = new DropdownDialog<>("Choose which table to drop");
                tableNamePicker.setPrimaryLabel("Drop a table");
                tableNamePicker.setDescription("The drop utility command requires you to specify which table to drop.\nIf you wish to drop all tables, run the drop-all utility command.");
                tableNamePicker.setItems(dataman.getTables());

                Optional<String> tableToDrop = tableNamePicker.showAndWait();
                tableToDrop.ifPresent(table -> {
                    Optional<ButtonType> choice = PopupManager.showConfirmation("Drop table '" + table + "'?", "Are you sure you wish to drop the table '" + table + "'?\nThis will irrevocably delete all data in that table and require you to run 'initdb' afterwards.",
                            new ButtonType("Drop table", ButtonBar.ButtonData.APPLY),
                            new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                    );

                    choice.ifPresent(b -> {
                        if (b.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                            if (dataman.dropTable(table)) {
                                PopupManager.showPopup("Table dropped", "The table '" + table + "' was dropped. Please run 'initdb' to reinitialize the database.", Alert.AlertType.INFORMATION);
                            }
                        }
                    });
                });
            }
            case "drop-all" -> {
                Optional<ButtonType> choice = PopupManager.showConfirmation("Drop all tables?", "Are you absolutely and unshakably sure you wish to drop all tables?\nThis cannot be undone and will irrevocably delete all program data.",
                        new ButtonType("Drop all tables", ButtonBar.ButtonData.APPLY),
                        new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                );

                choice.ifPresent(b -> {
                    if (b.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                        if (dataman.dropAllTables()) {
                            PopupManager.showPopup("Tables dropped", "All tables have been dropped.", Alert.AlertType.INFORMATION);
                        }
                    }
                });
            }
            case "reset" -> {
                Optional<ButtonType> choice = PopupManager.showConfirmation("Reset database", "Are you absolutely and unshakably sure you wish to reset the database?\nThis operation will drop all tables and reinitialize the database schema.",
                        new ButtonType("Reset everything", ButtonBar.ButtonData.APPLY),
                        new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                );

                choice.ifPresent(b -> {
                    if (b.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                        dataman.dropAllTables();
                        dataman.reinitializeCurrentDatabase();
                        PopupManager.showPopup("Application database reset", "The database was reset and initialized successfully.", Alert.AlertType.INFORMATION);
                    }
                });
            }

            case "clear-log" -> {
                Optional<ButtonType> choice = PopupManager.showConfirmation("Clear log file?", "Are you sure you'd like to clear the log file?\nThis cannot be undone.",
                        new ButtonType("Yes, clear logs", ButtonBar.ButtonData.APPLY),
                        new ButtonType("No, cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                );

                choice.ifPresent(b -> {
                    if (b.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                        Logging.getInstance().truncateLogFile();
                        PopupManager.showPopup("Logs cleared!", "The log file was truncated successfully.", Alert.AlertType.INFORMATION);
                    }
                });
            }

            case "refresh" -> {
                refreshGlobalSettings();
                refreshMoneyFlowSettings();
                refreshPricerSettings();
            }
            case "discard" -> {
                currentData = null;
                currentData = dataman.getConfigurationData();
                PopupManager.showPopup("Changes discarded", "All pending changes have been discarded!", Alert.AlertType.INFORMATION);
            }
            case "exit" -> self.stop();
            case "help" -> {
                StringBuilder commands = new StringBuilder();
                commands.append("The following utility commands are available from this utility:\n");
                for (String cmd : List.of("initdb", "drop", "drop-all", "reset", "clear-log", "refresh", "discard", "exit", "help")) {
                    commands.append("       - " + cmd + "\n");
                }

                PopupManager.showPopup("Available commands", commands.toString(), Alert.AlertType.INFORMATION);
            }
        }
    }

    private void initUI() {
        backButton.setOnAction((e) -> {
            self.stop();
        });

        addCategoryButton.setOnAction(this::addBlankCategory);

        categoryList.setCellFactory(new CustomCategoryCellFactory(
                (id, newValue) -> {
                    if (dataman.customTransactionCategoryExists(id)) {
                        dataman.updateCustomTransactionCategory(id, newValue);
                    } else {
                        dataman.addCustomTransactionCategory(newValue);
                    }
                    refreshCategoryList();
                },
                (id) -> {
                    if (id == 0) {
                        PopupManager.showPopup("System category", "Sorry, but this is a system-reserved category and cannot be deleted.", Alert.AlertType.ERROR);
                        return;
                    }

                    if (dataman.getTransactionsWithCustomCategory(id).isEmpty()) {
                        PopupManager.showConfirmation("Delete category?", "Are you sure you wish to delete the selected category?\nThis cannot be undone.",
                                new ButtonType("Yes, delete", ButtonBar.ButtonData.APPLY),
                                new ButtonType("No, cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                        ).ifPresent(r -> {
                            if (r.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                                dataman.removeCustomTransactionCategory(id);
                                refreshCategoryList();
                                self.log("Deleted custom transaction category #" + id);
                            }
                        });
                        return;
                    }

                    Optional<ButtonType> response = PopupManager.showConfirmation(
                            "Category in use",
                            "The category you are trying to delete is in use in some transactions.\nTo delete it, you will need to specify a replacement transaction category.",
                            new ButtonType("Choose replacement", ButtonBar.ButtonData.APPLY),
                            new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                    );

                    response.ifPresent(r -> {
                        if (r.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                            List<Pair<Integer, String>> categories = dataman.getCustomTransactionCategoryEntries().stream().filter(cat -> !cat.getKey().equals(id)).toList();

                            DropdownDialog<Pair<Integer, String>> chooser = new DropdownDialog<>("Choose replacement category");
                            chooser.setItems(categories);
                            chooser.setStringConverter(new StringConverter<>() {
                                @Override
                                public String toString(Pair<Integer, String> object) {
                                    return object == null ? "Select option..." : object.getValue();
                                }

                                @Override
                                public Pair<Integer, String> fromString(String string) {
                                    return null;
                                }
                            });
                            chooser.setPrimaryLabel("Select replacement");
                            chooser.setDescription("To delete the selected category, please choose a replacement category from the list below.");
                            Optional<Pair<Integer, String>> replacement = chooser.showAndWait();

                            replacement.ifPresent(integerStringPair -> {
                                dataman.replaceCustomTransactionInTransactions(id, integerStringPair.getKey());
                                dataman.removeCustomTransactionCategory(id);
                                self.log("Deleted custom transaction category #" + id);
                            });
                            refreshCategoryList();
                        }
                    });
                    refreshCategoryList();
                }
        ));

        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Database files", "*.db", "*.sql", "*.sqlite");
        dbPicker.setTitle("Select database file");
        dbPicker.getExtensionFilters().add(filter);
        dbPicker.setInitialDirectory(DataManager.getDataDirectory().toFile());

        dbFilePicker.setOnAction((_) -> {
            File f = dbPicker.showOpenDialog(self.getInstance().getOwner());
            if (f != null) {
                dbFilePathField.setText(f.getPath());

                dataman.updateDatabaseLocation(f.toPath());
                self.log("Data source switched to: " + f.getPath());
                promptRestartProgram();
            }
        });

        dbFilePathField.setOnAction(e -> {
            try {
                Path p = Path.of(dbFilePathField.getText());

                if (!p.toString().isEmpty() && p.toFile().exists() && p.toFile().isFile()) {
                    dataman.updateDatabaseLocation(p);
                    self.log("Data source switched to: " + p);
                    promptRestartProgram();
                } else {
                    dbFilePathField.setText(dataman.getConfigurationData().getDatabaseLocation().toString());
                }
            } catch (InvalidPathException _) {
                dbFilePathField.setText(dataman.getConfigurationData().getDatabaseLocation().toString());
            }
        });

        pricerCurrencyToggle.selectedProperty().addListener((obs, old, val) -> {
            pricerCurrencyToggle.setText(val ? "Prefixed" : "Suffixed");
            dataman.updatePricerCurrencyIsPrefix(val);
        });

        pricerCurrencyField.setOnAction(_ -> {
            if (!pricerCurrencyField.getText().isEmpty()) {
                dataman.updatePricerCurrencySymbol(pricerCurrencyField.getText().trim());
            }
        });

        VBox box = new VBox();
        HBox.setHgrow(box, Priority.ALWAYS);

        box.setAlignment(Pos.CENTER);
        box.setSpacing(10);

        Label l = new Label("No custom categories have been added yet.");
        l.getStyleClass().add("medium-label");

        Button add = new Button("Add category");
        add.setOnAction(this::addBlankCategory);

        box.getChildren().add(l);
        box.getChildren().add(add);
        categoryList.setPlaceholder(box);

        refreshGlobalSettings();
        refreshMoneyFlowSettings();
        refreshPricerSettings();
    }

    private void refreshGlobalSettings() {
        dbFilePathField.setText(dataman.getConfigurationData().getDatabaseLocation().toString());
    }

    private void refreshMoneyFlowSettings() {
        refreshCategoryList();
    }

    private void refreshPricerSettings() {
        pricerCurrencyField.setText(currentData.getPricerCurrencyConfiguration().getCurrencySymbol());
        pricerCurrencyToggle.setSelected(currentData.getPricerCurrencyConfiguration().isCurrencySymbolPrefix());
    }

    private void addBlankCategory() {
        categoryList.getItems().add(new Pair<>(-1, "New category..."));
    }

    private void refreshCategoryList() {
        categoryList.getItems().clear();
        categoryList.getItems().setAll(dataman.getCustomTransactionCategoryEntries());
    }

    private void addBlankCategory(ActionEvent ev) {
        addBlankCategory();
        int focusIndex = categoryList.getItems().indexOf(categoryList.getItems().getLast());
        categoryList.getSelectionModel().clearSelection();
        categoryList.getFocusModel().focus(focusIndex);
        categoryList.getSelectionModel().select(focusIndex);
        categoryList.scrollTo(focusIndex);

    }

    private void promptRestartProgram() {
        PopupManager.showConfirmation("Data source changed", "The active data source for Income Utility has changed - for these changes to take effect, the utility needs to be restarted.\n\nWould you like to restart the utility now?",
                new ButtonType("Yes, restart", ButtonBar.ButtonData.APPLY),
                new ButtonType("No, I'll restart later", ButtonBar.ButtonData.CANCEL_CLOSE)
        ).ifPresent(r -> {
            if (r.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                IncomeUtilityApplication.restart();
            }
        });
    }
}
