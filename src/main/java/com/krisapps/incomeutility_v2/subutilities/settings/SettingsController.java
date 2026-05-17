package com.krisapps.incomeutility_v2.subutilities.settings;

import com.krisapps.incomeutility_v2.subutilities.SubUtility;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityController;
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
import javafx.util.Pair;

import java.util.Optional;

public class SettingsController extends SubUtilityController {

    @FXML
    private Button backButton;

    @FXML
    private Button addCategoryButton;

    @FXML
    private ListView<Pair<Integer, String>> categoryList;

    private SubUtility self;
    private final DataManager data = DataManager.getInstance();

    @FXML
    public void initialize() {
        initUI();
        refreshUI();
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
    }

    @Override
    public void onShutdown() {

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
                    if (data.customTransactionCategoryExists(id)) {
                        data.updateCustomTransactionCategory(id, newValue);
                    } else {
                        data.addCustomTransactionCategory(newValue);
                    }
                    refreshCategoryList();
                },
                (id) -> {
                    Optional<ButtonType> response = PopupManager.showConfirmation(
                            "Category in use",
                            "The category you are trying to delete is in use in some transactions.\nTo delete it, you will need to specify a replacement transaction category.",
                            new ButtonType("Choose replacement", ButtonBar.ButtonData.APPLY),
                            new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                    );

                    response.ifPresent(r -> {
                        if (r.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                            // TODO: Implement combobox dialog to allow for choosing from a list of options
                            // TODO: Implement this
                        }
                    });

                    // data.replaceCustomTransactionInTransactions(id);
                    // data.removeCustomTransactionCategory(id);
                    PopupManager.showPredefinedPopup(PopupManager.PopupType.NOT_IMPLEMENTED);
                    refreshCategoryList();
                }
        ));


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

    private void refreshUI() {
        refreshCategoryList();
    }

    private void addBlankCategory() {
        categoryList.getItems().add(new Pair<>(-1, "New category..."));
    }

    private void refreshCategoryList() {
        categoryList.getItems().clear();
        categoryList.getItems().setAll(data.getCustomTransactionCategoryEntries());
    }

    private void addBlankCategory(ActionEvent ev) {
        addBlankCategory();
        int focusIndex = categoryList.getItems().indexOf(categoryList.getItems().getLast());
        categoryList.getSelectionModel().clearSelection();
        categoryList.getFocusModel().focus(focusIndex);
        categoryList.getSelectionModel().select(focusIndex);
        categoryList.scrollTo(focusIndex);
    }
}
