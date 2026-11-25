package com.blms.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** OWNER: Sufiyan. Infrastructure (not in D5). Loads application.properties, then config/local.properties overrides. */
public final class AppConfig {

    private final Properties props = new Properties();

    private AppConfig() {
    }

    public static AppConfig load() {
        AppConfig cfg = new AppConfig();
        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                cfg.props.load(in);
            }
            Path local = Path.of("config", "local.properties");
            if (Files.exists(local)) {
                try (Reader r = Files.newBufferedReader(local, StandardCharsets.UTF_8)) {
                    cfg.props.load(r);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load configuration", e);
        }
        return cfg;
    }

    public String get(String key) {
        return props.getProperty(key, "").trim();
    }
}
