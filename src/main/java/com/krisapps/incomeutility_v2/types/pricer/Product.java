package com.krisapps.incomeutility_v2.types.pricer;

public record Product(int id, String name, Double price, Double durationOfUse, Double unitsPerProduct,
                      Double smallestUnit, String unitSingular, String unitPlural) {

    public String quantityString() {
        return String.format("%s %s", unitsPerProduct, unitsPerProduct == 1 ? unitSingular : unitPlural);
    }

    public String quantityString(int quantity) {
        return String.format("%s %s", quantity, quantity == 1 ? unitSingular : unitPlural);
    }

    public double pricePerUnit() {
        return price / unitsPerProduct;
    }

    public double pricePerDay() {
        return price / durationOfUse;
    }

    public double pricePerDay(double days) {
        return price / days;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Product other)) {
            return false;
        }

        return id == other.id && name.equals(other.name) && price.equals(other.price) && durationOfUse.equals(other.durationOfUse) && unitsPerProduct.equals(other.unitsPerProduct)
                && smallestUnit.equals(other.smallestUnit) && unitSingular.equals(other.unitSingular) && unitPlural.equals(other.unitPlural);
    }
}
