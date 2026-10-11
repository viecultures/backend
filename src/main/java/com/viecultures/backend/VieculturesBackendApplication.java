package com.viecultures.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@SpringBootApplication
public class VieculturesBackendApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(VieculturesBackendApplication.class, args);
    }

    /**
     * Automatically scan and load .env variables into System properties if not already set.
     */
    private static void loadDotEnv() {
        String[] possiblePaths = {".env", "backend/.env", "../.env"};
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                try {
                    List<String> lines = Files.readAllLines(file.toPath());
                    for (String line : lines) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int eq = line.indexOf('=');
                        if (eq > 0) {
                            String key = line.substring(0, eq).trim();
                            String val = line.substring(eq + 1).trim();
                            if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, val);
                            }
                        }
                    }
                    System.out.println("[INFO] Loaded environment variables from: " + file.getAbsolutePath());
                    break;
                } catch (Exception e) {
                    System.err.println("[WARN] Could not load .env from " + path + ": " + e.getMessage());
                }
            }
        }
    }
}
