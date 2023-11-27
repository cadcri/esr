package UDP;

import Structs.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Random;

/*
 * UDPManager
 * The rationale is the following
 * 1. The node will have a UDP socket listening on a port
 * This unicast udp socket will be used to send the stream packets until the rp
 * The rp will receive this and then store the stream that is being sent 
 * when someone requests the stream with that streamid, the rp will start sending the stream to the node by the best path available and a unique udp multicast port
 * each intermediate node will join the following node multicast group and forward the packets
 * the client will receive the packets and play the stream
 * if a client node requests a stream that is already being streamed by an intermediate node, the intermediate node will just forward the packets to the client
 * by adding the client to the multicast group
 * this will require interaction with the tcp manager to send the streamid to the rp
 * and when the rp send the streamack back with the best path and the multicast port, the tcp manager will send the streamid to the client
 * and all the nodes that that tcp packet goes through should open a multicast udp socket and join the multicast group
 * and forward the packets
 */


public class UDPManager{

    private DatagramSocket udpSocket;
    private int UDPPORT = 4445;
    private Node node;
    private Boolean connected=false;
    private HashMap<Integer, Integer> ports = new HashMap<Integer, Integer>();
    //Stream id followed by the multicasSocket
    private HashMap<Integer, MulticastSocket> streams = new HashMap<Integer, MulticastSocket>();


    public UDPManager(Node node){
        this.node=node;
        try{
            this.initUnicast();
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Error creating UDP socket");
        }
    }


    public void initUnicast() {
        try {
            this.udpSocket = new DatagramSocket(UDPPORT);
            System.out.println("UDP socket listening on port " + UDPPORT);
            connected = true;
            new Thread( () -> {
                receivePacket();
            } ).start();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error creating UDP socket");
        }
    }

    public Integer[] addStream(String pathToFile) {
        
        try {
            int port = new Random().nextInt(65535 - 49152) + 49152;
            while(this.ports.containsValue(port)){
                port = new Random().nextInt(65535 - 49152) + 49152;
            }
            MulticastSocket multicastSocket = new MulticastSocket(port);
            int streamId = this.streams.size();
            this.streams.put(streamId, multicastSocket);
            this.ports.put(streamId, port);
            Integer[] ret = {streamId, port};
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error creating multicast socket");
            return null;
        }

    }

    public void joinStream(int streamId, int multicastPort) {
        if (streams.containsKey(streamId)) {
            try {
                MulticastSocket multicastSocket = streams.get(streamId);
                //Fix
                InetAddress multicastGroup = InetAddress.getByName("225.0.0.1"); // Replace with the actual multicast group address

                multicastSocket.joinGroup(multicastGroup);
                multicastSocket.connect(multicastGroup, multicastPort);

                System.out.println("Joined multicast stream for streamId: " + streamId);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error joining multicast stream");
            }
        } else {
            System.out.println("Stream does not exist");
        }
    }

    public void startStream(int StreamId, String pathToFile, ArrayList<String> path){
        try{

            //First generate the packets,
            //To do this we need to unhash the streamid to get the filepath
            //Then we need to read the file and generate the packets
            //Then we need to send the packets to the rp via unicast
            new Thread(() -> {
                

                //Generate the packets
                PacketManager packetManager;
                //threading this so the program isnt in halt while waiting for the packets and can continue encoding the path
                packetManager = new PacketManager(pathToFile);


                String pathEnc = "";
                for(String s: path){
                    pathEnc += s + ";";
                }

                System.out.println("Packets: "+ packetManager.getNumberOfFrames());
                System.out.println("Path encoded: "+pathEnc);
                //first encode the path for the packets to follow
                //Send the packets to the rp
                
            }).start();
        }catch(Exception e){
            e.printStackTrace();
            System.out.println("Error streaming the packets to the rp");
        }
    }


    public void receivePacket(){
        try{
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
            e.printStackTrace();
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