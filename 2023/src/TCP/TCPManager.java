
package TCP;

import java.io.*;
import java.net.*;
import Structs.*;
import UDP.UDPManager;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;

//  * The TCPManager class handles the management of TCP connections and communication.
//  * It provides methods for creating listening and writing sockets, sending packets,
//  * and handling incoming packets.
//  */


//Probably add a class gateway to store the gateway and the latency to it and the status of it

class RouteInfo {
    private String dest;
    private Float latency;
    private ArrayList<String> path;

    public RouteInfo(String dest, Float latency, ArrayList<String> path) {
        this.dest = dest;
        this.latency = latency;
        this.path = path;
    }

    public String getDest() {
        return this.dest;
    }

    public Float getLatency() {
        return this.latency;
    }

    public ArrayList<String> getPath() {
        return this.path;
    }

    public void setLatency(Float latency) {
        this.latency = latency;
    }

    public String getDestination() {
        return this.dest;
    }
}

public class TCPManager {

    private ServerSocket listeningSocket;
    private Node node;
    private int tcpDftPort = 44000;
    private UDPManager udpManager;


    // Dest node and the latencies to it with the path it follows
    private HashMap<String, ArrayList<RouteInfo>> routes = new HashMap<>();
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private Lock gatewaysLock = new ReentrantLock();

    public TCPManager(Node node, UDPManager udpManager) throws IOException {
        this.node = node;
        this.udpManager = udpManager;
        new Thread(() -> {
            createListening(); // Call the method asynchronously
        }).start();

        executor.scheduleAtFixedRate(() -> {
            //System.out.println("Updating paths");
            probeRoutes();
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void createListening() {
        try {
            System.out.println("Creating read socket");
            this.listeningSocket = new ServerSocket(tcpDftPort);
            read();
            System.out.println("Listening socket created");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error creating listening socket");
            System.exit(1);
        }
    }

    public void sendPacket(String gateway, TCPPacket packet) {
        try {
            gatewaysLock.lock();
            try {
                
                if (gateway.equals("")){
                    System.out.println("Setting bestPath");
                    ArrayList<String> bestPath = this.getBestPath();
                    packet.setOutgoingPath(bestPath);
                    gateway=bestPath.get(0);
                }
                Socket socket = new Socket(gateway, tcpDftPort);
                String serializedPacket = packet.serializa();
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                writer.println(serializedPacket);
                writer.flush();
                socket.close();

            } finally {
                gatewaysLock.unlock();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error sending packet: " + packet);
        }
    }


    public void read() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket clientSocket = this.listeningSocket.accept();
                    new Thread(() -> {
                        try {
                            this.handleReponseMiddle(clientSocket);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }).start();

                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Something went wrong accepting connections(Server Socket)");
                    System.exit(1);
                }
            }
            // System.out.println("Socket is disconnected");
        }).start();
    }

