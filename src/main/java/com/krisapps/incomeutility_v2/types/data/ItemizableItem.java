package com.krisapps.incomeutility_v2.types.data;

import com.krisapps.incomeutility_v2.types.pricer.Product;

public record ItemizableItem(
        String itemName,
        double itemPrice,
        int pricerProductId
) {
    public static ItemizableItem of(String itemName, double itemPrice) {
        return new ItemizableItem(itemName, itemPrice, -1);
    }

    public static ItemizableItem of(Product pricerProduct) {
        return new ItemizableItem(pricerProduct.name(), pricerProduct.price(), pricerProduct.id());
    }

    public boolean isPricerProduct() {
        return pricerProductId != -1;
    }
}
