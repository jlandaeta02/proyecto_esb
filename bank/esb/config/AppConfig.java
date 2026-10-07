package com.bank.esb.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class AppConfig {

    private static AppConfig instance;
    private final Properties properties;

    // Ruta por defecto en el Integrated File System (IFS) de IBM i
    private static final String DEFAULT_IFS_CONFIG_PATH = "/home/JLANDAETA/com/bank/esb/config/application.properties";

    private AppConfig() {
        this.properties = new Properties();
        loadProperties();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    private void loadProperties() {
        // 1. Intentar cargar desde la propiedad de sistema (java -Dconfig.file=...)
        String customPath = System.getProperty("config.file");
        Path configPath = (customPath != null && !customPath.isBlank()) 
                ? Paths.get(customPath) 
                : Paths.get(DEFAULT_IFS_CONFIG_PATH);

        if (Files.exists(configPath)) {
            try (InputStream input = new FileInputStream(configPath.toFile())) {
                properties.load(input);
                System.out.println("[AppConfig] Configuración cargada exitosamente desde IFS: " + configPath.toAbsolutePath());
            } catch (Exception e) {
                System.err.println("[AppConfig] Error cargando archivo de propiedades desde IFS: " + e.getMessage());
            }
        } else {
            // 2. Fallback: Intentar cargar desde el Classpath (útil para pruebas locales fuera del AS/400)
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
                if (input != null) {
                    properties.load(input);
                    System.out.println("[AppConfig] Configuración cargada desde el Classpath local.");
                } else {
                    System.err.println("[AppConfig] ADVERTENCIA: No se encontró application.properties en IFS ni en Classpath. Se utilizarán valores por defecto.");
                }
            } catch (Exception e) {
                System.err.println("[AppConfig] Error cargando propiedades desde Classpath: " + e.getMessage());
            }
        }
    }

    // --- Métodos de acceso a parámetros del Servidor Socket ---

    public int getServerPort() {
        return Integer.parseInt(properties.getProperty("server.port", "9090"));
    }

    public int getServerTimeoutMs() {
        return Integer.parseInt(properties.getProperty("server.timeout.ms", "5000"));
    }

    // --- Métodos de acceso a la Concurrencia (Thread Pool) ---

    public int getPoolCoreSize() {
        return Integer.parseInt(properties.getProperty("pool.core.size", "20"));
    }

    public int getPoolMaxSize() {
        return Integer.parseInt(properties.getProperty("pool.max.size", "200"));
    }

    public int getPoolQueueCapacity() {
        return Integer.parseInt(properties.getProperty("pool.queue.capacity", "500"));
    }

    // --- Métodos de acceso a Base de Datos DB2/400 ---

    public String getDbUrl() {
        return properties.getProperty("db.url", "jdbc:db2:*local");
    }

    public String getDbDriver() {
        return properties.getProperty("db.driver", "com.ibm.db2.jdbc.app.DB2Driver");
    }

    public int getDbPoolMaxSize() {
        return Integer.parseInt(properties.getProperty("db.pool.max.size", "30"));
    }

    public int getDbPoolMinIdle() {
        return Integer.parseInt(properties.getProperty("db.pool.min.idle", "10"));
    }

    // --- Métodos de acceso a la Criptografía ---

    public String getSystemMasterKey() {
        return properties.getProperty("system.master.key", "ClaveMaestraSistemaIBM");
    }

    // Obtención genérica de propiedades
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
