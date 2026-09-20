package com.krisapps.incomeutility_v2.types.data;

import com.krisapps.incomeutility_v2.types.pricer.Product;

public record ItemizableItem(
        int relationId,
        int transactionId,
        int productId,
        String itemName,
        Double itemPrice
) {
    public static ItemizableItem ofPricerProduct(Product product, int transactionId) {
        return new ItemizableItem(-1, transactionId, product.id(), product.name(), product.price());
    }

    public static ItemizableItem ofCustomProduct(String name, Double price, int transactionId) {
        return new ItemizableItem(-1, transactionId, -1, name, price);
    }

    public boolean isPricerProduct() {
        return productId != -1;
    }
}
