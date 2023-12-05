package UDPTeste;

import UDP.PacketManager;
import UDP.RTPPacket;

import java.io.IOException;
import java.net.*;

public class UDPTesteServer {

    public static void main(String[] args){

        // packet manager criado a partir do nome do ficheiro em input em primeiro argumento
        PacketManager packetManager = new PacketManager(args[0]);

        // criacao do socket
        DatagramSocket socket;
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        // loop nos frames do video
        for (int i = 1; i < packetManager.getNumberOfFrames(); i++){
            byte[] frame = packetManager.getFrame(i);

            // criacao do packet para enviar a partir dum RTPPacket
            RTPPacket rtpPacket = new RTPPacket(0, i, 10*i, frame, frame.length, "");
            byte[] packetContent = rtpPacket.getContent();

            // envio do packet ao IP em argumento 1 na porta 4555
            try {
                DatagramPacket packet =  new DatagramPacket(packetContent, packetContent.length, InetAddress.getByName(args[1]), 4555);
                socket.send(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // esperamos algums milli segundos ate enviar de novo
            try {
                final int TIME_BETWEEN_FRAMES = 100; // in ms
                Thread.sleep(TIME_BETWEEN_FRAMES);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}