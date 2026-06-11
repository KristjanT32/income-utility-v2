package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.dialogs.generic.CopyTextDialog;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.pricer.Dish;
import com.krisapps.incomeutility_v2.types.pricer.DishIngredient;
import com.krisapps.incomeutility_v2.types.pricer.Product;
import com.krisapps.incomeutility_v2.ui.listview.IngredientCellFactory;
import com.krisapps.incomeutility_v2.util.Formatting;
import com.krisapps.incomeutility_v2.util.misc.PricerUtilities;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.LinkedList;
import java.util.Optional;

public class DishDetailsDialog extends IncomeUtilityDialog<Void> {

    @FXML
    private Label dishNameLabel;

    @FXML
    private Label dishTotalLabel;

    @FXML
    private Label dishProductsTotalLabel;

    @FXML
    private Label servingPriceLabel;

    @FXML
    private Label servingCountLabel;

    @FXML
    private ListView<DishIngredient> ingredientList;

    @FXML
    private Button generateShoppingListButton;

    @FXML
    private Button generateRecipeSummaryButton;

    private final Dish dish;
    private final CurrencyConfig currencyConfig;

    public DishDetailsDialog(Dish dish, CurrencyConfig currencyConfig) {
        super("dish-details.fxml", "Dish details", "overview_96.png");
        this.dish = dish;
        this.currencyConfig = currencyConfig;

        generateShoppingListButton.setOnAction(_ -> {
            LinkedList<Product> productList = new LinkedList<>();
            for (DishIngredient ingredient : dish.ingredients()) {
                if (ingredient.unitsOfProduct() > 1) {
                    // Add a product as many times as necessary to reach the quantity represented by the ingredient
                    for (int i = 0; i < ingredient.unitsOfProduct(); i++) {
                        productList.add(ingredient.product());
                    }
                } else {
                    if (ingredient.unitsOfProduct() <= 0) continue;
                    productList.add(ingredient.product());
                }
            }

            ShoppingListGeneratorDialog dialog = new ShoppingListGeneratorDialog(
                    productList,
                    currencyConfig
            );
            Optional<String> list = dialog.showAndWait();
            list.ifPresent(l -> {
                CopyTextDialog copyDlg = new CopyTextDialog("Shopping list for '" + dish.name() + "'");
                copyDlg.setContent(l);
                copyDlg.setPrimaryLabel("Shopping list");
                copyDlg.showAndWait();
            });
        });

        generateRecipeSummaryButton.setOnAction(_ -> {
            CopyTextDialog copyDialog = new CopyTextDialog("Recipe summary for '" + dish.name() + "'");
            copyDialog.setPrimaryLabel("Recipe summary");
            copyDialog.setContent(PricerUtilities.generateRecipeSummary(dish, currencyConfig));
            copyDialog.showAndWait();
        });

        refreshUI();
    }

    private void refreshUI() {
        dishNameLabel.setText(dish.name());
        dishTotalLabel.setText(Formatting.formatMoney(dish.totalPrice(), currencyConfig));
        dishProductsTotalLabel.setText(Formatting.formatMoney(dish.purchasePrice(), currencyConfig));
        servingPriceLabel.setText(
                String.format("%s (%s)", Formatting.formatMoney(
                        dish.servingPrice(),
                        currencyConfig
                ), Formatting.formatMoney(
                        dish.servingPurchasePrice(),
                        currencyConfig
                ))
        );
        servingCountLabel.setText(Formatting.formatDouble(dish.servings()));

        Label ingredientsPlaceholder = new Label("There are no ingredients in this dish.");
        ingredientsPlaceholder.getStyleClass().add("medium-label");
        ingredientList.setPlaceholder(ingredientsPlaceholder);

        ingredientList.setCellFactory(new IngredientCellFactory((product, quantity) -> {
            // Quantity changing is not supported for this list
        }, (product) -> {
            // Deleting is not supported for this list
        }, currencyConfig, true));
        ingredientList.getItems().setAll(dish.ingredients());
    }
}
