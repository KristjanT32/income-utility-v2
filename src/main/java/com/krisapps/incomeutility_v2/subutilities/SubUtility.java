package com.krisapps.incomeutility_v2.subutilities;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.util.DataManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Consumer;

public abstract class SubUtility {
    private String id;
    private final SubUtilityType type;
    private final String layoutPath;
    private final SubUtilityController controller;

    private final int minWidth;
    private final int minHeight;
    private final boolean allowResize;

    private HBox commandPrompt;
    private TextField commandPromptField;
    private Label commandPromptLabel;


    private String processId;
    private final String utilityName;
    private String iconFilePath = "income_utility.png";
    private Consumer<String> onCloseCallback = (id) -> {
    };

    private Stage instance;

    public Stage getInstance() {
        return instance;
    }

    public SubUtility(SubUtilityType type, String layoutPath, @Nullable String iconFilePath, SubUtilityController controller, int minWidth, int minHeight, boolean allowResize) {
        this.type = type;
        this.layoutPath = layoutPath;
        this.controller = controller;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.allowResize = allowResize;
        this.utilityName = type.getDisplayName();
        this.iconFilePath = iconFilePath == null ? this.iconFilePath : iconFilePath;
    }

    public SubUtility(SubUtilityType type, String layoutPath, @Nullable String iconFilePath, SubUtilityController controller, int minWidth, int minHeight, boolean allowResize, String processId) {
        this.type = type;
        this.layoutPath = layoutPath;
        this.controller = controller;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.allowResize = allowResize;
        this.processId = processId;
        this.utilityName = type.getDisplayName();
        this.iconFilePath = iconFilePath == null ? this.iconFilePath : iconFilePath;
    }

    public SubUtility start(String processId) throws IOException {
        Stage window = new Stage();
        FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource(layoutPath));
        loader.setController(this.controller);

        Scene s = new Scene(loader.load());
        window.setScene(s);
        window.setMinWidth(minWidth);
        window.setMinHeight(minHeight);
        window.setResizable(allowResize);
        window.setTitle(type.getDisplayName());
        window.getIcons().add(new Image(IncomeUtilityApplication.class.getResource("icons/" + iconFilePath).toExternalForm()));
        window.setOnCloseRequest((_) -> {
            stop();
        });

        // If the utility has a command prompt
        if (loader.getNamespace().get("commandPrompt") != null) {
            log("Command prompt found, setting up...");
            commandPrompt = (HBox) loader.getNamespace().get("commandPrompt");
            commandPromptField = (TextField) loader.getNamespace().get("commandPromptField");
            commandPromptLabel = (Label) loader.getNamespace().get("commandPromptLabel");

            commandPrompt.managedProperty().bind(commandPrompt.visibleProperty());
            commandPromptLabel.setText(utilityName.toLowerCase().replaceAll("[,.;]", "").trim().replaceAll(" ", "-"));
            commandPromptField.setText("");
            commandPromptField.setOnAction((action) -> {
                String command = commandPromptField.getText();
                if (!command.isBlank()) {
                    String[] split = command.split(" ");

                    if (split.length > 1) {
                        String[] args = new String[split.length - 1];
                        System.arraycopy(split, 1, args, 0, split.length - 1);

                        controller.onPromptCommand(split[0], args);
                    } else {
                        controller.onPromptCommand(command, new String[0]);
                    }
                }

                commandPrompt.setVisible(false);
            });

            window.getScene().setOnKeyPressed((keyEvent) -> {
                if (keyEvent.getCode().equals(KeyCode.F1)) {
                    commandPrompt.setVisible(!commandPrompt.isVisible());
                    commandPromptField.setText("");
                    if (commandPrompt.isVisible()) {
                        commandPromptField.requestFocus();
                    }
                }
                if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                    if (commandPrompt.isVisible()) {
                        commandPrompt.setVisible(false);
                    }
                }
            });

            commandPrompt.setVisible(false);
        }

        try {
            this.id = processId;
            this.instance = window;
            this.controller.onStartup(this);
            this.instance.show();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return this;
    }

    public void stop() {
        if (this.instance != null) {
            Platform.runLater(() -> {
                this.controller.onShutdown();
                this.instance.close();
                onCloseCallback.accept(this.id);
            });
        }
    }

    public void focusWindow() {
        this.instance.toFront();
    }

    public void setOnCloseCallback(Consumer<String> onCloseCallback) {
        this.onCloseCallback = onCloseCallback;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getName() {
        return type.getDisplayName();
    }

    public SubUtilityType getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public void log(String msg) {
        DataManager.log(String.format("[%s/%s] %s", utilityName, processId, msg));
    }
}
