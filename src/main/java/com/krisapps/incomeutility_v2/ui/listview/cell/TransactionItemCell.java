package com.krisapps.incomeutility_v2.ui.listview.cell;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.types.data.ItemizableItem;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.util.Formatting;
import com.krisapps.incomeutility_v2.util.PopupManager;
import com.krisapps.incomeutility_v2.util.misc.Formats;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TransactionItemCell extends ListCell<ItemizableItem> {
    @FXML
    private VBox rootPane;

    @FXML
    private Label productNameLabel;

    @FXML
    private Label priceLabel;

    @FXML
    private HBox itemInfoPanel;

    @FXML
    private HBox itemSetupPanel;

    @FXML
    private TextField nameField;

    @FXML
    private TextField priceField;

    @FXML
    private Button saveButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    private final CurrencyConfig currencyConfig;
    private boolean isBeingEdited = false;
    private final BiConsumer<ItemizableItem, ItemizableItem> onItemUpdateRequest;
    private final Consumer<ItemizableItem> onItemDeleteRequest;

    public TransactionItemCell(CurrencyConfig currencyConfig, BiConsumer<ItemizableItem, ItemizableItem> onItemUpdateRequest, Consumer<ItemizableItem> onItemDeleteRequest) {
        this.currencyConfig = currencyConfig;
        this.onItemUpdateRequest = onItemUpdateRequest;
        this.onItemDeleteRequest = onItemDeleteRequest;

        loadFXML();
    }

    private void loadFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/ui/itemize_item_cell.fxml"));
            loader.setController(this);
            rootPane = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public VBox getRootPane() {
        return rootPane;
    }

    @Override
    public void updateItem(ItemizableItem item, boolean empty) {
        super.updateItem(item, empty);
        itemInfoPanel.managedProperty().bind(itemInfoPanel.visibleProperty());
        itemSetupPanel.managedProperty().bind(itemSetupPanel.visibleProperty());
        priceField.setTextFormatter(new TextFormatter<>(Formats.NUMBERS_ONLY_FORMATTER));

        productNameLabel.setStyle("-fx-text-fill: black");
        priceLabel.setStyle("-fx-text-fill: black");

        saveButton.setOnAction(_ -> {
            if (priceField.getText().isEmpty() || nameField.getText().isEmpty()) {
                PopupManager.showPopup("Missing information", "Please make sure to provide a name and a price to the item you're trying to save.", Alert.AlertType.ERROR);
                return;
            }

            onItemUpdateRequest.accept(item, new ItemizableItem(item.relationId(), item.transactionId(), item.productId(), nameField.getText().trim(), Double.parseDouble(priceField.getText().trim())));
            updateItem(item, false);
        });

        isBeingEdited = isBeingEdited || (item == null || item.isEmpty());

        editButton.setOnAction(_ -> {
            isBeingEdited = true;
            nameField.setText(item == null ? "" : item.name());
            priceField.setText(item == null ? "" : item.price().toString());

            updateItem(item, empty);
        });

        deleteButton.setOnAction(_ -> {
            onItemDeleteRequest.accept(item);
        });

        itemInfoPanel.setVisible(!isBeingEdited);
        itemSetupPanel.setVisible(isBeingEdited);

        if (item == null || empty) {
            setText(null);
            setGraphic(null);
            isBeingEdited = false;
        } else {
            setText(null);
            setGraphic(rootPane);
            if (!item.isEmpty()) {
                productNameLabel.setText(item.name());
                priceLabel.setText(Formatting.formatMoney(item.price(), currencyConfig));
            }

            if (item.isPricerProduct()) {
                editButton.setVisible(false);
                editButton.setManaged(false);
            }

            if (isBeingEdited) {
                nameField.requestFocus();
            }
            rootPane.prefWidthProperty().bind(getListView().widthProperty().subtract(20));
        }
    }
}
