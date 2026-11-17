package service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/**
 * Servidor UDP de la Estación Base (PeerB).
 * Escucha datagramas entrantes de sensores en campo, delega el procesamiento
 * al TelemetryProcessor y responde al emisor.
 */
public class BaseStationServer {

    private final int port;
    private final TelemetryProcessor processor;
    private DatagramSocket socket;
    private volatile boolean running;
    private Thread serverThread;

    public BaseStationServer(int port) {
        this(port, new TelemetryProcessor());
    }

    public BaseStationServer(int port, TelemetryProcessor processor) {
        this.port = port;
        this.processor = processor;
    }

    /**
     * Inicia el socket UDP y el hilo de escucha en segundo plano.
     */
    public synchronized void start() throws SocketException {
        if (running) {
            return;
        }
        this.socket = new DatagramSocket(this.port);
        this.running = true;
        this.serverThread = new Thread(this::listenLoop, "BaseStationServer-Worker");
        this.serverThread.setDaemon(true);
        this.serverThread.start();
    }

    /**
     * Bucle continuo de recepción de datagramas UDP.
     */
    private void listenLoop() {
        byte[] buffer = new byte[2048];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                handlePacket(socket, packet);
            } catch (SocketException e) {
                if (!running) {
                    break;
                }
                System.err.println("[BaseStationServer] Socket cerrado o error: " + e.getMessage());
            } catch (IOException e) {
                if (running) {
                    System.err.println("[BaseStationServer] Error al recibir/procesar paquete: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Paso 2: Manejo de cada paquete UDP recibido.
     *
     * @param socket Socket UDP activo.
     * @param packet Paquete recibido del sensor.
     * @throws IOException Si ocurre un error de E/S al enviar la respuesta.
     */
    public void handlePacket(DatagramSocket socket, DatagramPacket packet) throws IOException {
        // 2. Convierta los datos recibidos a String utilizando packet.getOffset() y packet.getLength() con StandardCharsets.UTF_8
        String message = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
        System.out.println("[UDP RX] Desde " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + " -> " + message);

        // 3. Procese el mensaje con this.processor.process(...)
        String response = this.processor.process(message);

        // 4. Construya un DatagramPacket de respuesta dirigido a la dirección y puerto de origen del emisor (packet.getAddress(), packet.getPort())
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        DatagramPacket responsePacket = new DatagramPacket(
                responseBytes,
                responseBytes.length,
                packet.getAddress(),
                packet.getPort()
        );

        // 5. Envíe la respuesta con socket.send(...)
        socket.send(responsePacket);
        System.out.println("[UDP TX] Respuesta enviada -> " + response);
    }

    /**
     * Detiene el servidor y libera los recursos del socket.
     */
    public synchronized void stop() {
        this.running = false;
        if (this.socket != null && !this.socket.isClosed()) {
            this.socket.close();
        }
        if (this.serverThread != null) {
            try {
                this.serverThread.join(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public int getPort() {
        return socket != null && !socket.isClosed() ? socket.getLocalPort() : port;
    }

    public TelemetryProcessor getProcessor() {
        return processor;
    }

    public boolean isRunning() {
        return running;
    }
}
