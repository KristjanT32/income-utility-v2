package com.krisapps.incomeutility_v2.types.data;

import com.krisapps.incomeutility_v2.types.pricer.Product;

import java.util.Objects;

public record ItemizableItem(
        int relationId,
        int transactionId,
        int productId,
        String name,
        Double price
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

    public boolean isEmpty() {
        return productId == -1 && name == null && price == null;
    }

    /**
     * Returns <code>true</code> if this item represents the same item as the supplied item.
     * For Pricer products this is true when they share an ID, for custom entries this is true when the entries share a name (case-insensitive) and price.
     *
     * @param item The other item to compare this item to.
     */
    public boolean is(ItemizableItem item) {
        if (item.isPricerProduct()) {
            return item.productId() == productId();
        } else {
            return String.valueOf(item.name()).equalsIgnoreCase(String.valueOf(name())) && Objects.equals(item.price(), price());
        }
    }
}
