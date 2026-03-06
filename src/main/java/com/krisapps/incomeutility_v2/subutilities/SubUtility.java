package com.krisapps.incomeutility_v2.subutilities;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.util.DataManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class SubUtility {
    private String id;
    private SubUtilityType type;
    private String layoutPath;
    private SubUtilityController controller;

    private int minWidth;
    private int minHeight;
    private boolean allowResize;


    private String processId;
    private String utilityName;
    private Consumer<String> onCloseCallback = (id) -> {};

    private Stage instance;

    public SubUtility(SubUtilityType type, String layoutPath, SubUtilityController controller, int minWidth, int minHeight, boolean allowResize) {
        this.type = type;
        this.layoutPath = layoutPath;
        this.controller = controller;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.allowResize = allowResize;
        this.utilityName = type.getDisplayName();
    }

    public SubUtility(SubUtilityType type, String layoutPath, SubUtilityController controller, int minWidth, int minHeight, boolean allowResize, String processId) {
        this.type = type;
        this.layoutPath = layoutPath;
        this.controller = controller;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.allowResize = allowResize;
        this.processId = processId;
        this.utilityName = type.getDisplayName();
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
        window.setOnCloseRequest((_) -> {
            stop();
        });

        this.controller.onStartup(this);

        try {
            this.id = processId;
            this.instance = window;
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
