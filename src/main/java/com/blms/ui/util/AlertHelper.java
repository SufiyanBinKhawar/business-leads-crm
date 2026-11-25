package com.blms.ui.util;

import com.blms.exception.BlmsException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/** OWNER: Ismail. All user-facing messages go through here. */
public final class AlertHelper {

    private AlertHelper() {
    }

    public static void info(String title, String message) {
        show(Alert.AlertType.INFORMATION, title, message);
    }

    public static void warn(String title, String message) {
        show(Alert.AlertType.WARNING, title, message);
    }

    /** BlmsException messages are user-safe (D3 extension texts); anything else is unexpected. */
    public static void error(Throwable t) {
        String msg = t instanceof BlmsException ? t.getMessage() : "Unexpected error: " + t;
        show(Alert.AlertType.ERROR, "Error", msg);
    }

    public static boolean confirm(String title, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        a.setTitle(title);
        a.setHeaderText(null);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.YES;
    }

    private static void show(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type, message);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
