package com.krisapps.incomeutility_v2.subutilities.pricer;

import com.krisapps.incomeutility_v2.dialogs.AddProductDialog;
import com.krisapps.incomeutility_v2.dialogs.DishDetailsDialog;
import com.krisapps.incomeutility_v2.dialogs.IngredientPickerDialog;
import com.krisapps.incomeutility_v2.dialogs.ShoppingListGeneratorDialog;
import com.krisapps.incomeutility_v2.dialogs.generic.CopyTextDialog;
import com.krisapps.incomeutility_v2.dialogs.generic.InputDialog;
import com.krisapps.incomeutility_v2.subutilities.SubUtility;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityController;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.pricer.Dish;
import com.krisapps.incomeutility_v2.types.pricer.DishIngredient;
import com.krisapps.incomeutility_v2.types.pricer.Product;
import com.krisapps.incomeutility_v2.ui.listview.DishCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.IngredientCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.ProductCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.SimpleProductCellFactory;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.Formatting;
import com.krisapps.incomeutility_v2.util.PopupManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Optional;

public class PricerController extends SubUtilityController {

    private enum DishEditorState {
        SELECT_DISH,
        DISH_CREATION,
        DISH_EDITING
    }

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

    @FXML
    private ListView<DishIngredient> dishIngredientList;

    @FXML
    private ListView<Dish> dishList;

    @FXML
    private Button saveDishButton;

    @FXML
    private Button applyDishChangesButton;

    @FXML
    private Button closeDishButton;

    @FXML
    private Button addIngredientButton;

    @FXML
    private Button createDishButton;

    @FXML
    private Spinner<Double> dishServingSpinner;

    @FXML
    private Label dishTotalLabel;

    @FXML
    private Label dishProductsTotalLabel;

    @FXML
    private Label servingPriceLabel;

    @FXML
    private Label dishNameLabel;

    @FXML
    private VBox dishEditor;

    @FXML
    private VBox dishSelection;

    private SubUtility utility;
    private CurrencyConfig currencyConfig;
    private final DataManager dataman = DataManager.getInstance();

    private DishEditorState dishEditorState = DishEditorState.SELECT_DISH;
    private boolean editorHasChanges = false;
    private Dish currentEditorDish = null;

    private final LinkedList<Product> cart = new LinkedList<>();

