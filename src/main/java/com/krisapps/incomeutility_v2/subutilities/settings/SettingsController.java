package com.krisapps.incomeutility_v2.subutilities.settings;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.dialogs.generic.DropdownDialog;
import com.krisapps.incomeutility_v2.subutilities.SubUtility;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityController;
import com.krisapps.incomeutility_v2.types.data.ConfigurationData;
import com.krisapps.incomeutility_v2.ui.listview.CustomCategoryCellFactory;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.PopupManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
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

    private SubUtility self;
    private final DataManager dataman = DataManager.getInstance();
    private ConfigurationData currentData;

    private final FileChooser dbPicker = new FileChooser();

    @FXML
    public void initialize() {
        initUI();
        refreshGlobalSettings();
        refreshMoneyFlowSettings();
    }

    @Override
    public void onStartup(SubUtility utility) {
        this.self = utility;

        self.getInstance().getScene().setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ESCAPE)) {
                categoryList.getItems().removeIf(item -> item.getKey() == -1);
                self.log("Cleared all pending category additions");
            }
        });

        currentData = dataman.getConfigurationData();
    }

    @Override
    public void onShutdown() {
        dataman.saveCurrentConfigurationData();
    }

    @Override
    public void onPromptCommand(String command, String[] args) {
        switch (command) {
            case "exit" -> self.stop();
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

        dbFilePathField.setText(dataman.getConfigurationData().getDatabaseLocation().toString());


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
    }

    private void refreshGlobalSettings() {

    }

    private void refreshMoneyFlowSettings() {
        refreshCategoryList();
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
