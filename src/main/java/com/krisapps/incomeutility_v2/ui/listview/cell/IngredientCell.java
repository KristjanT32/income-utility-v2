package com.krisapps.incomeutility_v2.ui.listview.cell;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.pricer.DishIngredient;
import com.krisapps.incomeutility_v2.types.pricer.Product;
import com.krisapps.incomeutility_v2.util.Formatting;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class IngredientCell extends ListCell<DishIngredient> {

    @FXML
    private VBox rootPane;

    @FXML
    private Label productNameLabel;

    @FXML
    private Label priceLabel;

    @FXML
    private Button increaseQuantity;

    @FXML
    private Button decreaseQuantity;

    @FXML
    private TextField quantityBox;

    @FXML
    private Button deleteButton;

    private final BiConsumer<Product, Double> onQuantityChangeRequest;
    private final Consumer<Product> onDeleteRequest;
    private final CurrencyConfig currencyConfig;

    public IngredientCell(BiConsumer<Product, Double> onQuantityChangeRequest, Consumer<Product> onDeleteRequest, CurrencyConfig currencyConfig) {
        this.onQuantityChangeRequest = onQuantityChangeRequest;
        this.onDeleteRequest = onDeleteRequest;
        this.currencyConfig = currencyConfig;
        loadFXML();
    }

    private double getQuantity() {
        if (quantityBox == null) {
            return 0.0d;
        }

        return Double.parseDouble(quantityBox.getText());
    }

    private void loadFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/ui/ingredient_cell.fxml"));
            loader.setController(this);
            rootPane = loader.load();

            increaseQuantity.setOnAction((e) -> {
                onQuantityChangeRequest.accept(getItem().product(), getQuantity() + getItem().product().smallestUnit());
            });

            decreaseQuantity.setOnAction((e) -> {
                if (getQuantity() <= 0) return;
                onQuantityChangeRequest.accept(getItem().product(), getQuantity() - getItem().product().smallestUnit());
            });

            quantityBox.setOnAction((e) -> {
                onQuantityChangeRequest.accept(getItem().product(), getQuantity());
            });

            deleteButton.setOnAction((e) -> {
                onDeleteRequest.accept(getItem().product());
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void updateItem(DishIngredient item, boolean empty) {
        super.updateItem(item, empty);

        if (!empty) {
            productNameLabel.setText(item.product().name());
            productNameLabel.setStyle("-fx-text-fill: black");

            priceLabel.setText(Formatting.formatMoney(item.product().pricePerUnit() * item.quantity()));
            priceLabel.setStyle("-fx-text-fill: black");
            setGraphic(rootPane);
        } else {
            setText(null);
            setGraphic(null);
        }
    }
}
