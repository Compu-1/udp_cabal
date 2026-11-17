package client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class SensorClientTest {

    private DatagramSocket mockServerSocket;
    private int mockServerPort;
    private Thread mockServerThread;
    private volatile boolean serverRunning;

    @BeforeEach
    void setUp() throws IOException {
        mockServerSocket = new DatagramSocket(0);
        mockServerPort = mockServerSocket.getLocalPort();
        serverRunning = true;
    }

    @AfterEach
    void tearDown() {
        serverRunning = false;
        if (mockServerSocket != null && !mockServerSocket.isClosed()) {
            mockServerSocket.close();
        }
        if (mockServerThread != null) {
            try {
                mockServerThread.join(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void startMockEchoResponder(String expectedResponse) {
        mockServerThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                mockServerSocket.receive(packet);

                byte[] responseBytes = expectedResponse.getBytes(StandardCharsets.UTF_8);
                DatagramPacket responsePacket = new DatagramPacket(
                        responseBytes,
                        responseBytes.length,
                        packet.getAddress(),
                        packet.getPort()
                );
                mockServerSocket.send(responsePacket);
            } catch (IOException ignored) {
            }
        });
        mockServerThread.setDaemon(true);
        mockServerThread.start();
    }

    @Test
    @DisplayName("CL-01: Envío de telemetría y recepción exitosa de respuesta")
    void testCL01_SendTelemetrySuccessfulResponse() throws IOException {
        startMockEchoResponder("OK;TEMP_RECORDED;25.0");

        SensorClient client = new SensorClient("localhost", mockServerPort, 2000);
        String response = client.sendAndReceive("sensor-01;TEMP;25.0");

        assertEquals("OK;TEMP_RECORDED;25.0", response);
    }

    @Test
    @DisplayName("CL-02: Envío de consulta STATUS y recepción exitosa de respuesta")
    void testCL02_SendStatusQuerySuccessfulResponse() throws IOException {
        startMockEchoResponder("STATUS_OK;sensor-01;TEMP;25.0");

        SensorClient client = new SensorClient("localhost", mockServerPort, 2000);
        String response = client.sendAndReceive("STATUS;sensor-01");

        assertEquals("STATUS_OK;sensor-01;TEMP;25.0", response);
    }

    @Test
    @DisplayName("CL-03: Verificación de Timeout con SocketTimeoutException ante falta de respuesta")
    void testCL03_SocketTimeoutExceptionWhenServerDoesNotRespond() {
        // En este caso el servidor mock NO envía ninguna respuesta
        mockServerThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                mockServerSocket.receive(packet);
                // No responde intencionalmente para forzar el timeout
            } catch (IOException ignored) {
            }
        });
        mockServerThread.setDaemon(true);
        mockServerThread.start();

        // Cliente configurado con un timeout corto de 500 ms
        SensorClient client = new SensorClient("localhost", mockServerPort, 500);

        long startTime = System.currentTimeMillis();
        assertThrows(SocketTimeoutException.class, () -> {
            client.sendAndReceive("sensor-01;TEMP;25.0");
        });
        long elapsed = System.currentTimeMillis() - startTime;

        assertTrue(elapsed >= 450, "El tiempo transcurrido debe ser de al menos 450 ms por el timeout");
    }
}
