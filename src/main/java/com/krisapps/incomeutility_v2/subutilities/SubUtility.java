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
    private Object controller;

    private int minWidth;
    private int minHeight;
    private boolean allowResize;

    private Consumer<String> onCloseCallback = (id) -> {};

    private Stage instance;

    public SubUtility(SubUtilityType type, String layoutPath, Object controller, int minWidth, int minHeight, boolean allowResize) {
        this.type = type;
        this.layoutPath = layoutPath;
        this.controller = controller;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.allowResize = allowResize;
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

        initialize();

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
                shutdown(this.controller);
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

    public String getName() {
        return type.getDisplayName();
    }

    public SubUtilityType getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    /**
     * Runs when this sub-utility is initializing.
     */
    public abstract void initialize();

    /**
     * Runs when this sub-utility is shut down (closed).
     */
    public abstract void shutdown(Object controller);
}
