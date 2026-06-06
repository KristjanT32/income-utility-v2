package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.types.pricer.Product;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.PopupManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import static com.krisapps.incomeutility_v2.util.Formatting.isNumber;

public class EditProductDialog extends IncomeUtilityDialog<Product> {

    @FXML
    private TextField nameField;

    @FXML
    private TextField priceField;

    @FXML
    private TextField quantityField;

    @FXML
    private TextField singularUnitField;

    @FXML
    private TextField pluralUnitField;

    @FXML
    private TextField smallestUnitField;

    @FXML
    private Spinner<Double> useDurationSpinner;

    boolean preventClose = false;

    public EditProductDialog(Product original) {
        super("edit-product.fxml", "Edit product information", "overview_96.png");

        getDialogPane().getButtonTypes().addAll(
                new ButtonType("Apply changes", ButtonBar.ButtonData.APPLY),
                new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        setOnCloseRequest((request) -> {
            if (preventClose) {
                request.consume();
                preventClose = false;
                DataManager.log("Denying close request to show error popup", this.getClass().getSimpleName());
            }
        });

        useDurationSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(original.smallestUnit(), Double.MAX_VALUE, original.durationOfUse(), original.smallestUnit()));
        nameField.setText(original.name());
        priceField.setText(String.valueOf(original.price()));
        quantityField.setText(String.valueOf(original.unitsPerProduct()));
        singularUnitField.setText(original.unitSingular());
        pluralUnitField.setText(original.unitPlural());
        smallestUnitField.setText(String.valueOf(original.smallestUnit()));


        setResultConverter((response) -> {
            if (response.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                if (nameField.getText().isEmpty()) {
                    preventClose = true;
                    PopupManager.showPopup("Incomplete data!", "Please make sure to add a product name and try again.", Alert.AlertType.ERROR);
                    return null;
                }

                if (priceField.getText().isEmpty() || !isNumber(priceField.getText())) {
                    preventClose = true;
                    PopupManager.showPopup(priceField.getText().isEmpty() ? "Incomplete data!" : "Invalid price!", priceField.getText().isEmpty() ? "Please make sure to add a price and try again." : "The supplied price is not valid.", Alert.AlertType.ERROR);
                    return null;
                }

                if (quantityField.getText().isEmpty() || !isNumber(quantityField.getText())) {
                    preventClose = true;
                    PopupManager.showPopup(quantityField.getText().isEmpty() ? "Incomplete data!" : "Invalid quantity!", quantityField.getText().isEmpty() ? "Please make sure to add a quantity and try again." : "The supplied quantity is not valid.", Alert.AlertType.ERROR);
                    return null;
                }

                Product out = new Product(
                        original.id(),
                        nameField.getText().isEmpty() ? "New Product" : nameField.getText(),
                        !isNumber(priceField.getText()) ? 0.0d : Double.parseDouble(priceField.getText()),
                        useDurationSpinner.getValue() == null ? 1 : useDurationSpinner.getValue(),
                        !isNumber(quantityField.getText()) ? 1.0d : Double.parseDouble(quantityField.getText()),
                        !isNumber(smallestUnitField.getText()) ? 0.5d : Double.parseDouble(smallestUnitField.getText()),
                        singularUnitField.getText().isEmpty() ? "gram" : singularUnitField.getText().trim(),
                        pluralUnitField.getText().isEmpty() ? "grams" : pluralUnitField.getText()
                );

                if (!out.equals(original)) {
                    return out;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        });
    }
}
