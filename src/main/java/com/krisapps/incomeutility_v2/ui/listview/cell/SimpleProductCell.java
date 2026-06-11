package com.krisapps.incomeutility_v2.ui.listview.cell;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
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
import java.util.function.Consumer;

public class SimpleProductCell extends ListCell<Product> {

    @FXML
    private VBox rootPane;

    @FXML
    private Label productNameLabel;

    @FXML
    private Label priceLabel;

    @FXML
    private Label quantityLabel;

    @FXML
    private Button addButton;

    @FXML
    private HBox buttonBar;

    private final Consumer<Product> onAddRequest;
    private final CurrencyConfig currencyConfig;

    private final boolean canAdd;
    private final BooleanExpression expression = hoverProperty().or(selectedProperty());

    public SimpleProductCell(Consumer<Product> onAddRequest, CurrencyConfig currencyConfig, boolean canAdd) {
        this.onAddRequest = onAddRequest;
        this.currencyConfig = currencyConfig;
        this.canAdd = canAdd;
        loadFXML();
    }

    private void loadFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/ui/product_cell_simple.fxml"));
            loader.setController(this);
            rootPane = loader.load();

            addButton.setOnAction((e) -> {
                onAddRequest.accept(getItem());
            });

            addButton.managedProperty().bind(addButton.visibleProperty());
            addButton.setVisible(canAdd);

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
    protected void updateItem(Product item, boolean empty) {
        super.updateItem(item, empty);

        if (!empty) {
            productNameLabel.setText(item.name());
            productNameLabel.setStyle("-fx-text-fill: black");

            priceLabel.setText(Formatting.formatMoney(item.price(), currencyConfig));
            priceLabel.setStyle("-fx-text-fill: black");

            quantityLabel.setText(item.quantityString());
            quantityLabel.setStyle("-fx-text-fill: black");

            setGraphic(rootPane);
            rootPane.prefWidthProperty().bind(getListView().widthProperty().subtract(20));
        } else {
            setText(null);
            setGraphic(null);
        }
    }
}
