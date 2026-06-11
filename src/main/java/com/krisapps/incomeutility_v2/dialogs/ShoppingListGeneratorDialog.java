package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.pricer.Product;
import com.krisapps.incomeutility_v2.util.misc.PricerUtilities;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.LinkedList;

public class ShoppingListGeneratorDialog extends IncomeUtilityDialog<String> {

    @FXML
    private VBox rootPane;

    @FXML
    private TextField storeNameField;

    public ShoppingListGeneratorDialog(LinkedList<Product> products, CurrencyConfig currencyConfig) {
        super("shopping-list-setup.fxml", "Generate shopping list");

        ButtonType generateButton = new ButtonType("Generate shopping list!", ButtonBar.ButtonData.APPLY);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().setAll(generateButton, cancelButton);

        setResultConverter((response) -> {
            if (response.getButtonData() == ButtonBar.ButtonData.APPLY) {
                return PricerUtilities.generateShoppingList(products, storeNameField.getText(), currencyConfig);
            } else {
                return null;
            }
        });
    }


}
