module com.krisapps.incomeutility_v2 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;

    opens com.krisapps.incomeutility_v2 to javafx.fxml;
    exports com.krisapps.incomeutility_v2;
}