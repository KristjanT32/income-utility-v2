package com.krisapps.incomeutility_v2.ui.listview.cell;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.pricer.DishIngredient;
import com.krisapps.incomeutility_v2.types.pricer.Product;
import com.krisapps.incomeutility_v2.util.PopupManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class IngredientCell extends ListCell<DishIngredient> {

    @FXML
    private VBox rootPane;

    @FXML
    private Label productNameLabel;

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

        return Double.parseDouble(quantityBox.getTextFormatter().getValue().toString());
    }

    private void loadFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/ui/ingredient_cell.fxml"));
            loader.setController(this);
            rootPane = loader.load();

            increaseQuantity.setOnMouseClicked(event -> {
                if (event.isShiftDown()) {
                    onQuantityChangeRequest.accept(getItem().product(), getQuantity() + getItem().product().smallestUnit() * 10);
                } else if (event.isControlDown()) {
                    onQuantityChangeRequest.accept(getItem().product(), getQuantity() + getItem().product().unitsPerProduct());
                } else {
                    onQuantityChangeRequest.accept(getItem().product(), getQuantity() + getItem().product().smallestUnit());
                }
            });

            decreaseQuantity.setOnMouseClicked(event -> {
                if (getQuantity() == 0) {
                    Optional<ButtonType> choice = PopupManager.showConfirmation("Remove product?", "Would you like to remove this product from the dish?",
                            new ButtonType("Yes, remove", ButtonBar.ButtonData.APPLY),
                            new ButtonType("No, leave it in", ButtonBar.ButtonData.CANCEL_CLOSE)
                    );

                    choice.ifPresent(c -> {
                        if (c.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                            onDeleteRequest.accept(getItem().product());
                        }
                    });
                    return;
                }
                ;
                if (event.isShiftDown()) {
                    if (getQuantity() - getItem().product().smallestUnit() * 10 < 0) return;
                    onQuantityChangeRequest.accept(getItem().product(), getQuantity() - getItem().product().smallestUnit() * 10);
                } else if (event.isControlDown()) {
                    if (getQuantity() - getItem().product().unitsPerProduct() < 0) return;
                    onQuantityChangeRequest.accept(getItem().product(), getQuantity() - getItem().product().unitsPerProduct());
                } else {
                    if (getQuantity() - getItem().product().smallestUnit() < 0) return;
                    onQuantityChangeRequest.accept(getItem().product(), getQuantity() - getItem().product().smallestUnit());
                }
            });

            Tooltip increaseTooltip = new Tooltip("Increase the quantity by the smallest unit.\n\nShift+Click: Add 10x the smallest unit\nCtrl+Click: Add an entire pack");
            Tooltip decreaseTooltip = new Tooltip("Decrease the quantity by the smallest unit.\n\nShift+Click: Remove 10x the smallest unit\nCtrl+Click: Remove an entire pack");

            Tooltip.install(increaseQuantity, increaseTooltip);
            Tooltip.install(decreaseQuantity, decreaseTooltip);

            quantityBox.setOnAction((e) -> {
                onQuantityChangeRequest.accept(getItem().product(), getQuantity());
            });

            quantityBox.setTextFormatter(new TextFormatter<>(new StringConverter<Double>() {
                @Override
                public String toString(Double object) {
                    if (getItem() == null) return "0.0";
                    if (object == null) return getItem().product().quantityString(0);
                    return getItem().product().quantityString(object);
                }

                @Override
                public Double fromString(String string) {
                    if (getItem() == null) return 0.0d;
                    if (string == null || string.trim().isEmpty()) return 0.0d;
                    return Double.parseDouble(string.replace(getItem().product().unitSingular(), "").replace(getItem().product().unitPlural(), "").trim());
                }
            }));

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
            double productCount = Math.ceil(item.quantity() / item.product().unitsPerProduct());
            productNameLabel.setText(String.format("%s %s", item.product().name(), (productCount == 1 ? "" : String.format("(%s)", (int) productCount + " packs"))));
            productNameLabel.setStyle("-fx-text-fill: black");
            quantityBox.setText(item.quantity().toString());
            setGraphic(rootPane);
        } else {
            setText(null);
            setGraphic(null);
        }
    }
}
