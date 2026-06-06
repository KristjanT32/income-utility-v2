package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.pricer.Product;
import com.krisapps.incomeutility_v2.util.Formatting;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ShoppingListGeneratorDialog extends IncomeUtilityDialog<String> {

    @FXML
    private VBox rootPane;

    @FXML
    private TextField nameField;

    public ShoppingListGeneratorDialog(LinkedList<Product> products, CurrencyConfig currencyConfig) {
        super("shopping-list-setup.fxml", "Generate shopping list");

        ButtonType generateButton = new ButtonType("Generate shopping list!", ButtonBar.ButtonData.APPLY);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().setAll(generateButton, cancelButton);

        setResultConverter((response) -> {
            if (response.getButtonData() == ButtonBar.ButtonData.APPLY) {
                return generateList(products, currencyConfig);
            } else {
                return null;
            }
        });
    }

    private String generateList(LinkedList<Product> products, CurrencyConfig currencyConfig) {
        StringBuilder listBuilder = new StringBuilder();

        if (nameField.getText().isEmpty()) {
            listBuilder.append("Shopping list\n");
        } else {
            String[] storeNames = nameField.getText().trim().split(",");
            listBuilder.append("Shopping list for ");

            if (storeNames.length >= 2) {
                int index = 0;
                for (String store : storeNames) {
                    if (store.isBlank()) continue;

                    listBuilder.append(store.trim());

                    if (index != storeNames.length - 1) {
                        if (index + 1 == storeNames.length - 1) {
                            listBuilder.append(" and ");
                        } else {
                            listBuilder.append(", ");
                        }
                    } else {
                        listBuilder.append("\n");
                    }
                    index++;
                }
            } else {
                listBuilder.append(storeNames[0] + "\n");
            }
        }

        listBuilder.append("=================================\n");
        listBuilder.append("Need:\n\n");

        HashMap<Product, Integer> compactedProductMap = new HashMap<>();
        for (Product product : products) {
            compactedProductMap.computeIfPresent(product, (key, val) -> val + 1);
            compactedProductMap.putIfAbsent(product, 1);
        }

        for (Map.Entry<Product, Integer> entry : compactedProductMap.entrySet()) {
            listBuilder.append(String.format("- %s, %s %s\n", entry.getKey().name(), entry.getKey().price(), entry.getValue() > 1 ? "(x" + entry.getValue() + ")" : ""));
        }
        listBuilder.append("=================================");
        listBuilder.append("\n\nEstimated total: " + Formatting.formatMoney(products.stream().mapToDouble(Product::price).sum(), currencyConfig));
        return listBuilder.toString();
    }
}
