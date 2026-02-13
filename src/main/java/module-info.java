module com.krisapps.incomeutility_v2 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;
    requires org.jetbrains.annotations;
    requires java.logging;
    requires com.google.gson;
    requires javafx.graphics;
    requires java.desktop;
    requires javafx.base;

    opens com.krisapps.incomeutility_v2 to javafx.fxml;
    opens com.krisapps.incomeutility_v2.subutilities.money_flow to javafx.fxml;
    opens com.krisapps.incomeutility_v2.dialogs to javafx.fxml;
    opens com.krisapps.incomeutility_v2.util to com.google.gson;
    opens com.krisapps.incomeutility_v2.types.fiscal to com.google.gson;
    opens com.krisapps.incomeutility_v2.ui.listview.cell to javafx.fxml;

    exports com.krisapps.incomeutility_v2;
    exports com.krisapps.incomeutility_v2.types.transaction;
}