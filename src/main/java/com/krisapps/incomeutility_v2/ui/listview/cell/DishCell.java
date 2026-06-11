package com.krisapps.incomeutility_v2.ui.listview.cell;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.pricer.Dish;
import com.krisapps.incomeutility_v2.util.Formatting;
import javafx.beans.binding.BooleanExpression;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.function.Consumer;

public class DishCell extends ListCell<Dish> {

    @FXML
    private VBox rootPane;

    @FXML
    private Label dishNameLabel;

    @FXML
    private Label servingPriceLabel;

    @FXML
    private Label servingCountLabel;

    @FXML
    private Button openButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private HBox buttonBar;

    private final Consumer<Dish> onOpenRequest;
    private final Consumer<Dish> onEditRequest;
    private final Consumer<Dish> onDeleteRequest;
    private final CurrencyConfig currencyConfig;

    private final boolean canOpen;
    private final boolean canEdit;
    private final boolean canDelete;
    private final BooleanExpression expression = hoverProperty().or(selectedProperty());

    public DishCell(Consumer<Dish> onOpenRequest, Consumer<Dish> onEditRequest, Consumer<Dish> onDeleteRequest, CurrencyConfig currencyConfig, boolean canOpen, boolean canEdit, boolean canDelete) {
        this.onOpenRequest = onOpenRequest;
        this.onEditRequest = onEditRequest;
        this.onDeleteRequest = onDeleteRequest;
        this.currencyConfig = currencyConfig;
        this.canOpen = canOpen;
        this.canEdit = canEdit;
        this.canDelete = canDelete;
        loadFXML();
    }

    private void loadFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/ui/dish_cell.fxml"));
            loader.setController(this);
            rootPane = loader.load();

            openButton.setOnAction((e) -> {
                if (!canOpen) return;
                onOpenRequest.accept(getItem());
            });

            editButton.setOnAction((e) -> {
                if (!canEdit) return;
                onEditRequest.accept(getItem());
            });

            deleteButton.setOnAction((e) -> {
                if (!canDelete) return;
                onDeleteRequest.accept(getItem());
            });

            openButton.managedProperty().bind(openButton.visibleProperty());
            editButton.managedProperty().bind(editButton.visibleProperty());
            deleteButton.managedProperty().bind(deleteButton.visibleProperty());

            openButton.setVisible(canOpen);
            editButton.setVisible(canEdit);
            deleteButton.setVisible(canDelete);

            buttonBar.managedProperty().bind(buttonBar.visibleProperty());
            buttonBar.setVisible(isSelected());
            expression.addListener(((obs, old, val) -> {
                buttonBar.setVisible(val);
            }));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void updateItem(Dish item, boolean empty) {
        super.updateItem(item, empty);

        if (!empty && item != null) {
            dishNameLabel.setText(item.name());
            dishNameLabel.setStyle("-fx-text-fill: black");

            servingPriceLabel.setText(String.format("%s per serving", Formatting.formatMoney(item.servingPrice(), currencyConfig)));
            servingPriceLabel.setStyle("-fx-text-fill: black");

            servingCountLabel.setText(String.format("%s serving%s", Formatting.formatDouble(item.servings()), (item.servings() == 1 ? "" : "s")));
            servingCountLabel.setStyle("-fx-text-fill: black");

            setGraphic(rootPane);
        } else {
            setText(null);
            setGraphic(null);
        }
    }
}
