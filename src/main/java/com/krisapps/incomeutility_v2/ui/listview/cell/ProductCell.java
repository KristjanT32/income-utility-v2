package com.krisapps.incomeutility_v2.ui.listview.cell;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.dialogs.EditProductDialog;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.pricer.Product;
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
import java.util.Optional;
import java.util.function.Consumer;

public class ProductCell extends ListCell<Product> {

    @FXML
    private VBox rootPane;

    @FXML
    private Label productNameLabel;

    @FXML
    private Label priceLabel;

    @FXML
    private Label quantityLabel;

    @FXML
    private Label durationLabel;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private HBox buttonBar;

    private final Consumer<Product> onAddRequest;
    private final Consumer<Product> onEditRequest;
    private final Consumer<Product> onDeleteRequest;
    private final CurrencyConfig currencyConfig;

    private final boolean canAdd;
    private final boolean canEdit;
    private final boolean canDelete;
    private final BooleanExpression expression = hoverProperty().or(selectedProperty());

    public ProductCell(Consumer<Product> onAddRequest, Consumer<Product> onEditRequest, Consumer<Product> onDeleteRequest, CurrencyConfig currencyConfig, boolean canAdd, boolean canEdit, boolean canDelete) {
        this.onAddRequest = onAddRequest;
        this.onEditRequest = onEditRequest;
        this.onDeleteRequest = onDeleteRequest;
        this.currencyConfig = currencyConfig;
        this.canAdd = canAdd;
        this.canEdit = canEdit;
        this.canDelete = canDelete;
        loadFXML();
    }

    private void loadFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/ui/product_cell.fxml"));
            loader.setController(this);
            rootPane = loader.load();

            addButton.setOnAction((e) -> {
                if (!canAdd) return;
                onAddRequest.accept(getItem());
            });

            editButton.setOnAction((e) -> {
                if (!canEdit) return;
                EditProductDialog dialog = new EditProductDialog(this.getItem());
                Optional<Product> updated = dialog.showAndWait();

                updated.ifPresent(onEditRequest);
            });

            deleteButton.setOnAction((e) -> {
                if (!canDelete) return;
                onDeleteRequest.accept(this.getItem());
            });

            addButton.managedProperty().bind(addButton.visibleProperty());
            editButton.managedProperty().bind(editButton.visibleProperty());
            deleteButton.managedProperty().bind(deleteButton.visibleProperty());

            durationLabel.managedProperty().bind(durationLabel.visibleProperty());
            quantityLabel.managedProperty().bind(quantityLabel.visibleProperty());

            addButton.setVisible(canAdd);
            editButton.setVisible(canEdit);
            deleteButton.setVisible(canDelete);

            buttonBar.managedProperty().bind(buttonBar.visibleProperty());
            buttonBar.setVisible(isSelected());
            expression.addListener(((obs, old, val) -> {
                buttonBar.setVisible(val);
                durationLabel.setVisible(!val);
                quantityLabel.setVisible(!val);
            }));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void updateItem(Product item, boolean empty) {
        super.updateItem(item, empty);

        if (!empty) {
            productNameLabel.setText(item.name());
            productNameLabel.setStyle("-fx-text-fill: black");

            priceLabel.setText(Formatting.formatMoney(item.price(), currencyConfig));
            priceLabel.setStyle("-fx-text-fill: black");

            quantityLabel.setText(item.quantityString());
            quantityLabel.setStyle("-fx-text-fill: black");

            durationLabel.setText(String.format("Lasts for %s day%s", Formatting.formatDouble(item.durationOfUse()), (item.durationOfUse() == 1 ? "" : "s")));
            durationLabel.setStyle("-fx-text-fill: black");

            setGraphic(rootPane);
            rootPane.prefWidthProperty().bind(getListView().widthProperty().subtract(20));
        } else {
            setText(null);
            setGraphic(null);
        }
    }
}
