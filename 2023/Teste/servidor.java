import java.net.*;
import java.io.*;
import java.awt.event.*;

public class servidor {

    private static final String rp = "10.0.1.20";
    private static String packet = "Teste";
    private static ServerSocket server = null;

    public static void sendData(String destIp){
        try {
            DatagramSocket socket = new DatagramSocket(5000);
            InetAddress ip = InetAddress.getByName(destIp);
            byte[] buffer = packet.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, ip, 5000);
            socket.send(packet);
            socket.close();
            System.out.println("Packet sent");
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    public static void main(String[] args) {
        System.out.println("Servidor");
        try{
            server = new ServerSocket(5000);
            //Thread para aceitar conexoes

            while(true){
                Socket client = server.accept();
                System.out.println("Connection accepted with ip: "+ client.getInetAddress().getHostAddress());
                //Thread para receber dados
                new Thread(() -> {
                    try{
                        sendData(client.getInetAddress().getHostAddress());
                    }
                    catch(Exception e){
                        e.printStackTrace();
                        System.out.println("Error sending data");
                    }
                });
            }
                
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Error creating server");
        }
    }  
}
