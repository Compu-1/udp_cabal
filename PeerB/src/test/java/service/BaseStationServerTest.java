package service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BaseStationServerTest {

    private BaseStationServer server;
    private int serverPort;

    @BeforeEach
    void setUp() throws IOException {
        // Usar puerto 0 para que el sistema operativo asigne un puerto libre automáticamente
        server = new BaseStationServer(0);
        server.start();
        serverPort = server.getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    private String sendAndReceive(String message) throws IOException {
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            clientSocket.setSoTimeout(3000);
            byte[] sendBuffer = message.getBytes(StandardCharsets.UTF_8);
            InetAddress host = InetAddress.getByName("localhost");
            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, host, serverPort);
            clientSocket.send(sendPacket);

            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            clientSocket.receive(receivePacket);

            return new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("IT-01: Envío de telemetría y recepción de respuesta a través de Socket UDP")
    void testIT01_SendTelemetryAndReceiveResponse() throws IOException {
        String response = sendAndReceive("sensor-01;TEMP;25.5");
        assertEquals("OK;TEMP_RECORDED;25.5", response);

        String alertResponse = sendAndReceive("sensor-01;TEMP;42.5");
        assertEquals("ALERT;HIGH_TEMPERATURE;42.5", alertResponse);
    }

    @Test
    @DisplayName("IT-02: Envío de consulta STATUS y validación de error de dispositivo no encontrado")
    void testIT02_SendStatusQueryAndReceiveResponse() throws IOException {
        // Primero registrar telemetría
        sendAndReceive("sensor-01;BATTERY;85.0");

        // Consultar estado existente
        String statusResponse = sendAndReceive("STATUS;sensor-01");
        assertEquals("STATUS_OK;sensor-01;BATTERY;85.0", statusResponse);

        // Consultar dispositivo no existente
        String notFoundResponse = sendAndReceive("STATUS;sensor-desconocido");
        assertEquals("ERROR;DEVICE_NOT_FOUND", notFoundResponse);

        // Envío de trama con formato inválido
        String invalidResponse = sendAndReceive("INVALID_DATA");
        assertEquals("ERROR;INVALID_FORMAT", invalidResponse);
    }
}
