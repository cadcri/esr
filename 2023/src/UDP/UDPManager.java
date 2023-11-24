package UDP;

import Structs.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;





public class UDPManager{

    private DatagramSocket udpSocket;
    private int UDPPORT = 4445;
    private Node node;
    private Boolean connected=false;
    private HashMap<String, MulticastSocket> multicastSockets = new HashMap<String, MulticastSocket>();


    public UDPManager(Node node, String port){
        this.node=node;
        this.UDPPORT = Integer.parseInt(port);
        try{
            this.initUnicast();
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Error creating UDP socket");
        }
    }


    public void initUnicast(){
        try{
            this.udpSocket = new DatagramSocket(UDPPORT);
            System.out.println("UDP socket listening on port "+UDPPORT);
            connected=true;
            receivePacket();
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Error creating UDP socket");
        }
    }

    public void joinMulticast(String ip){
        try{
            MulticastSocket multicastSocket = new MulticastSocket(UDPPORT);
            if(!multicastSockets.containsKey(ip)){
                multicastSocket.joinGroup(InetAddress.getByName(ip));
                multicastSockets.put(ip, multicastSocket);
                System.out.println("Joined multicast group: "+ip);
                receiveMulticastPacket(ip);
            }
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Error joining multicast group");
        }
    }

    public void receiveMulticastPacket(String ip){
        try{
            new Thread(() -> {
                while(connected){
                    try{
                        byte[] buffer = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        multicastSockets.get(ip).receive(packet);
                        System.out.println("Received packet from: "+packet.getAddress().toString());
                        new Thread(() -> {
                            handleResponse(packet);
                        }).start();
                    }
                    catch(Exception e){
                        e.printStackTrace();
                        System.out.println("Error receiving UDP packet");
                    }
                }
            }).start();
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Error receiving UDP packet");
        }
    }


    public void receivePacket(){
        try{
            new Thread(() -> {
                while(connected){
                    try{
                        byte[] buffer = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        udpSocket.receive(packet);
                        System.out.println("Received packet from: "+packet.getAddress().toString());
                        new Thread(() -> {
                            handleResponse(packet);
                        }).start();
                    }
                    catch(Exception e){
                        e.printStackTrace();
                        System.out.println("Error receiving UDP packet");
                    }
                }
            }).start();
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Error receiving UDP packet");
        }
    }

    public void handleResponse(DatagramPacket packet){
        try{
            if(this.node.getNodeType()==Node.type.node){
                //retransmit();
            }
            else{
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println(received);
            }

        }
        catch(Exception e){
            e.printStrackTrace();
            System.out.println("An error has ocured while handling UDP response");
        }
    }


    public void closeSocket(){
        try{
            connected=false;
            udpSocket.close();
            return;
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Error closing UDP socket");
        }
    }

}