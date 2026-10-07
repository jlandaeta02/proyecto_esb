package com.bank.esb.db;

import com.bank.esb.config.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class HikariDbPool {

    private static HikariDataSource dataSource;

    private HikariDbPool() {
        // Clase de utilidad no instanciable
    }

    public static synchronized void init() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }

        AppConfig config = AppConfig.getInstance();

        HikariConfig hikariConfig = new HikariConfig();

        // 1. Configuración del Driver y URL Nativa DB2/400
        hikariConfig.setDriverClassName(config.getDbDriver());
        hikariConfig.setJdbcUrl(config.getDbUrl());

        // En conexiones locales nativas a DB2/400 (jdbc:db2:*local)
        // no se requiere usuario y clave explícitos ya que usa la autoridad del Job actual.
        // Si se usa JTOpen fuera de la máquina, se especificarían aquí:
        // hikariConfig.setUsername(config.getProperty("db.user", ""));
        // hikariConfig.setPassword(config.getProperty("db.password", ""));

        // 2. Parámetros de Capacidad del Pool
        hikariConfig.setMaximumPoolSize(config.getDbPoolMaxSize()); // p. ej., 30 conexiones
        hikariConfig.setMinimumIdle(config.getDbPoolMinIdle());     // p. ej., 10 conexiones ociosas
        hikariConfig.setPoolName("HikariPool-DB2-AS400");

        // 3. Timeouts y Optimización de Vida Util
        hikariConfig.setConnectionTimeout(5000);  // 5 segundos máx. esperando una conexión
        hikariConfig.setIdleTimeout(600000);      // 10 minutos para liberar conexiones ociosas
        hikariConfig.setMaxLifetime(1800000);     // 30 minutos máx. de vida útil por conexión
        hikariConfig.setValidationTimeout(3000);  // 3 segundos para validar si la conexión está viva

        // 4. Query de Validación Nativa en DB2 for i
        hikariConfig.setConnectionTestQuery("SELECT 1 FROM QSYS2.QSQPTABL");

        // 5. Propiedades Adicionales del Driver Nativo IBM i
        // 'prompt=false' evita diálogos de autenticación
        // 'naming=system' permite sintaxis de bibliotecas LIB/FILE si se prefiere
        hikariConfig.addDataSourceProperty("prompt", "false");
        hikariConfig.addDataSourceProperty("errors", "full");

        dataSource = new HikariDataSource(hikariConfig);
        System.out.println("[HikariDbPool] Pool de conexiones nativas DB2/400 inicializado correctamente.");
    }

    /**
     * Obtiene una conexión activa del Pool.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            init();
        }
        return dataSource.getConnection();
    }

    /**
     * Cierre controlado del Pool de conexiones durante el apagado del ESB.
     */
    public static synchronized void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("[HikariDbPool] Pool de conexiones DB2/400 cerrado correctamente.");
        }
    }
}
