package com.krisapps.incomeutility_v2.subutilities.pricer;

import com.krisapps.incomeutility_v2.dialogs.AddProductDialog;
import com.krisapps.incomeutility_v2.dialogs.CopyTextDialog;
import com.krisapps.incomeutility_v2.dialogs.ShoppingListGeneratorDialog;
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

import java.util.LinkedList;
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
    private Label cartTotalLabel;

    @FXML
    private Label cartPerUnitOfTimeLabel;

    @FXML
    private Label cartTotalOverDurationLabel;

    @FXML
    private Label cartTotalOverDurationTitle;

    @FXML
    private Spinner<Double> cartDurationSpinner;

    @FXML
    private Button backButton;

    @FXML
    private Button createButton;

    @FXML
    private Button generateShoppingListButton;

    @FXML
    private Spinner<Double> durationSpinner;

    @FXML
    private ListView<Product> existingProductList;

    @FXML
    private ListView<Product> pickableProductList;

    @FXML
    private ListView<Product> cartList;

    private SubUtility utility;
    private CurrencyConfig currencyConfig;
    private final DataManager dataman = DataManager.getInstance();

    private final LinkedList<Product> cart = new LinkedList<>();

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

        existingProductList.setCellFactory(new ProductCellFactory(_ -> {/* Adding is not supported for this listview */}, product -> {
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
        }, currencyConfig, false, true, true));

        pickableProductList.setCellFactory(new SimpleProductCellFactory(product -> {
            cart.add(product);
            refreshCartList();
        }, currencyConfig, true));

        cartList.setCellFactory(new ProductCellFactory((product) -> {
            // Adding is not supported for this ListView.
        }, (product) -> {
            // Editing is not supported for this ListView.
        }, (product) -> {
            cart.remove(product);
            refreshCartList();
        }, currencyConfig, false, false, true));

        existingProductList.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null) {
                pricerPerUnitLabel.setText(Formatting.formatMoneyWithImprecision(newValue.pricePerUnit(), currencyConfig));
                pricePerDayOfUseLabel.setText(Formatting.formatMoneyWithImprecision(newValue.pricePerDay(), currencyConfig));
            }
        }));

        cartDurationSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.5d, Double.MAX_VALUE, 1.0d, 0.5d));
        cartDurationSpinner.valueProperty().addListener((obs, old, val) -> {
            if (val == null) return;
            refreshCartStats();
        });

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

        generateShoppingListButton.setOnAction(_ -> {
            ShoppingListGeneratorDialog dialog = new ShoppingListGeneratorDialog(cart, currencyConfig);
            Optional<String> list = dialog.showAndWait();
            if (list.isPresent()) {
                CopyTextDialog textDialog = new CopyTextDialog("Shopping list");
                textDialog.setPrimaryLabel("Generated shopping dialog");
                textDialog.setContent(list.get());
                textDialog.showAndWait();
            }
        });

        refreshUI();
    }

    private void refreshCartStats() {
        cartTotalLabel.setText(
                Formatting.formatMoney(
                        cart.stream().mapToDouble(Product::price).sum(),
                        currencyConfig
                )
        );

        cartPerUnitOfTimeLabel.setText(
                Formatting.formatMoney(
                        cart.stream().mapToDouble(Product::pricePerDay).sum(),
                        currencyConfig
                )
        );

        cartTotalOverDurationTitle.setText(String.format("Cart total per day (over %s day%s)", cartDurationSpinner.getValue() == 0.5 ? "half a" : Formatting.formatDouble(cartDurationSpinner.getValue()), cartDurationSpinner.getValue() == 0.5 || cartDurationSpinner.getValue() == 1 ? "" : "s"));
        cartTotalOverDurationLabel.setText(
                Formatting.formatMoney(
                        cart.stream().mapToDouble(Product::price).sum() / cartDurationSpinner.getValue(),
                        currencyConfig
                )
        );
    }

    private void refreshCartList() {
        cartList.getItems().clear();
        cartList.getItems().setAll(cart);
        refreshCartStats();
    }

    private void refreshExistingProductList() {
        existingProductList.getItems().clear();
        existingProductList.getItems().setAll(DataManager.getInstance().getProducts());
    }

    private void refreshPickableProductList() {
        pickableProductList.getItems().clear();
        pickableProductList.getItems().setAll(DataManager.getInstance().getProducts());
    }

    private void refreshUI() {
        refreshCartList();
        refreshExistingProductList();
        refreshPickableProductList();
    }
}
