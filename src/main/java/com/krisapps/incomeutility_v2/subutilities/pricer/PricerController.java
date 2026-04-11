package com.krisapps.incomeutility_v2.subutilities.pricer;

import com.krisapps.incomeutility_v2.subutilities.SubUtility;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityController;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.util.DataManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;

public class PricerController extends SubUtilityController {

    @FXML
    private Label ppuLabel;

    @FXML
    private Label pputLabel;

    @FXML
    private TextField nameBox;

    @FXML
    private TextField priceBox;

    @FXML
    private TextField amountBox;

    @FXML
    private TextField unitBox;

    @FXML
    private Spinner<Double> durationSpinner;

    private SubUtility utility;

    @Override
    public void onStartup(SubUtility utility) {
        this.utility = utility;
    }

    @Override
    public void onShutdown() {

    }

    @FXML
    public void initialize() {
        initUI();
    }

    public void initUI() {
        priceBox.textProperty().addListener((_, _, _) -> {
            calculate();
        });

        amountBox.textProperty().addListener((_, _, _) -> {
            calculate();
        });

        durationSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0d, 365d, 1d, 0.5d));
        durationSpinner.valueProperty().addListener((_, _, _) -> {
            calculate();
        });
    }

    public void calculate() {
        if (priceBox.getText().isEmpty() || amountBox.getText().isEmpty()) return;

        double price = Double.parseDouble(priceBox.getText());
        double units = Double.parseDouble(amountBox.getText());
        double ppu = ((price / units) * 10000) / 10000;

        // Price per unit
        ppuLabel.setText(
                ppu < 0.01 ? "< " + DataManager.Formatting.formatMoney(ppu, CurrencyConfig.DEFAULT) : DataManager.Formatting.formatMoney(ppu, CurrencyConfig.DEFAULT)
        );

        if (durationSpinner.getValue() != 0) {

            double daysPerSingle = durationSpinner.getValue();
            double pput = ((price / daysPerSingle) * 10000) / 10000;

            pputLabel.setText(pput < 0.01 ? "< " + DataManager.Formatting.formatMoney(pput, CurrencyConfig.DEFAULT) : DataManager.Formatting.formatMoney(pput, CurrencyConfig.DEFAULT));
        }
    }
}
