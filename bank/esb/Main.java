package com.bank.esb;

import com.bank.esb.concurrency.ClientTask;
import com.bank.esb.router.ServiceRouter; // <-- Cambiar .services. por .router.
import com.bank.esb.services.impl.PagoService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static final int DEFAULT_PORT = 9093;
    private static final int THREAD_POOL_SIZE = 20;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Puerto inválido. Usando el puerto por defecto: " + DEFAULT_PORT);
            }
        }

        // 1. Inicializar y registrar servicios en el router
        //ServiceRouter router = ServiceRouter.getInstance();
        //router.registerService(new PagoService());
        
        //System.out.println("[ESB] Servicios registrados correctamente:");
        //router.getRegisteredServices().forEach(s -> System.out.println(" - " + s));

        // 2. Crear el pool de hilos para concurrencia
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        // 3. Iniciar el ServerSocket
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[ESB] Servidor iniciado escuchando en el puerto " + port + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[ESB] Nueva conexion aceptada desde: " + clientSocket.getRemoteSocketAddress());
                
                // Delegar atención del cliente al hilo de concurrencia ClientTask
                threadPool.execute(new ClientTask(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("[ESB] Error critico en el servidor TCP: " + e.getMessage());
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }
}
