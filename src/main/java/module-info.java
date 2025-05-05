module com.example.mantracount {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.base;
    requires java.desktop;
    requires java.base;
    requires org.json;


    opens com.example.mantracount to javafx.fxml;
    exports com.example.mantracount;
}