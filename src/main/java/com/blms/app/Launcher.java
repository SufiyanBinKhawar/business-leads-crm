package com.blms.app;

/**
 * OWNER: Ismail. Run THIS class from IntelliJ (not MainApp) - a main class that does not extend
 * Application avoids the "JavaFX runtime components are missing" error on the classpath.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        MainApp.main(args);
    }
}
