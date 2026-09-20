package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.types.pricer.Product;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.util.ArrayList;
import java.util.List;

public class ProductPickerDialog extends IncomeUtilityDialog<Product> {

    private final ArrayList<Product> products = new ArrayList<>();

    public ProductPickerDialog(String title, List<Product> products) {
        super("product-picker.fxml", title, "overview_96.png");

        getDialogPane().getButtonTypes().setAll(
                new ButtonType("Confirm", ButtonBar.ButtonData.APPLY),
                new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        this.products.addAll(products);
    }
}
