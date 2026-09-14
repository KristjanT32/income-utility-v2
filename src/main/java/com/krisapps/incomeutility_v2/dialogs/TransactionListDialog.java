package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.ui.listview.TransactionCellFactory;
import com.krisapps.incomeutility_v2.util.PopupManager;
import com.krisapps.incomeutility_v2.util.services.ExportService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;

import java.util.List;
import java.util.Map;

public class TransactionListDialog extends IncomeUtilityDialog<Transaction> {

    @FXML
    private ListView<Transaction> transactionList;

    @FXML
    private Label label;

    @FXML
    private Label sublabel;

    private Transaction selection = null;
    private final ExportService exporter = ExportService.getInstance();

    public TransactionListDialog(String title) {
        super("transaction-list-dialog.fxml", title, "overview_96.png");
        Platform.runLater(() -> {
            label.managedProperty().bind(label.visibleProperty());
            label.visibleProperty().bind(label.textProperty().isNotEmpty());
            sublabel.managedProperty().bind(sublabel.visibleProperty());
            sublabel.visibleProperty().bind(sublabel.textProperty().isNotEmpty());
            transactionList.setCellFactory(new TransactionCellFactory(null, _ -> {
            }, true));
        });

        transactionList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            selection = val;
        });

        setResultConverter(button -> {
            if (button.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                return selection;
            } else {
                return null;
            }
        });
    }

    public void setItems(List<Transaction> items) {
        Platform.runLater(() -> {
            transactionList.setItems(FXCollections.observableList(items));
        });
    }

    public void setLabel(String text) {
        Platform.runLater(() -> {
            label.setText(text);
        });
    }

    public void setSubLabel(String text) {
        Platform.runLater(() -> {
            sublabel.setText(text);
        });
    }

    public void exportAsCSV() {
        PopupManager.showPredefinedPopup(PopupManager.PopupType.NOT_IMPLEMENTED);
    }

    public void exportAsPlainText() {
        Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, exporter.exportAsPlainText(transactionList.getItems())));
    }

    public void exportAsExcelWorksheet() {
        PopupManager.showPredefinedPopup(PopupManager.PopupType.NOT_IMPLEMENTED);
    }
}
