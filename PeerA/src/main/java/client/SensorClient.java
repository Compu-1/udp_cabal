package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Cliente UDP para el nodo sensor (PeerA).
 * Envía paquetes de telemetría y consultas a la estación base con soporte de timeout.
 */
public class SensorClient {

    private final String serverHost;
    private final int serverPort;
    private final int timeoutMs;

    public SensorClient(String serverHost, int serverPort) {
        this(serverHost, serverPort, 2000);
    }

    public SensorClient(String serverHost, int serverPort, int timeoutMs) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Paso 3: Envía un mensaje UDP a la estación base y espera la respuesta con timeout.
     *
     * @param message Trama de texto a enviar codificada en UTF-8.
     * @return Respuesta recibida del servidor.
     * @throws java.net.SocketTimeoutException Si se agota el tiempo de espera configurado.
     * @throws IOException Si ocurre un error de comunicación de red.
     */
    public String sendAndReceive(String message) throws IOException {
        // 2. Abra un DatagramSocket (en un bloque try-with-resources)
        try (DatagramSocket socket = new DatagramSocket()) {
            // 3. Configure el timeout con socket.setSoTimeout(this.timeoutMs)
            socket.setSoTimeout(this.timeoutMs);

            // 4. Empaquete el mensaje en un DatagramPacket hacia InetAddress.getByName(this.serverHost) y this.serverPort
            byte[] sendBuffer = message.getBytes(StandardCharsets.UTF_8);
            InetAddress address = InetAddress.getByName(this.serverHost);
            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, this.serverPort);

            // 5. Envíe el datagrama con socket.send(...)
            socket.send(sendPacket);

            // 6. Reciba la respuesta con socket.receive(...) y decodifíquela a String respetando offset y length
            byte[] receiveBuffer = new byte[2048];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket);

            return new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength(), StandardCharsets.UTF_8);
        }
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }
}