    private void handleReponseMiddle(Socket socket) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String packet = reader.readLine();
        if (packet != null) {
            InetAddress clientAddress = socket.getInetAddress();
            String clientIP = clientAddress.getHostAddress();
            TCPPacket pacote = TCPPacket.deserialize(packet);
            this.handleResponse(pacote, clientIP);
        }
    }

    private void handleResponse(TCPPacket packet, String clientIP) {
        switch (packet.getType()) {
            case PROBE:
                handleProbe(packet, clientIP);
                break;
            
            case PROBE_ACK:
                handleProbeAck(packet, clientIP);
                break;

            case JOIN:
            case JOIN_ACK:
                vascoDaGama(packet, clientIP);
                break;

            case LEAVE:
                System.out.println("Leave request received");
                // Send the leave ack to the gateway
                // Set this gateway as inactive
                break;
            case STREAM:
                handleStream(packet, clientIP);
                break;
            case STREAM_ACK:
                handleStreamAck(packet, clientIP);
                break;

            case REQUEST_STREAM:
                break;

            case REQUEST_STREAM_ACK:
                break;

            case STREAM_END:
                break;

            case STREAM_END_ACK:
                break;


            case LEAVE_ACK:
                System.out.println("Leaving");
                // Close all sockets
                // Go to the main screen so it can become active once again
                break;
        }
    }

    private void vascoDaGama(TCPPacket packet, String clientIP) {
        try {
            switch (packet.getType()) {
                case JOIN:
                    handleJoin(packet, clientIP);
                    break;

                case JOIN_ACK:
                    handleJoinAck(packet, clientIP);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleJoin(TCPPacket packet, String ClientIP) {
        if (this.node.getNodeType() == Node.type.rp) {
            // If it is the rp, it doesn't redirect the packet to the neighboring nodes, it sends the ack packet
            // The ack packet should have the initial timestamp
            packet.addToIncomingPath(ClientIP);
            System.out.println("Join request received from: " + packet.getSrc());
            System.out.println("Path from me: " + packet.getIncomingPath().toString());
            TCPPacket clone = packet.clone();
            
            clone.setType(TCPPacket.Type.JOIN_ACK);
            this.sendPacket(ClientIP, clone);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime packetInitTime = packet.getTimeStampInit();

            Float latency = (float) (packetInitTime.getNano()-now.getNano()) / 1000000;

            // Create the route info
            RouteInfo route = new RouteInfo(packet.getSrc(), latency, packet.getIncomingPath());
            addRoute(route, packet.getSrc());

        } else {
            // Send a clone of the packet to all of the neighboring nodes
            // It should send to all of the neighboring nodes except the one it came from
            if (packet.getSrc() == null || packet.getSrc().equals("") || packet.getSrc().equals("null")) {
                packet.setSrc(ClientIP);
            }
            for (String neighbourGateway : this.node.getGateways()) {
                TCPPacket clone = packet.clone();
                if (!ClientIP.equals(neighbourGateway)) {
                    clone.addToIncomingPath(ClientIP);
                    clone.addToOutgoingPath(neighbourGateway);
                    // Add the current node
                    this.sendPacket(neighbourGateway, clone);
                }
            }

        }
    }

    private void handleJoinAck(TCPPacket packet, String clientIP) {
        if (clientIP.equals(packet.getOutgoingPath().get(0))) {
            // This is the node that tried to join
            packet.setTimeStampEnd();
            Float latency = packet.getLatency();
            ArrayList<String> temp = packet.getOutgoingPath();
            RouteInfo routeInfo = new RouteInfo(packet.getSrc(), latency/2, temp);
            // add this path
            addRoute(routeInfo, packet.getSrc());
        } else {
            for (String neighbourGateway : this.node.getGateways()) {
                if (packet.getIncomingPath().contains(neighbourGateway)) {
                    // I want to redirect the packet to the next node in the path
                    int index = packet.getIncomingPath().indexOf(neighbourGateway);
                    this.sendPacket(packet.getIncomingPath().get(index), packet);
                }
            }
        }
    }

    private void handleStream(TCPPacket packet, String clientIP) {
        // Handle the stream packet
        if(this.node.getNodeType()==Node.type.rp){
            //send the stream ack

            TCPPacket clone = packet.clone();
            clone.setType(TCPPacket.Type.STREAM_ACK);
            
            ArrayList<String> bestPath = this.getBestPath(packet.getSrc());
            if(bestPath.size()==0){
                System.out.println("No route to destination");
                return;
            }

            //Start the multicastSocket
            Integer[] stream = this.udpManager.addStream(packet.getPathToFile());
            if(stream==null){
                System.out.println("Error creating multicast socket");
                return;
            }

            clone.setIncomingPath(bestPath);
            clone.setStreamId(stream[0]);
            clone.setStreamPort(stream[1]);
            this.sendPacket(clientIP, clone);
            
        }
        else{
            //send the stream to the next node
            //Start the multicast in each node the packet goes through and 
            //Join the multicastgroup from the port the rp just set
            if (packet.getSrc() == null || packet.getSrc().equals("") || packet.getSrc().equals("null")) {
                packet.setSrc(clientIP);
            }
            for (String neighbourGateway : this.node.getGateways()) {
                if (packet.getOutgoingPath().contains(neighbourGateway)) {
                    // I want to redirect the packet to the next node in the path
                    int index = packet.getOutgoingPath().indexOf(neighbourGateway);
                    this.sendPacket(packet.getOutgoingPath().get(index), packet);
                }
            }
        }
    }

    private void handleStreamAck(TCPPacket packet, String clientIP) {
        // Handle the stream acknowledgment packet
        if (clientIP.equals(packet.getOutgoingPath().get(0))) {
            //Start the unicast streaming of the stream packets to the rp
            System.out.println("Starting the stream: "+packet.getStreamId()+ " path to file: "+packet.getPathToFile());
            this.udpManager.startStream(packet.getStreamId(), packet.getPathToFile(), packet.getOutgoingPath());
        }
        else{
            //send the stream ack to the next node
            for (String neighbourGateway : this.node.getGateways()) {
                if (packet.getIncomingPath().contains(neighbourGateway)) {
                    // I want to redirect the packet to the next node in the path
                    int index = packet.getIncomingPath().indexOf(neighbourGateway);
                    this.sendPacket(packet.getIncomingPath().get(index), packet);
                    
                    //Open the port to receive the stream
                }
            }
        }
    }

    private void probeRoutes(){
        //Only the rp executes this
        //For every route it exists
        //Send a probe packet to the destination
        //Set the route status as offline
        //If the rp receives a probe ack from the destination
        //Set the route status as online
        //Update the latency
        if(this.node.getNodeType()==Node.type.rp){
            for(String dest : this.routes.keySet()){
                for(RouteInfo route : this.routes.get(dest)){
                    //Send the probe packet
                    TCPPacket packet = new TCPPacket(TCPPacket.Type.PROBE);
                    ArrayList<String> path = new ArrayList<String>(route.getPath());
                    String firstNode = path.remove(path.size()-1);
                    packet.setOutgoingPath(path);
                    this.sendPacket(firstNode, packet);
                    //Set the path status as offline
                }
            }
        }
    }

    private void handleProbe(TCPPacket packet, String clientIP){
        //Only the rp send this packet to the routes he has
        //The other nodes just forward the packet
        //when the packet reaches the destination node he should update the latency and send it back
        //the rp should update the latency of the route
        if(packet.getOutgoingPath().size()==packet.getIncomingPath().size()){
            //This is the destination node of the probe
            //Send the probe ack
            packet.addToIncomingPath(clientIP);
            TCPPacket clone = packet.clone();
            clone.setType(TCPPacket.Type.PROBE_ACK);
            ArrayList<String> path = new ArrayList<String>(packet.getIncomingPath());
            String firstNode = path.remove(path.size()-1);
            clone.setIncomingPath(path);
            this.sendPacket(firstNode, clone);
            //Update the latency of the route
            for(String dest : this.routes.keySet()){
                for(RouteInfo route : this.routes.get(dest)){
                    //we should compare the route, if it is the same we update the latency
                    if(route.getPath().equals(packet.getIncomingPath())){
                        route.setLatency(packet.getLatency());
                        System.out.println("Found the route");
                    }
                }
            }
        }
        else{
            if(packet.getSrc() == null || packet.getSrc().equals("") || packet.getSrc().equals("null")){
                packet.setSrc(clientIP);
            }
            //Forward the packet
            for (String neighbourGateway : this.node.getGateways()) {
                if (packet.getOutgoingPath().contains(neighbourGateway)) {
                    // I want to redirect the packet to the next node in the path
                    //Update the incoming path of the packet
                    int index = packet.getOutgoingPath().indexOf(neighbourGateway);
                    packet.addToIncomingPath(clientIP);
                    this.sendPacket(packet.getOutgoingPath().get(index), packet);
                }
            }
        }
    }

    private void handleProbeAck(TCPPacket packet, String clientIP) {
        // Handle the probe ack packet
        if (this.node.getNodeType() == Node.type.rp) {
            //Get the new latency
            System.out.println("Here, "+packet.getLatency());
            float latency = packet.getLatency();
        
            //Update the latency of the route
            for(String dest : this.routes.keySet()){
                for(RouteInfo route : this.routes.get(dest)){
                    //we should compare the route, if it is the same we update the latency
                    if(route.getPath().equals(packet.getOutgoingPath())){
                        route.setLatency(latency);
                    }
                }
            }

        }
        else{
            //send the stream ack to the next node
            for (String neighbourGateway : this.node.getGateways()) {
                if (packet.getIncomingPath().contains(neighbourGateway)) {
                    // I want to redirect the packet to the next node in the path
                    int index = packet.getIncomingPath().indexOf(neighbourGateway);
                    this.sendPacket(packet.getIncomingPath().get(index), packet);
                }
            }
        }
    }





    private void addRoute(RouteInfo route, String destination) {
        if (!this.routes.containsKey(destination)) {
            this.routes.put(destination, new ArrayList<>());
        }
        Boolean exists=false;;
        //check if the route is already there
        exists = compareRoutes(route);
        if(exists){
            this.routes.get(destination).remove(route);
        }
        this.routes.get(destination).add(route);

        
    }

    private boolean compareRoutes(RouteInfo route) {
        // Compare the route with existing routes
        for (ArrayList<RouteInfo> routeList : routes.values()) {
            for (RouteInfo existingRoute : routeList) {
                if (existingRoute.getDestination().equals(route.getDestination())) {
                    // Compare the nodes in the route
                    if (existingRoute.getPath().size() == route.getPath().size()) {
                        boolean sameNodes = true;
                        for (int i = 0; i < existingRoute.getPath().size(); i++) {
                            if (!existingRoute.getPath().get(i).equals(route.getPath().get(i))) {
                                sameNodes = false;
                                break;
                            }
                        }
                        if (sameNodes) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
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


    public void listRoutes(){
        
        for(String dest : this.routes.keySet()){
            System.out.println("Available routes: "+this.routes.get(dest).size());
            System.out.println("Destination: " + dest);
            for(RouteInfo route : this.routes.get(dest)){
                System.out.println("Latency: " + route.getLatency());
                System.out.println("Path: " + route.getPath().toString());
            }
        }
    }

    //Probe neighbours should be probepaths and probe if the path is still up
    // private void probeNeighbors() {
    // // For each neighbor, send a probe packet and measure the time it takes for a response to arrive
    // for (Map.Entry<String, Socket> entry : gateways.entrySet()) {
    //     String neighbor = entry.getKey();
    //     //Socket socket = entry.getValue();
    //     Socket socket = new Socket(neighbor, tcpDftPort);
    //     try {
    //         // Send a probe packet
    //         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
    //         out.println("PROBE");

    //         // Record the current time
    //         long startTime = System.currentTimeMillis();

    //         // Wait for a response
    //         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    //         String response = in.readLine();

    //         // If the response is a probe acknowledgement, calculate the latency
    //         if (response != null && response.equals("PROBE_ACK")) {
    //             long endTime = System.currentTimeMillis();
    //             float latency = (endTime - startTime) / 1000.0f;

    //             // Update the 'paths' HashMap with the new latency information
    //             // HashMap<Float, RouteInfo> routeInfo = paths.getOrDefault(neighbor, new HashMap<>());
    //             // routeInfo.put(latency, new RouteInfo(Arrays.asList(neighbor), latency));
    //             // paths.put(neighbor, routeInfo);
    //             RouteInfo route = new RouteInfo();
    //             addRoute(, neighbor);
    //         }
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    //     finally{
    //         socket.close();
    //     }
    // }}


}



// private void handleJoinAck(TCPPacket packet, String clientIP){
//          if (clientIP.equals(packet.getOutgoingPath().get(0))) {
//             // This is the node that tried to join
//             System.out.println("I was the node that joined");
//             packet.setEndTimeStamp();
//             Float latency = packet.getLatency();
//             RouteInfo routeInfo = new RouteInfo(packet.getSrc(), latency, packet.getIncomingPath());
//             //add this path
//             addRoute(routeInfo, packet.getSrc());
//         } else {
//             for (String neighbourGateway : this.node.getGateways()) {
//                 if (packet.getIncomingPath().contains(neighbourGateway)) {
//                     // I want to redirect the packet to the next node in the path
//                     int index = packet.getIncomingPath().indexOf(neighbourGateway);
//                     System.out.println("Sending the packet to: " + packet.getIncomingPath().get(index));
//                     this.sendPacket(packet.getIncomingPath().get(index), packet);
//                 }
//             }
//         }
//     }