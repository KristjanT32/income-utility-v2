package com.krisapps.incomeutility_v2.types.pricer;

import java.util.List;

public record Dish(int id, String name, List<DishIngredient> ingredients) {

    public Double getQuantity(Product p) {
        return ingredients.stream().filter(e -> e.product().equals(p)).findAny().orElse(new DishIngredient(-1, -1, null, 0.0d)).quantity();
    }

    public Double getTotalPrice() {
        double price = 0.0d;
        for (DishIngredient ingredient : ingredients) {
            price += ingredient.product().pricePerUnit() * ingredient.quantity();
        }

        return price;
    }

    public Double getTotalPurchasePrice() {
        double price = 0.0d;
        for (DishIngredient ingredient : ingredients) {
            price += ingredient.product().price();
        }

        return price;
    }
}
