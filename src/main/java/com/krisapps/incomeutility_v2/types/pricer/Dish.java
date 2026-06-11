package com.krisapps.incomeutility_v2.types.pricer;

import java.util.List;

public record Dish(int id, String name, double servings, List<DishIngredient> ingredients) {

    public Double getQuantity(Product p) {
        return ingredients.stream().filter(e -> e.product().equals(p)).findAny().orElse(new DishIngredient(-1, -1, null, 0.0d)).quantity();
    }

    public Double totalPrice() {
        return ingredients.stream().mapToDouble(ingredient -> ingredient.product().pricePerUnit() * ingredient.quantity()).sum();
    }

    public Double purchasePrice() {
        return ingredients.stream().mapToDouble(ingredient -> Math.ceil(ingredient.quantity() / ingredient.product().unitsPerProduct()) * ingredient.product().price()).sum();
    }

    public Double servingPrice() {
        return totalPrice() / servings();
    }

    public Double servingPrice(double servings) {
        return totalPrice() / servings;
    }

    public Double servingPurchasePrice() {
        return purchasePrice() / servings();
    }

    public Double servingPurchasePrice(double servings) {
        return purchasePrice() / servings;
    }
}
