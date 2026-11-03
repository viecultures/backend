package com.viecultures.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.net.BindException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@EnableScheduling
@SpringBootApplication
public class VieculturesBackendApplication {

    public static void main(String[] args) {
        try {
            // 1. Quet va nap bien moi truong tu file .env
            boolean envLoaded = loadDotEnv();

            // 2. Bat loi cau hinh .env (thieu bien, bien sai, dung gia tri placeholder mau)
            validateEnvironment(envLoaded);

            // 3. Khoi chay Spring Boot
            SpringApplication.run(VieculturesBackendApplication.class, args);

            // In thong bao khi khoi dong thanh cong
            printSuccessBanner();

        } catch (Throwable ex) {
            // Xu ly va phan loai toan bo loi: Loi DB, Loi Port, Loi Code/Runtime
            handleStartupError(ex);
            System.exit(1);
        }
    }

    /**
     * Tu dong quet va nap file .env vao System properties neu chua duoc cau hinh.
     * 
     * @return true neu tim thay va nap thanh cong it nhat 1 file .env, false neu
     *         khong tim thay file nao.
     */
    static boolean loadDotEnv() {
        String[] possiblePaths = { ".env", "backend/.env", "../.env" };
        boolean loaded = false;

        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                try {
                    List<String> lines = Files.readAllLines(file.toPath());
                    for (String line : lines) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#"))
                            continue;
                        int eq = line.indexOf('=');
                        if (eq > 0) {
                            String key = line.substring(0, eq).trim();
                            String val = line.substring(eq + 1).trim();
                            if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if (val != null && !val.isBlank()) {
                                System.setProperty(key, val);
                            }
                        }
                    }
                    System.out.println("[INFO] Da nap thanh cong bien moi truong tu: " + file.getAbsolutePath());
                    loaded = true;
                    break;
                } catch (Exception e) {
                    System.err.println("[WARN] Khong the doc file .env tu " + path + ": " + e.getMessage());
                }
            }
        }
        return loaded;
    }

    /**
     * Kiem tra tinh hop le cua cac du lieu moi truong bat buoc.
     * Phat hien du lieu thieu, rong hoac con dung placeholder mau chua cap nhat.
     */
    static void validateEnvironment(boolean envLoaded) {
        // Kiem tra che do Deploy: neu DEPLOY=true, deploy=true, hoac dang chay tren cloud (vi du: Render)
        String deployMode = getEnvOrProperty("DEPLOY");
        if (deployMode == null || deployMode.isBlank()) {
            deployMode = getEnvOrProperty("deploy");
        }
        boolean isDeploy = "true".equalsIgnoreCase(deployMode)
                || "1".equals(deployMode)
                || "yes".equalsIgnoreCase(deployMode)
                || "true".equalsIgnoreCase(getEnvOrProperty("SKIP_ENV_CHECK"))
                || "true".equalsIgnoreCase(getEnvOrProperty("SKIP_ENV_VALIDATION"))
                || System.getenv("RENDER") != null;

        if (isDeploy) {
            System.out.println("[INFO] Che do Deploy / Cloud duoc kich hoat (DEPLOY=true hoac Render). Bo qua kiem tra file .env.");
            return;
        }

        List<String> errors = new ArrayList<>();

        if (!envLoaded) {
            // Neu khong co file .env va khong co bien moi truong he thong quan trong
            boolean hasEssentialEnv = getEnvOrProperty("DB_URL") != null
                    && getEnvOrProperty("DB_USERNAME") != null
                    && getEnvOrProperty("JWT_SECRET") != null;
            if (!hasEssentialEnv) {
                errors.add("Khong tim thay file .env tai thu muc goc hoac backend/.env!");
            }
        }

        // 1. Kiem tra PORT
        String port = getEnvOrProperty("PORT");
        if (port == null || port.isBlank()) {
            errors.add("Thieu bien PORT trong .env (mac dinh khuyen nghi: 8080).");
        } else {
            try {
                int p = Integer.parseInt(port.trim());
                if (p < 1 || p > 65535) {
                    errors.add("Bien PORT khong hop le (" + port + "). Phai la so trong khoang 1 - 65535.");
                }
            } catch (NumberFormatException e) {
                errors.add("Bien PORT khong phai la so hop le: '" + port + "'.");
            }
        }

        // 2. Kiem tra Database URL
        String dbUrl = getEnvOrProperty("DB_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            errors.add("Thieu bien DB_URL trong .env.");
        } else if (!dbUrl.startsWith("jdbc:")) {
            errors.add("Bien DB_URL khong dung dinh dang JDBC (can bat dau bang 'jdbc:postgresql://...').");
        } else if (dbUrl.contains("your_") || dbUrl.contains("example.com")) {
            errors.add("Bien DB_URL dang chua thong tin mau placeholder chua cap nhat.");
        }

        // 3. Kiem tra DB_USERNAME
        String dbUser = getEnvOrProperty("DB_USERNAME");
        if (dbUser == null || dbUser.isBlank()) {
            errors.add("Thieu bien DB_USERNAME trong .env.");
        } else if (dbUser.contains("your_") || dbUser.equalsIgnoreCase("postgres.example")) {
            errors.add("Bien DB_USERNAME dang la placeholder mau ('" + dbUser + "').");
        }

        // 4. Kiem tra DB_PASSWORD
        String dbPass = getEnvOrProperty("DB_PASSWORD");
        if (dbPass == null || dbPass.isBlank()) {
            errors.add("Thieu bien DB_PASSWORD trong .env.");
        } else if (dbPass.contains("your_db_password_here") || dbPass.equals("your_password")) {
            errors.add("Bien DB_PASSWORD dang la mat khau mau ('" + dbPass + "').");
        }

        // 5. Kiem tra JWT_SECRET
        String jwtSecret = getEnvOrProperty("JWT_SECRET");
        if (jwtSecret == null || jwtSecret.isBlank()) {
            errors.add("Thieu bien JWT_SECRET trong .env.");
        } else if (jwtSecret.contains("your_32_characters_secret_jwt_signing_key_here!")) {
            errors.add("Bien JWT_SECRET dang la key mau. Vui long cap nhat key bi mat cua team.");
        } else if (jwtSecret.length() < 32) {
            errors.add("Bien JWT_SECRET qua ngan (" + jwtSecret.length()
                    + " ky tu). HMAC-SHA yeu cau toi thieu 32 ky tu (256-bit).");
        }

        // Neu co loi cau hinh, hien thi bang thong bao va dung ung dung
        if (!errors.isEmpty()) {
            System.err.println("\n" + "=".repeat(85));
            System.err.println("[LOI CAU HINH .ENV] PHAT HIEN THONG TIN MOI TRUONG CHUA DUNG HOAC CHUA CAP NHAT!");
            System.err.println("=".repeat(85));
            for (String err : errors) {
                System.err.println("  - " + err);
            }
            System.err.println("\nHUONG DAN CAP NHAT:");
            System.err.println("  Vui long cap nhat lai file .env cua team.");
            System.err.println("  Sau do luu vao thu muc: 'backend/.env'");
            System.err.println("=".repeat(85) + "\n");
            throw new IllegalStateException(
                    "Cau hinh moi truong .env khong hop le. Vui long kiem tra lai log ben tren.");
        }
    }

    /**
     * Bat va phan loai chi tiet cac loi khi ung dung khoi dong.
     */
    static void handleStartupError(Throwable ex) {
        Throwable rootCause = getRootCause(ex);
        String fullErrorChain = buildExceptionChainString(ex);

        System.err.println("\n" + "=".repeat(85));

        if (isDatabaseError(fullErrorChain, rootCause)) {
            // Truong hop 1: Loi ket noi Database
            System.err.println("[LOI KET NOI DATABASE] KHONG THE KET NOI DEN CO SO DU LIEU POSTGRESQL / SUPABASE!");
            System.err.println("=".repeat(85));
            System.err.println(
                    "Chi tiet nguyen nhan: " + rootCause.getClass().getSimpleName() + " - " + rootCause.getMessage());
            System.err.println("\nCAC NGUYEN NHAN THUONG GAP VA CACH KHAC PHUC:");
            System.err.println("  1. Mat khau DB hoac User khong dung (password authentication failed):");
            System.err.println("     -> Kiem tra lai DB_USERNAME va DB_PASSWORD trong backend/.env.");
            System.err.println("  2. Du an Supabase dang bi tam dung (Paused) hoac qua tai Connection:");
            System.err.println("     -> Truy cap https://supabase.com/dashboard kiem tra trang thai database.");
            System.err.println("  3. Ket noi mang / Tuong lua:");
            System.err.println("     -> Kiem tra ket noi Internet hoac thu tat VPN/Firewall chan cong ket noi ra ngoai.");
            System.err.println("  4. Cap nhat lai cau hinh .env moi nhat cua team.");

        } else if (isPortAlreadyInUse(fullErrorChain, rootCause)) {
            // Truong hop 2: Loi trung cong mang
            String port = getEnvOrProperty("PORT");
            System.err.println("[LOI CONG MANG] CONG SERVER DA BI CHIEM DUNG (PORT IN USE)!");
            System.err.println("=".repeat(85));
            System.err.println("Cong " + (port != null ? port : "8080") + " dang duoc su dung boi mot tien trinh khac.");
            System.err.println("\nCACH KHAC PHUC:");
            System.err.println("  - Dung tien trinh cu dang chiem cong (vi du: tat ung dung Java cu chay ngam).");
            System.err.println("  - Hoac doi sang cong khac bang cach sua 'PORT=8081' trong file backend/.env.");

        } else {
            // Truong hop 3: Loi code / Bean injection / Loi runtime khac
            System.err.println("[LOI KHOI DONG HE THONG / LOI CODE] XAY RA LOI TRONG QUA TRINH KHOI TAO UNG DUNG!");
            System.err.println("=".repeat(85));
            System.err.println("Loai loi (Root Cause): " + rootCause.getClass().getName());
            System.err.println(
                    "Thong diep loi: " + (rootCause.getMessage() != null ? rootCause.getMessage() : ex.getMessage()));

            // In cac vi tri loi trong source code thuoc package com.viecultures
            System.err.println("\nCac vi tri phat sinh trong source code (com.viecultures):");
            boolean foundAppTrace = false;
            for (StackTraceElement element : rootCause.getStackTrace()) {
                if (element.getClassName().startsWith("com.viecultures")) {
                    System.err.println("  at " + element);
                    foundAppTrace = true;
                }
            }
            if (!foundAppTrace) {
                // Neu khong co frame thuoc com.viecultures trong root cause, in top stack trace cua exception goc
                for (int i = 0; i < Math.min(5, ex.getStackTrace().length); i++) {
                    System.err.println("  at " + ex.getStackTrace()[i]);
                }
            }
            System.err.println(
                    "\nGoi y: Vui long kiem tra lai cu phap code, cau hinh bean, dependency injection hoac annotation vua thay doi.");
        }

        System.err.println("=".repeat(85) + "\n");
    }

    /**
     * Kiem tra xem chuoi loi co lien quan toi ket noi Database hay khong.
     */
    private static boolean isDatabaseError(String fullChain, Throwable rootCause) {
        String lowerChain = fullChain.toLowerCase();
        String rootName = rootCause.getClass().getName().toLowerCase();

        return rootName.contains("psqlexception")
                || rootName.contains("hikari")
                || rootName.contains("cannotgetjdbcconnectionexception")
                || rootName.contains("jdbcconnectionexception")
                || rootName.contains("sqlexception")
                || lowerChain.contains("password authentication failed")
                || lowerChain.contains("the connection attempt failed")
                || lowerChain.contains("connection refused")
                || lowerChain.contains("driver org.postgresql.driver")
                || lowerChain.contains("hikaripool")
                || lowerChain.contains("cannot open connection")
                || lowerChain.contains("database");
    }

    /**
     * Kiem tra xem co phai loi trung cong (Port In Use / BindException) hay khong.
     */
    private static boolean isPortAlreadyInUse(String fullChain, Throwable rootCause) {
        return rootCause instanceof BindException
                || fullChain.contains("PortInUseException")
                || fullChain.contains("Address already in use")
                || fullChain.contains("Web server failed to start. Port");
    }

    /**
     * Tim nguyen nhan goc re (Root Cause) trong chuoi exception.
     */
    private static Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    /**
     * Ghep toan bo chuoi thong diep va ten lop exception thanh mot chuoi van ban de kiem tra.
     */
    private static String buildExceptionChainString(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        Throwable current = ex;
        while (current != null) {
            sb.append(current.getClass().getName()).append(": ").append(current.getMessage()).append(" | ");
            current = current.getCause();
        }
        return sb.toString();
    }

    /**
     * Lay gia tri tu System properties hoac System environment variables.
     */
    private static String getEnvOrProperty(String key) {
        String val = System.getProperty(key);
        if (val == null || val.isBlank()) {
            val = System.getenv(key);
        }
        return val;
    }

    private static String getEnvOrProperty(String key, String defaultValue) {
        String val = getEnvOrProperty(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }

    private static void printSuccessBanner() {
        String port = getEnvOrProperty("PORT", "8080");
        System.out.println("\n" + "=".repeat(85));
        System.out.println("[VIECULTURES BACKEND] DA KHOI DONG THANH CONG!");
        System.out.println("=".repeat(85));
        System.out.println("  - Cong Server (PORT) : http://localhost:" + port);
        System.out.println("  - Swagger UI API Docs: http://localhost:" + port + "/swagger-ui.html");
        System.out.println("  - OpenAPI JSON Docs  : http://localhost:" + port + "/v3/api-docs");
        System.out.println("=".repeat(85) + "\n");
    }
}

