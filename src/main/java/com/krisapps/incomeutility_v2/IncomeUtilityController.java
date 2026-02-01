package com.krisapps.incomeutility_v2;

import com.krisapps.incomeutility_v2.subutilities.SubUtilityType;
import javafx.fxml.FXML;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class IncomeUtilityController {

    public final UtilityManager utilities = UtilityManager.create();
    public final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(4);

    @FXML
    private VBox root;

    @FXML
    private TilePane utilitiesView;

    @FXML
    private VBox mimoCell;

    @FXML
    private VBox pricerCell;

    @FXML
    private VBox subscriptionsCell;

    @FXML
    public void initialize() {
        IncomeUtilityApplication.updateTitle("Starting application...", true);

        registerHandlers();

        IncomeUtilityApplication.updateTitle("Dashboard", false);
    }

    public void registerHandlers() {
        mimoCell.setOnMouseClicked((e) -> {
            if (!e.isShiftDown()) {
                utilities.openUtility(SubUtilityType.MONEY_IN_MONEY_OUT);
            } else {
                utilities.focusAll(SubUtilityType.MONEY_IN_MONEY_OUT);
            }
        });

    }
}
