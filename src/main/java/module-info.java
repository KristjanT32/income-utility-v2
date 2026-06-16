module com.krisapps.incomeutility_v2 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fluentui;
    requires org.jetbrains.annotations;
    requires java.logging;
    requires com.google.gson;
    requires javafx.graphics;
    requires java.desktop;
    requires javafx.base;
    requires java.sql;
    requires jdk.jshell;
    requires java.management;

    opens com.krisapps.incomeutility_v2 to javafx.fxml;
    opens com.krisapps.incomeutility_v2.subutilities.money_flow to javafx.fxml;
    opens com.krisapps.incomeutility_v2.dialogs to javafx.fxml;
    opens com.krisapps.incomeutility_v2.types.fiscal to com.google.gson;
    opens com.krisapps.incomeutility_v2.ui.listview.cell to javafx.fxml;
    opens com.krisapps.incomeutility_v2.subutilities.pricer to javafx.fxml;
    opens com.krisapps.incomeutility_v2.subutilities.breakdown to javafx.fxml;
    opens com.krisapps.incomeutility_v2.subutilities.settings to javafx.fxml;

    exports com.krisapps.incomeutility_v2;
    exports com.krisapps.incomeutility_v2.types.transaction;
    exports com.krisapps.incomeutility_v2.types.fiscal;
    exports com.krisapps.incomeutility_v2.types.pricer;
    exports com.krisapps.incomeutility_v2.util;
    exports com.krisapps.incomeutility_v2.types.data;
    opens com.krisapps.incomeutility_v2.types.data to com.google.gson;
    opens com.krisapps.incomeutility_v2.util.services to com.google.gson;
    opens com.krisapps.incomeutility_v2.types.fiscal.cashew to com.google.gson;
    opens com.krisapps.incomeutility_v2.util to com.google.gson, javafx.fxml;
    opens com.krisapps.incomeutility_v2.ui.listview to javafx.fxml;
    opens com.krisapps.incomeutility_v2.dialogs.generic to javafx.fxml;
}