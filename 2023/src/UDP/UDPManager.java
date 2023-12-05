package UDP;
import TCP.RouteInfo;

import Structs.*;
import TCP.TCPManager;
import TCP.TCPPacket;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.ArrayList;

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
    //This will have the routing info of each node
    private HashMap<String, ArrayList<RouteInfo>> routes = new HashMap<String, ArrayList<RouteInfo>>();
    //Stream id followed by the streamId
    //private ArrayList<Integer> streams = new ArrayList<Integer>();
    //HashMap with the streamId and an arrayList of the gateways to redirect the streams
    public HashMap<Integer, ArrayList<String>> streamSockets = new HashMap<Integer, ArrayList<String>>();
    public HashMap<Integer, String>  streams = new HashMap<Integer, String>();

    private TCPManager tcpManager;

    public UDPManager(Node node, TCPManager tcpManager){
        this.node=node;
        this.tcpManager = tcpManager;
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

    public Integer addStream(String pathToFile) {
        
        try {
            //Lets generate the streamId
            Integer streamId = this.streamSockets.size();
            this.streamSockets.put(streamId, new ArrayList<String>());
            this.streams.put(streamId, pathToFile);
            return streamId;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }


    public void startStream(int StreamId, String pathToFile){
        try{

            //First generate the packets,
            //To do this we need to unhash the streamid to get the filepath
            //Then we need to read the file and generate the packets
            //Then we need to send the packets to the rp via unicast
            new Thread(() -> {
                  // packet manager criado a partir do nome do ficheiro em input em primeiro argumento
                PacketManager packetManager = new PacketManager(pathToFile);

                // criacao do socket
                DatagramSocket socket;
                try {
                    socket = new DatagramSocket();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                ArrayList<String> bestPath = getBestPath();
                String proxNode = bestPath.get(0);

                // loop nos frames do video
                for (int i = 1; i < packetManager.getNumberOfFrames(); i++){
                    //Adicionar prevenção de falhas relativamente ao path de destino(for loop)
                    byte[] frame = packetManager.getFrame(i);

                    // criacao do packet para enviar a partir dum RTPPacket
                    RTPPacket rtpPacket = new RTPPacket(StreamId, i, 10*i, frame, frame.length, bestPath.get(bestPath.size()-1));
                    byte[] packetContent = rtpPacket.getContent();

                    // envio do packet ao IP em argumento 1 na porta 4555
                    try {
                        InetAddress nodeAdd = InetAddress.getByName(proxNode);
                        DatagramPacket packet =  new DatagramPacket(packetContent, packetContent.length,nodeAdd , UDPPORT);
                        socket.send(packet);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    // esperamos algums milli segundos ate enviar de novo
                    try {
                        final int TIME_BETWEEN_FRAMES = 100; // in ms
                        Thread.sleep(TIME_BETWEEN_FRAMES);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                this.tcpManager.sendPacket(null, new TCPPacket(TCPPacket.Type.END_STREAM)); // PARA QUE GATEWAY ENVIAR ? QUAL O IP DO RP ?
                System.out.println("Sending END_STREAM packet to the rp");
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
                    //System.out.println("Received packet from: "+packet.getAddress().toString());
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

    private void rpRedirect(DatagramPacket pac){
        //The rp redirects the stream to all the nodes that want to consume the stream
        RTPPacket packet = new RTPPacket(pac.getData(), pac.getLength());
        int streamId = packet.getStreamID();
        for(String node: this.streamSockets.get(streamId)){
            System.out.println("Redirecting the stream to:"+ node);
            ArrayList<String> path = this.getBestPath(node);
            String gt = path.get(0);
            //Send the packet to the gt
            byte[] payload = packet.payload;
            RTPPacket clone = new RTPPacket(packet.getStreamID(), packet.sequenceNr, packet.timestamp, payload, packet.payload_size, node);
            System.out.println(clone.getDestIp());
            byte[] packetContent = clone.getContent();
            try{
                InetAddress nodeAdd = InetAddress.getByName(gt);  
                DatagramPacket newPacket =  new DatagramPacket(packetContent, packetContent.length,nodeAdd , UDPPORT);
                udpSocket.send(newPacket);
                System.out.println("Sent packet to: "+ gt);
            }catch(Exception e){
                e.printStackTrace();
            }

        }

    }

    public void handleResponse(DatagramPacket packet){
        try{
            if(this.node.getNodeType()==Node.type.node){
                //First we need to get the dest of the packet
                RTPPacket pac = new RTPPacket(packet.getData(), packet.getLength());
                ArrayList<String> bestPath = getBestPath(pac.getDestIp());
                String proxNode = bestPath.get(0);
                InetAddress nodeAdd = InetAddress.getByName(proxNode);  
                DatagramPacket newPacket =  new DatagramPacket(packet.getData(), packet.getLength(),nodeAdd , UDPPORT);
                udpSocket.send(newPacket);
            }
            if(this.node.getNodeType()==Node.type.rp){
                //the rp will receive the packets and redirect them
                new Thread(() -> {
                    this.rpRedirect(packet);
                }).start();
            }
            if(this.node.getNodeType()==Node.type.client){
                System.out.println("Received packet from: "+packet.getAddress().toString());
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

    public void setRoutes(HashMap<String, ArrayList<RouteInfo>> routes) {
        this.routes = routes;
    }

    private ArrayList<String> getBestPath(){
        ArrayList<String> bestPath = new ArrayList<>();
        Float bestLatency = Float.MAX_VALUE;
        for(String dest : this.routes.keySet()){
            for(RouteInfo route : this.routes.get(dest)){
                if(route.getLatency()<bestLatency){
                    bestLatency=route.getLatency();
                    bestPath=route.getPath();
                }
            }
        }
        return bestPath;
    }

    private ArrayList<String> getBestPath(String destination){
        ArrayList<String> bestPath = new ArrayList<>();
        Float bestLatency = Float.MAX_VALUE;
        if(!this.routes.containsKey(destination)){
            System.out.println("No route to destination: "+ destination);
            return bestPath;
        }
        for(RouteInfo route : this.routes.get(destination)){
            if(route.getLatency()<bestLatency){
                bestLatency=route.getLatency();
                bestPath=route.getPath();
            }
        }
        return bestPath;
    }

}