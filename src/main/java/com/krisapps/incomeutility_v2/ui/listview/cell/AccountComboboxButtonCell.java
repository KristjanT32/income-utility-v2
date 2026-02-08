package com.krisapps.incomeutility_v2.ui.listview.cell;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;

public class AccountComboboxButtonCell extends ComboBoxListCell<Account> {

    @FXML
    private HBox rootPane;

    @FXML
    private Label balanceLabel;

    @FXML
    private Label nameLabel;

    @FXML
    private FontIcon typeIcon;

    public AccountComboboxButtonCell() {
        try {
            FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource("layouts/ui/account_cell_combobox.fxml"));
            loader.setController(this);
            rootPane = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateItem(Account account, boolean empty) {
        super.updateItem(account, empty);
        nameLabel.setText(!empty ? account.getName() : "");
        balanceLabel.setText(!empty ? account.formatBalance() : "");

        nameLabel.setStyle("-fx-text-fill: black");
        balanceLabel.setStyle("-fx-text-fill: black");

        if (empty || account == null) {
            setText(null);
            setGraphic(null);
        } else {
            setText(null);
            setGraphic(rootPane);
        }
    }
}
