package UDPTeste;

import UDP.PacketManager;
import UDP.RTPPacket;

import java.io.IOException;
import java.net.*;

public class UDPTesteCliente {

    public static void main(String[] args){

        // criacao do socket para receber com uma porta de 4555
        DatagramSocket rtpSocket;
        try {
            rtpSocket = new DatagramSocket(4555);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        // criacao do packet que vai ser povoado
        byte[] buff = new byte[150000];
        DatagramPacket rcvdp = new DatagramPacket(buff, buff.length);


        while (true){
            // rececao do packet e creacao do RTPPacket com os dados recebidos
            try {
                rtpSocket.receive(rcvdp);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            RTPPacket packet = new RTPPacket(rcvdp.getData(), rcvdp.getLength());


            System.out.println("Receved packet nb : " + packet.getsequencenumber());
        }
    }

}
