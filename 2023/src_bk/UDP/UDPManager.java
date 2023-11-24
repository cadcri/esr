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

    public UDPManager(Node node){
        this.node = node;
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

    public void sendPacket(Message message, String ip){
        try{
            System.out.println("Sending packet to: "+ip);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(outputStream);
            os.writeObject(message);
            byte[] buffer = outputStream.toByteArray();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(ip), UDPPORT);
            System.out.println(packet.toString());
            udpSocket.send(packet);        
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Error sending UDP packet");
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
            String received = new String(packet.getData(), 0, packet.getLength());
            //Deserialize the packet
            ByteArrayInputStream in = new ByteArrayInputStream(packet.getData());
            ObjectInputStream is = new ObjectInputStream(in);
            try {
                Message message = (Message) is.readObject();

                String senderIp = packet.getAddress().toString();

                switch(message.getTipo()){
                    case JOIN:
                        System.out.println("Received join message");
                        //Always send the ack 
                        this.sendPacket(new Message(Message.Type.JOIN_ACK), senderIp.replace("/",""));
                        // //if it is the RP then add the path to the routes 
                        // if(this.node.getNodeType()==Node.type.rp){
                        //     this.node.addRoute(message.getPath());
                        // }
                        // else{
                        //     //else send to all the neighbors except the one that sent the message
                        //     for(Node neighbor: this.node.getNeighbors().values()){
                        //         if(!neighbor.getNodeIP().equals(senderIp)){
                        //             //add the current node to the message path
                        //             message.addNodeToPath(senderIp);

                        //             this.sendPacket(message, neighbor.getNodeIP());
                        //         }
                        //     }
                        // }

                        break;
                }


            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Error handling UDP packet");
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