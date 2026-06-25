package com.krisapps.incomeutility_v2.subutilities.pantry;

import com.krisapps.incomeutility_v2.subutilities.SubUtility;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class PantryController extends SubUtilityController {

    @FXML
    private Button backButton;

    private SubUtility utility;

    @Override
    public void onStartup(SubUtility utility) {
        this.utility = utility;
    }

    @Override
    public void onShutdown() {

    }

    @Override
    public void onPromptCommand(String command, String[] args) {

    }

    @FXML
    public void initialize() {
        backButton.setOnAction((_) -> utility.stop());
    }
}
