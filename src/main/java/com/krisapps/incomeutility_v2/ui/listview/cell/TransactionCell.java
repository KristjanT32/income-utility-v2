package com.krisapps.incomeutility_v2.ui.listview.cell;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.dialogs.TransactionDetailsDialog;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.transaction.TransactionCategory;
import com.krisapps.incomeutility_v2.types.transaction.TransactionType;
import com.krisapps.incomeutility_v2.util.DataManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Paint;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.Optional;

public class TransactionCell extends ListCell<Transaction> {

    @FXML
    private HBox rootPane;

    @FXML
    private Button detailsButton;

    @FXML
    private Label amountLabel;

    @FXML
    private Label categoryLabel;

    @FXML
    private Label commentLabel;

    @FXML
    private FontIcon typeIcon;

    private final Account parent;

    public TransactionCell(Account parent) {
        try {
            FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/ui/transaction_cell.fxml"));
            loader.setController(this);
            rootPane = loader.load();
            this.parent = parent;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void updateItem(Transaction transaction, boolean empty) {
        super.updateItem(transaction, empty);

        if (empty || transaction == null) {
            setText(null);
            setGraphic(null);
            return;
        } else {
            setText(null);
            setGraphic(rootPane);
        }

        detailsButton.setStyle("-fx-text-fill: black");
        amountLabel.setStyle("-fx-text-fill: black");
        categoryLabel.setStyle("-fx-text-fill: black");
        commentLabel.setStyle("-fx-text-fill: black");

        detailsButton.setOnAction((ev) -> {
            TransactionDetailsDialog detailsDialog = new TransactionDetailsDialog(transaction, parent);
            detailsDialog.showAndWait();
        });

        commentLabel.visibleProperty().addListener((obs, _, now) -> {
            commentLabel.setManaged(now);
            rootPane.requestLayout();
        });
        commentLabel.textProperty().addListener((obs, _, now) -> {
            if (now == null) {
                commentLabel.setVisible(false);
            } else {
                if (!now.isEmpty()) {
                    commentLabel.setVisible(true);
                } else {
                    commentLabel.setVisible(false);
                }
            }
        });

        detailsButton.setVisible(isSelected());
        amountLabel.setText(transaction.formatAmount(DataManager.getInstance(), true));

        rootPane.getStyleClass().removeAll("outflow", "inflow", "transfer");
        if (transaction.getType().equals(TransactionType.TRANSFER)) {
            if (parent.getId().equals(transaction.getTargetAccountId())) {

                // If current account is the target
                Optional<Account> account = DataManager.getInstance().getAccount(transaction.getSourceAccountId());

                rootPane.getStyleClass().add("inflow");
                categoryLabel.setText("Transferred from " + (account.isPresent() ? account.get().getName() : "Unknown account") + " (" + (transaction.getCategory() == TransactionCategory.CUSTOM
                        ? DataManager.Formatting.capitalize(transaction.getCustomCategory())
                        : DataManager.Formatting.capitalize(transaction.getCategory().toString())) + ")");
                commentLabel.setText(transaction.getComment());
            } else if (parent.getId().equals(transaction.getSourceAccountId())) {

                // If current account is the source
                Optional<Account> account = DataManager.getInstance().getAccount(transaction.getTargetAccountId());

                rootPane.getStyleClass().add("outflow");
                categoryLabel.setText("Transferred to " + (account.isPresent() ? account.get().getName() : "Unknown account") + " (" + (transaction.getCategory() == TransactionCategory.CUSTOM
                        ? DataManager.Formatting.capitalize(transaction.getCustomCategory())
                        : DataManager.Formatting.capitalize(transaction.getCategory().toString())) + ")");
                commentLabel.setText(transaction.getComment());
            }
        } else {
            rootPane.getStyleClass().add(transaction.getType() == TransactionType.WITHDRAWAL ? "outflow" : transaction.getType() == TransactionType.DEPOSIT ? "inflow" : "transfer");
            categoryLabel.setText(
                    transaction.getCategory() == TransactionCategory.CUSTOM
                            ? DataManager.Formatting.capitalize(transaction.getCustomCategory())
                            : DataManager.Formatting.capitalize(transaction.getCategory().toString())
            );
            commentLabel.setText(transaction.getComment().trim());
        }

        switch (transaction.getType()) {
            case DEPOSIT -> {
                typeIcon.setIconLiteral("fltfal-arrow-right-20");
                typeIcon.setIconColor(Paint.valueOf("#00bc22"));
            }
            case WITHDRAWAL -> {
                typeIcon.setIconLiteral("fltfal-arrow-left-20");
                typeIcon.setIconColor(Paint.valueOf("#bf0000"));
            }
            case TRANSFER -> {
                typeIcon.setIconLiteral("fltfal-arrow-swap-20");
                typeIcon.setIconColor(Paint.valueOf("#3b42ff"));
            }
        }
    }
}

