package com.krisapps.incomeutility_v2.types.pricer;

public record DishIngredient(int relationId, int dishId, Product product, Double quantity) {

    /**
     * Returns the quantity of product represented by this DishIngredient record.
     * That is, if this DishIngredient represents 400g of ProductA (200g per unit), this will return 2,
     * as 400g corresponds to 2 units of ProductA.
     * <p>
     * The quantity is always rounded up.
     */
    public int unitsOfProduct() {
        return (int) Math.ceil(quantity / product.unitsPerProduct());
    }
}
