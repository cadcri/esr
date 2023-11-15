
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPManager{

    private enum Type{
        PROBE,
        PROBE_ACK,
        ACK,
        JOIN,
        JOIN_ACK,
        LEAVE,
        LEAVE_ACK,
        STREAM
    }

    private DatagramSocket udpSocket;
    private int UDPPORT = 4445;
    private Node node;

    public UDPManager(Node node){
        this.node = node;
        try{
            this.udpSocket = new DatagramSocket(UDPPORT);
            init();
            // new Thread(() -> {
            //     while(true){
            //         System.out.println("UDP socket status: "+udpSocket.isConnected());
            //         try{
            //             Thread.sleep(100);
            //         }
            //         catch(Exception e){
            //             e.printStackTrace();
            //         }
    
            //     }
            // }).start();
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Error creating UDP socket");
        }
    }


    public void init(){
        System.out.println("UDP socket listening on port "+UDPPORT);
        receivePacket();
    }

    public void sendPacket(String message, String ip, int port){
        try{
            System.out.println("Sending packet to: "+ip+":"+port);
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(ip), port);
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
                while(true){
                    try{
                        byte[] buffer = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        udpSocket.receive(packet);
                        System.out.println("Received packet from: "+packet.getAddress().toString()+ " with:"+new String(packet.getData()));
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
            System.out.println("Received UDP message: "+received);
            if(received.equals(Type.PROBE.toString())){
                //Send ACK
                String senderIP = packet.getAddress().getHostAddress();
                System.out.println("Received message from: " + senderIP);
                for(Node nodo: this.node.getNeighbors().values()){
                    if(nodo.getNodeIP().equals(senderIP)){
                        nodo.setNodeState(Node.state.on);
                        System.out.println("Node "+nodo.getNodeName()+" is on");
                    }
                }   
                byte[] buf = new byte[1024];
                buf = Type.ACK.toString().getBytes();
                packet = new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort());
                this.udpSocket.send(packet);
            }
            else if(received.equals(Type.ACK.toString())){
                 // Get the IP of the sender
                String senderIP = packet.getAddress().getHostAddress();
                System.out.println("Received message from: " + senderIP);
                for(Node nodo: this.node.getNeighbors().values()){
                    if(nodo.getNodeIP().equals(senderIP)){
                        nodo.setNodeState(Node.state.on);
                        System.out.println("Node "+nodo.getNodeName()+" is on");
                    }
                }   
            }
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Error handling UDP packet");
        }
    }


    public void closeSocket(){
        try{
            udpSocket.close();
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Error closing UDP socket");
        }
    }

}