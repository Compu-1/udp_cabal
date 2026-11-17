package ui;

import client.SensorClient;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Scanner;

/**
 * Punto de entrada interactivo para el Nodo Sensor (PeerA).
 * Presenta un menú de consola para enviar tramas normales, alertas, consultas
 * de estado o tramas personalizadas, mostrando el manejo de timeout en vivo.
 */
public class Main {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;
    private static final int TIMEOUT_MS = 2000;

    public static void main(String[] args) {
        SensorClient client = new SensorClient(SERVER_HOST, SERVER_PORT, TIMEOUT_MS);
        Scanner scanner = new Scanner(System.in);

        System.out.println("=============================================================");
        System.out.println("       NODO SENSOR IoT - CLIENTE UDP (PEER A)                ");
        System.out.println("=============================================================");
        System.out.println("Servidor destino: " + SERVER_HOST + ":" + SERVER_PORT);
        System.out.println("Timeout configurado: " + TIMEOUT_MS + " ms");
        System.out.println("-------------------------------------------------------------");

        boolean exit = false;

        while (!exit) {
            printMenu();
            System.out.print("Seleccione una opción: ");
            String input = scanner.nextLine().trim();

            String messageToSend = null;

            switch (input) {
                case "1":
                    messageToSend = "sensor-01;TEMP;25.0";
                    break;
                case "2":
                    messageToSend = "sensor-01;TEMP;42.5";
                    break;
                case "3":
                    messageToSend = "sensor-01;TEMP;-5.0";
                    break;
                case "4":
                    messageToSend = "sensor-01;HUMIDITY;60.0";
                    break;
                case "5":
                    messageToSend = "sensor-01;HUMIDITY;93.0";
                    break;
                case "6":
                    messageToSend = "sensor-01;HUMIDITY;18.0";
                    break;
                case "7":
                    messageToSend = "sensor-01;BATTERY;80.0";
                    break;
                case "8":
                    messageToSend = "sensor-01;BATTERY;15.0";
                    break;
                case "9":
                    messageToSend = "STATUS;sensor-01";
                    break;
                case "10":
                    System.out.print("Ingrese la trama personalizada (ej: sensor-02;TEMP;30.0): ");
                    messageToSend = scanner.nextLine().trim();
                    break;
                case "0":
                    exit = true;
                    System.out.println("\n[INFO] Saliendo del programa. ¡Hasta luego!");
                    continue;
                default:
                    System.out.println("\n[AVISO] Opción no válida. Por favor seleccione una opción del menú.\n");
                    continue;
            }

            if (messageToSend != null && !messageToSend.isEmpty()) {
                sendAndDisplay(client, messageToSend);
            }
        }

        scanner.close();
    }

    private static void printMenu() {
        System.out.println("\n--- MENÚ DE TELEMETRÍA IoT ---");
        System.out.println("1. Enviar Temperatura Normal               (sensor-01;TEMP;25.0)");
        System.out.println("2. Enviar Alerta Temperatura Alta          (sensor-01;TEMP;42.5)");
        System.out.println("3. Enviar Alerta Temperatura Bajo Cero     (sensor-01;TEMP;-5.0)");
        System.out.println("4. Enviar Humedad Normal                   (sensor-01;HUMIDITY;60.0)");
        System.out.println("5. Enviar Alerta Humedad Crítica Alta      (sensor-01;HUMIDITY;93.0)");
        System.out.println("6. Enviar Alerta Humedad Crítica Baja      (sensor-01;HUMIDITY;18.0)");
        System.out.println("7. Enviar Batería Normal                   (sensor-01;BATTERY;80.0)");
        System.out.println("8. Enviar Alerta Batería Crítica           (sensor-01;BATTERY;15.0)");
        System.out.println("9. Consultar Estado del Sensor             (STATUS;sensor-01)");
        System.out.println("10. Enviar Trama Personalizada");
        System.out.println("0. Salir");
        System.out.println("-------------------------------------------------------------");
    }

    private static void sendAndDisplay(SensorClient client, String message) {
        System.out.println("\n[TX] Enviando trama UDP: " + message);
        long start = System.currentTimeMillis();
        try {
            String response = client.sendAndReceive(message);
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[RX] Respuesta de la Estación Base (" + elapsed + " ms): " + response);
        } catch (SocketTimeoutException e) {
            long elapsed = System.currentTimeMillis() - start;
            System.err.println("[TIMEOUT] Tiempo de espera agotado (" + elapsed + " ms). La estación base no respondió.");
        } catch (IOException e) {
            System.err.println("[ERROR] Error de comunicación de red: " + e.getMessage());
        }
    }
}
