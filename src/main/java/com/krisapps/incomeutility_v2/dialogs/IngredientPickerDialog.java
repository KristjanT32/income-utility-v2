package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.pricer.DishIngredient;
import com.krisapps.incomeutility_v2.types.pricer.Product;
import com.krisapps.incomeutility_v2.ui.listview.SimpleProductCellFactory;
import com.krisapps.incomeutility_v2.util.PopupManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class IngredientPickerDialog extends IncomeUtilityDialog<DishIngredient> {

    @FXML
    private ListView<Product> ingredientList;

    private boolean preventClose = false;

    public IngredientPickerDialog(List<Product> availableProducts, int targetDishId) {
        super("ingredient-picker.fxml", "Pick an ingredient to add", "overview_96.png");

        getDialogPane().getButtonTypes().setAll(
                new ButtonType("Select", ButtonBar.ButtonData.APPLY),
                new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        ingredientList.getItems().setAll(availableProducts);
        ingredientList.setCellFactory(new SimpleProductCellFactory(_ -> {
        }, CurrencyConfig.DEFAULT, false));

        Label l = new Label("No more products are available.");
        l.getStyleClass().add("medium-label");
        ingredientList.setPlaceholder(l);

        setOnCloseRequest(request -> {
            if (preventClose) {
                request.consume();
                preventClose = false;
            }
        });

        setResultConverter((response) -> {
            if (response.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                if (ingredientList.getSelectionModel().getSelectedItem() == null) {
                    PopupManager.showPopup("No product selected!", "Please select a product to add to your dish, or press 'Cancel' to cancel.", Alert.AlertType.ERROR);
                    preventClose = true;
                    return null;
                }

                return new DishIngredient(
                        -1,
                        targetDishId,
                        ingredientList.getSelectionModel().getSelectedItem(),
                        ingredientList.getSelectionModel().getSelectedItem().unitsPerProduct()
                );
            } else {
                return null;
            }
        });
    }
}
