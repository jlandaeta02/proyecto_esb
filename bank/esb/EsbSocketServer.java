package com.bank.esb;

import com.bank.esb.config.AppConfig;
import com.bank.esb.concurrency.ClientTask;
import com.bank.esb.concurrency.ConcurrencyManager;
import com.bank.esb.db.HikariDbPool;

import java.net.ServerSocket;
import java.net.Socket;

public class EsbSocketServer {

    private static boolean running = true;

    public static void main(String[] args) {
        AppConfig config = AppConfig.getInstance();
        int port = config.getServerPort();

        // 1. Inicializar el Pool de Conexiones a DB2/400
        HikariDbPool.init();

        // 2. Inicializar el Gestor Concurrente de Hilos
        ConcurrencyManager concurrencyManager = new ConcurrencyManager();

        System.out.println("Servidor ESB listo en el puerto " + port + " [DB2 Pool + Concurrencia Activos]");

        // Hook de Apagado para liberar recursos en IBM i (SIGTERM / ENDJOB)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Iniciando secuencia de apagado seguro del ESB...");
            running = false;
            concurrencyManager.shutdown();
            HikariDbPool.shutdown();
        }));

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    concurrencyManager.execute(new ClientTask(clientSocket));
                } catch (Exception e) {
                    if (!running) break;
                    System.err.println("Error aceptando conexión: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error crítico en ServerSocket: " + e.getMessage());
        }
    }
}