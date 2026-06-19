package com.krisapps.incomeutility_v2.subutilities;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.util.Logging;
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
import java.nio.file.InvalidPathException;
import java.util.function.Consumer;

public abstract class SubUtility {
    private String id;
    private final SubUtilityType type;
    private final String layoutPath;
    private final SubUtilityController controller;

    private final Logging log = Logging.getInstance();

    private final int minWidth;
    private final int minHeight;
    private final boolean allowResize;

    private HBox commandPrompt;
    private TextField commandPromptField;


    private String processId;
    private final String utilityName;
    private String iconFilePath = "income_utility.png";
    private Consumer<String> onCloseCallback = (_) -> {
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

    @SuppressWarnings("ConstantConditions")
    public SubUtility start(String processId) throws IOException {
        if (IncomeUtilityApplication.class.getResource(layoutPath) == null) {
            throw new InvalidPathException(layoutPath, "The specified layout path for this utility is invalid");
        }

        if (IncomeUtilityApplication.class.getResource("icons/" + iconFilePath) == null) {
            throw new InvalidPathException(iconFilePath, "The specified icon file path for this utility is invalid!");
        }


        Stage window = new Stage();
        FXMLLoader loader = new FXMLLoader(IncomeUtilityApplication.class.getResource(layoutPath));
        loader.setController(this.controller);

        try {
            Scene s = new Scene(loader.load());
            window.setScene(s);
            window.setMinWidth(minWidth);
            window.setMinHeight(minHeight);
            window.setResizable(allowResize);
            window.setTitle(type.getDisplayName());
            window.getIcons().add(new Image(IncomeUtilityApplication.class.getResource("icons/" + iconFilePath).toExternalForm()));
            window.setOnCloseRequest((_) -> stop());
        } catch (Exception e) {
            log("Failed to start utility of type '" + type + "'");
            log.logStackTrace(e);
        }

        // If the utility has a command prompt
        if (loader.getNamespace().get("commandPrompt") != null) {
            log("Command prompt found, setting up...");
            commandPrompt = (HBox) loader.getNamespace().get("commandPrompt");
            commandPromptField = (TextField) loader.getNamespace().get("commandPromptField");
            Label commandPromptLabel = (Label) loader.getNamespace().get("commandPromptLabel");

            commandPrompt.managedProperty().bind(commandPrompt.visibleProperty());
            commandPromptLabel.setText(utilityName.toLowerCase().replaceAll("[,.;]", "").trim().replaceAll(" ", "-"));
            commandPromptField.setText("");
            commandPromptField.setOnAction((_) -> {
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
            //noinspection CallToPrintStackTrace
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
        log.info(msg, String.format("%s/%s", utilityName, processId));
    }
}
