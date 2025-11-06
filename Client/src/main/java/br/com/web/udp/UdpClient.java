package br.com.web.udp;

import java.net.*;

public class UdpClient {
    public void send(byte[] data) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress addr = InetAddress.getByName("192.168.1.16");
            DatagramPacket packet = new DatagramPacket(data, data.length, addr, 9998);
            socket.send(packet);

            byte[] resp = new byte[1024];
            DatagramPacket respPacket = new DatagramPacket(resp, resp.length);
            socket.receive(respPacket);
            System.out.println("Resposta UDP: " +
                new String(respPacket.getData(), 0, respPacket.getLength()));
        } catch (Exception e) { e.printStackTrace(); }
    }
}
