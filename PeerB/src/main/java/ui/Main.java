package ui;

import service.BaseStationServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Punto de entrada ejecutable para la Estación Base (PeerB).
 * Inicia el servidor UDP en el puerto 5000 y muestra el log de actividad.
 */
public class Main {

    private static final int PORT = 5000;

    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println("   ESTACION BASE DE TELEMETRIA IoT - UDP SERVER (PEER B)     ");
        System.out.println("=============================================================");

        BaseStationServer server = new BaseStationServer(PORT);

        try {
            server.start();
            System.out.println("[INFO] Servidor escuchando en el puerto UDP " + PORT + "...");
            System.out.println("[INFO] Esperando paquetes de sensores en campo (PeerA)...");
            System.out.println("[INFO] Escriba 'q' + Enter o presione Ctrl+C para detener el servidor.");
            System.out.println("-------------------------------------------------------------");

            // Registrar ShutdownHook para cerrar limpiamente con Ctrl+C
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[INFO] Deteniendo servidor UDP...");
                server.stop();
                System.out.println("[INFO] Servidor detenido con exito.");
            }));

            // Manejo tanto de consola interactiva como ejecución en segundo plano
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    // Si la entrada estándar no está conectada a un TTY (ej. proceso en segundo plano)
                    Thread.sleep(Long.MAX_VALUE);
                    break;
                }
                if (line.trim().equalsIgnoreCase("q") || line.trim().equalsIgnoreCase("exit")) {
                    break;
                }
            }

            System.out.println("[INFO] Deteniendo servidor UDP...");
            server.stop();
            System.out.println("[INFO] Servidor detenido.");

        } catch (InterruptedException ignored) {
            server.stop();
        } catch (Exception e) {
            System.err.println("[ERROR] Error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
