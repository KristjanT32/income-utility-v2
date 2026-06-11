package com.krisapps.incomeutility_v2.util.misc;

import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.pricer.Dish;
import com.krisapps.incomeutility_v2.types.pricer.DishIngredient;
import com.krisapps.incomeutility_v2.types.pricer.Product;
import com.krisapps.incomeutility_v2.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PricerUtilities {

    public static String generateShoppingList(List<Product> products, @Nullable String stores, CurrencyConfig currencyConfig) {
        StringBuilder listBuilder = new StringBuilder();

        if (stores == null || stores.isEmpty()) {
            listBuilder.append("Shopping list\n");
        } else {
            String[] storeNames = stores.trim().split(",");
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
            listBuilder.append(String.format("- %s, %s %s\n", entry.getKey().name(), Formatting.formatMoney(entry.getKey().price(), currencyConfig), entry.getValue() > 1 ? "(x" + entry.getValue() + ")" : ""));
        }
        listBuilder.append("=================================");
        listBuilder.append("\n\nEstimated total: " + Formatting.formatMoney(products.stream().mapToDouble(Product::price).sum(), currencyConfig));
        return listBuilder.toString();
    }

    public static String generateRecipeSummary(Dish dish, CurrencyConfig currencyConfig) {
        StringBuilder recipeBuilder = new StringBuilder();

        recipeBuilder.append("Dish '" + dish.name() + "'\n");
        recipeBuilder.append("Serves " + Formatting.formatDouble(dish.servings()) + ", " + Formatting.formatMoney(dish.servingPrice(), currencyConfig) + " per serving.\n");
        recipeBuilder.append("\n\n");

        recipeBuilder.append("Ingredients -----------------------\n");
        for (DishIngredient ingredient : dish.ingredients()) {
            recipeBuilder.append(String.format("* %s of %s\n", ingredient.product().quantityString(ingredient.quantity()), ingredient.product().name()));
        }

        recipeBuilder.append("\n\nTotal price for ingredients: " + Formatting.formatMoney(dish.purchasePrice(), currencyConfig));
        return recipeBuilder.toString();
    }

}
