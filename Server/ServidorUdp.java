import java.net.*;
import java.net.http.*;

public class ServidorUdp {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(9998);
        byte[] buffer = new byte[4096];
        System.out.println("Servidor UDP aguardando arquivos...");

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            String recebido = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Recebido: " + recebido);

            String ok = "RECEBIDO";
            DatagramPacket resp = new DatagramPacket(ok.getBytes(), ok.length(),
                    packet.getAddress(), packet.getPort());
            socket.send(resp);

            HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/app/api/notify"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
        }
    }
}
