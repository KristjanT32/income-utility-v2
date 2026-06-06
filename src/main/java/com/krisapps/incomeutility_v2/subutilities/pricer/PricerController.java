package com.krisapps.incomeutility_v2.subutilities.pricer;

import com.krisapps.incomeutility_v2.dialogs.AddProductDialog;
import com.krisapps.incomeutility_v2.subutilities.SubUtility;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityController;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.pricer.Product;
import com.krisapps.incomeutility_v2.ui.listview.ProductCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.SimpleProductCellFactory;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.Formatting;
import com.krisapps.incomeutility_v2.util.PopupManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.text.DecimalFormat;
import java.util.Optional;

public class PricerController extends SubUtilityController {

    @FXML
    private Label pricerPerUnitLabel;

    @FXML
    private Label pricePerDayOfUseLabel;

    @FXML
    private TextField nameBox;

    @FXML
    private TextField priceBox;

    @FXML
    private TextField amountBox;

    @FXML
    private TextField unitBox;

    @FXML
    private Button backButton;

    @FXML
    private Button createButton;

    @FXML
    private Spinner<Double> durationSpinner;

    @FXML
    private ListView<Product> existingProductList;

    @FXML
    private ListView<Product> pickableProductList;

    private SubUtility utility;
    private CurrencyConfig currencyConfig;
    private final DataManager dataman = DataManager.getInstance();

    @Override
    public void onStartup(SubUtility utility) {
        this.utility = utility;
    }

    @Override
    public void onShutdown() {
    }

    @Override
    public void onPromptCommand(String command, String[] args) {
        switch (command) {
            case "refresh" -> {
                refreshUI();
            }
        }
    }

    @FXML
    public void initialize() {
        currencyConfig = DataManager.getInstance().getPricerCurrencyConfiguration();
        initUI();
    }

    public void initUI() {
        backButton.setOnAction(_ -> utility.stop());

        existingProductList.setCellFactory(new ProductCellFactory(product -> {
            // Adding is not supported for this listview
        }, product -> {
            dataman.updateProduct(product.id(), product);
            refreshUI();
        }, product -> {
            Optional<ButtonType> response = PopupManager.showConfirmation("Delete product?", "Are you sure you wish to delete " + product.name() + "?\n\nThis action cannot be undone.",
                    new ButtonType("Delete", ButtonBar.ButtonData.APPLY),
                    new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
            );

            if (response.isPresent()) {
                if (response.get().getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                    dataman.deleteProduct(product.id());
                    refreshUI();
                }
            }
        }, currencyConfig, false, true));

        pickableProductList.setCellFactory(new SimpleProductCellFactory(_ -> {
            // TODO: Implement
        }, currencyConfig, true));

        existingProductList.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null) {
                boolean isImprecisePerUnit = !new DecimalFormat("0.00").format(newValue.pricePerUnit()).equals(String.valueOf(newValue.pricePerUnit()));
                boolean isImprecisePerDay = !new DecimalFormat("0.00").format(newValue.pricePerDay()).equals(String.valueOf(newValue.pricePerDay()));

                pricerPerUnitLabel.setText((isImprecisePerUnit ? " ~ " : "") + Formatting.formatMoney(newValue.pricePerUnit(), currencyConfig));
                pricePerDayOfUseLabel.setText((isImprecisePerDay ? " ~ " : "") + Formatting.formatMoney(newValue.pricePerDay(), currencyConfig));
            }
        }));

        Label l = new Label("No products have been added yet.");
        l.getStyleClass().add("medium-label");
        existingProductList.setPlaceholder(l);
        pickableProductList.setPlaceholder(l);

        createButton.setOnAction(_ -> {
            AddProductDialog dialog = new AddProductDialog();
            Optional<Product> product = dialog.showAndWait();

            if (product.isPresent()) {
                DataManager.getInstance().addProduct(product.get());
                refreshUI();
            }
        });

        refreshUI();
    }

    private void refreshUI() {
        existingProductList.getItems().clear();
        pickableProductList.getItems().clear();

        existingProductList.getItems().setAll(DataManager.getInstance().getProducts());
        pickableProductList.getItems().setAll(DataManager.getInstance().getProducts());
    }
}