    @Override
    public void onStartup(SubUtility utility) {
        this.utility = utility;
        this.utility.getInstance().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode().equals(KeyCode.ESCAPE)) {
                closeEditor();
            }
        });
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

        Label dishPlaceholder = new Label("No dishes have been created yet.");
        dishPlaceholder.getStyleClass().add("medium-label");
        dishList.setPlaceholder(dishPlaceholder);

        Label ingredientsPlaceholder = new Label("No ingredients have been added yet.");
        ingredientsPlaceholder.getStyleClass().add("medium-label");
        dishIngredientList.setPlaceholder(ingredientsPlaceholder);

        createButton.setOnAction(_ -> {
            AddProductDialog dialog = new AddProductDialog();
            Optional<Product> product = dialog.showAndWait();

            if (product.isPresent()) {
                DataManager.getInstance().addProduct(product.get());
                refreshUI();
            }
        });

        generateShoppingListButton.setOnAction(_ -> {
            if (cart.isEmpty()) {
                PopupManager.showPopup("Shopping cart is empty", "Add some products to your cart first, to generate a shopping list.", Alert.AlertType.ERROR);
                return;
            }

            ShoppingListGeneratorDialog dialog = new ShoppingListGeneratorDialog(cart, currencyConfig);
            Optional<String> list = dialog.showAndWait();
            if (list.isPresent()) {
                CopyTextDialog textDialog = new CopyTextDialog("Shopping list");
                textDialog.setPrimaryLabel("Generated shopping dialog");
                textDialog.setContent(list.get());
                textDialog.showAndWait();
            }
        });

        createDishButton.setOnAction(_ -> {
            InputDialog nameDialog = new InputDialog("New dish");
            nameDialog.setPrimaryLabel("What would you like to call your dish?");
            nameDialog.setDescription("To create a new dish, please supply the name using the field below.");
            nameDialog.setPrompt("New dish...");
            Optional<String> name = nameDialog.showAndWait();
            if (name.isPresent() && !name.get().isEmpty()) {
                dishEditorState = DishEditorState.DISH_CREATION;
                currentEditorDish = new Dish(
                        -1,
                        name.get(),
                        1,
                        new ArrayList<>()
                );
            } else {
                PopupManager.showPopup("Name required", "A dish name is required to open the dish editor.", Alert.AlertType.ERROR);
            }
            refreshUI();
        });

        addIngredientButton.setOnAction(_ -> {
            IngredientPickerDialog dialog = new IngredientPickerDialog(
                    dataman.getProducts().stream().filter(p -> currentEditorDish.ingredients().stream().map(DishIngredient::product).distinct().noneMatch(existing -> existing.id() == p.id())).toList(),
                    currentEditorDish != null ? currentEditorDish.id() : -1
            );
            Optional<DishIngredient> ingredient = dialog.showAndWait();

            if (ingredient.isPresent()) {
                currentEditorDish.ingredients().add(ingredient.get());
                editorHasChanges = true;
                refreshDishEditor();
            }
        });

        dishEditor.managedProperty().bind(dishEditor.visibleProperty());
        dishSelection.managedProperty().bind(dishSelection.visibleProperty());

        saveDishButton.managedProperty().bind(saveDishButton.visibleProperty());
        applyDishChangesButton.managedProperty().bind(applyDishChangesButton.visibleProperty());
        closeDishButton.managedProperty().bind(closeDishButton.visibleProperty());

        dishList.setCellFactory(new DishCellFactory(dish -> {
            DishDetailsDialog detailsDialog = new DishDetailsDialog(dish, currencyConfig);
            detailsDialog.showAndWait();
        }, dish -> {
            resetDishEditor();

            currentEditorDish = dish;
            dishEditorState = DishEditorState.DISH_EDITING;
            refreshDishEditor();
        }, dish -> {
            Optional<ButtonType> choice = PopupManager.showConfirmation("Delete dish?", "Are you sure you wish to delete '" + dish.name() + "'?\n\nThis cannot be undone.",
                    new ButtonType("Yes, delete", ButtonBar.ButtonData.APPLY), new ButtonType("No, leave it be", ButtonBar.ButtonData.CANCEL_CLOSE)
            );

            choice.ifPresent(c -> {
                if (c.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                    dataman.deleteDish(dish.id());
                    refreshUI();
                }
            });
        }, currencyConfig, true, true, true));
        dishList.getItems().setAll(dataman.getDishes());

        dishServingSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(1, Double.MAX_VALUE, currentEditorDish == null ? 1.0d : currentEditorDish.servings(), 1));
        dishServingSpinner.valueProperty().addListener((obs, old, val) -> {
            editorHasChanges = true;
            refreshDishEditor();
        });

        dishIngredientList.setCellFactory(new IngredientCellFactory((product, quantity) -> {
            currentEditorDish.ingredients().replaceAll(ing -> {
                if (ing.product().id() == product.id()) {
                    editorHasChanges = true;
                    return new DishIngredient(
                            ing.relationId(),
                            ing.dishId(),
                            product,
                            quantity
                    );
                } else {
                    return ing;
                }
            });
            refreshDishEditor();
        }, (product) -> {
            currentEditorDish.ingredients().removeIf(ing -> ing.product().id() == product.id());
            editorHasChanges = true;
            refreshDishEditor();
        }, currencyConfig, false));

        Tooltip servingPriceTip = new Tooltip("The price per a single serving of this dish.\nThe price in parentheses is the cart total per serving.");
        servingPriceLabel.setTooltip(servingPriceTip);

        saveDishButton.setOnAction(_ -> {
            if (!dishEditorState.equals(DishEditorState.DISH_CREATION)) {
                DataManager.log("'Save' button clicked during invalid state (actual state: " + dishEditorState.name() + ", expected: DISH_CREATION)", "Dish Editor");
                return;
            }

            Dish outputDish = new Dish(
                    currentEditorDish.id(),
                    currentEditorDish.name(),
                    dishServingSpinner.getValue(),
                    currentEditorDish.ingredients()
            );

            dataman.addDish(outputDish);
            DataManager.log("New dish created: " + currentEditorDish.name(), "Dish Editor");

            resetDishEditor();
            dishEditorState = DishEditorState.SELECT_DISH;
            refreshDishEditor();
        });

        applyDishChangesButton.setOnAction(_ -> {
            if (!dishEditorState.equals(DishEditorState.DISH_EDITING)) {
                DataManager.log("'Apply' button clicked during invalid state (actual state: " + dishEditorState.name() + ", expected: DISH_EDITING)", "Dish Editor");
                return;
            }

            if (editorHasChanges) {
                Dish outputDish = new Dish(
                        currentEditorDish.id(),
                        currentEditorDish.name(),
                        dishServingSpinner.getValue(),
                        currentEditorDish.ingredients()
                );

                dataman.updateDish(currentEditorDish.id(), outputDish);
                for (DishIngredient ingredient : currentEditorDish.ingredients()) {
                    dataman.updateDishIngredient(ingredient.relationId(), ingredient);
                }

                DataManager.log("Dish data updated: " + currentEditorDish.name() + " (id: " + currentEditorDish.id() + ")", "Dish Editor");
            } else {
                DataManager.log("Editor closed with no changes.", "Dish Editor");
            }

            resetDishEditor();
            dishEditorState = DishEditorState.SELECT_DISH;
            refreshDishEditor();
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

    private void refreshDishEditor() {
        dishEditor.setVisible(dishEditorState.equals(DishEditorState.DISH_CREATION) || dishEditorState.equals(DishEditorState.DISH_EDITING));
        dishSelection.setVisible(dishEditorState.equals(DishEditorState.SELECT_DISH));

        saveDishButton.setVisible(dishEditorState.equals(DishEditorState.DISH_CREATION));
        applyDishChangesButton.setVisible(dishEditorState.equals(DishEditorState.DISH_EDITING));

        closeDishButton.setText(dishEditorState.equals(DishEditorState.DISH_CREATION) ? "Discard dish" : "Discard changes");
        closeDishButton.setOnAction(_ -> closeEditor());

        switch (dishEditorState) {
            case SELECT_DISH -> {
                dishList.getItems().clear();
                dishList.getItems().setAll(dataman.getDishes());
            }
            case DISH_CREATION, DISH_EDITING -> {
                dishIngredientList.getItems().setAll(currentEditorDish.ingredients());
                dishNameLabel.setText(currentEditorDish.name());
                double dishTotal = currentEditorDish.totalPrice();
                dishTotalLabel.setText(
                        Formatting.formatMoney(
                                dishTotal,
                                currencyConfig
                        )
                );
                double dishProductsTotal = currentEditorDish.purchasePrice();
                dishProductsTotalLabel.setText(
                        Formatting.formatMoney(
                                dishProductsTotal,
                                currencyConfig
                        )
                );
                servingPriceLabel.setText(
                        String.format("%s (%s)", Formatting.formatMoney(
                                currentEditorDish.servingPrice(dishServingSpinner.getValue()),
                                currencyConfig
                        ), Formatting.formatMoney(
                                currentEditorDish.servingPurchasePrice(dishServingSpinner.getValue()),
                                currencyConfig
                        ))
                );
            }
        }
    }

    private void closeEditor() {
        if (dishEditorState.equals(DishEditorState.DISH_CREATION)) {
            Optional<ButtonType> response = PopupManager.showConfirmation("Discard dish", "Are you sure you wish to discard this dish?\n\nThis cannot be undone.",
                    new ButtonType("Yes, discard", ButtonBar.ButtonData.APPLY),
                    new ButtonType("No, leave it be", ButtonBar.ButtonData.CANCEL_CLOSE)
            );

            if (response.isPresent()) {
                if (response.get().getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                    resetDishEditor();
                    dishEditorState = DishEditorState.SELECT_DISH;
                    refreshDishEditor();
                }
            }
        } else if (dishEditorState.equals(DishEditorState.DISH_EDITING)) {
            if (!editorHasChanges) {
                resetDishEditor();
                dishEditorState = DishEditorState.SELECT_DISH;
                refreshDishEditor();
                return;
            }

            Optional<ButtonType> response = PopupManager.showConfirmation("Discard changes?", "Are you sure you wish to discard all changes made to '" + currentEditorDish.name() + "'?\n\nThis cannot be undone.",
                    new ButtonType("Yes, discard", ButtonBar.ButtonData.APPLY),
                    new ButtonType("No, cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
            );

            if (response.isPresent()) {
                if (response.get().getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                    resetDishEditor();
                    dishEditorState = DishEditorState.SELECT_DISH;
                    refreshDishEditor();
                }
            }
        }
    }

    private void refreshUI() {
        refreshCartList();
        refreshExistingProductList();
        refreshPickableProductList();
        refreshDishEditor();
    }

    private void resetDishEditor() {
        currentEditorDish = null;
        editorHasChanges = false;
    }
}
