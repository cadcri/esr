
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



public class TCPManager {

    private ServerSocket listeningSocket;
    private Node node;
    private int tcpDftPort = 44000;
    private UDPManager udpManager;
    public Boolean streamsUpdated = false;


    // Dest node and the latencies to it with the path it follows
    private HashMap<String, ArrayList<RouteInfo>> routes = new HashMap<>();
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    public HashMap<Integer, String>  streams = new HashMap<Integer, String>();


    private Lock gatewaysLock = new ReentrantLock();

    public TCPManager(Node node, UDPManager udpManager) throws IOException {
        this.node = node;
        this.udpManager = udpManager;
        this.udpManager.setRoutes(this.routes);
        new Thread(() -> {
            createListening(); // Call the method asynchronously
        }).start();

        executor.scheduleAtFixedRate(() -> {
            //System.out.println("Updating paths");
            //probeRoutes();
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
                if(gateway==null){
                    ArrayList<String> bestPath = this.getBestPath();
                    gateway = bestPath.get(0);
                    packet.setDest(bestPath.get(bestPath.size()-1));
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
                handleRequestStream(packet, clientIP);
                break;

            case REQUEST_STREAM_ACK:
                //Handle this
                break;

            case STREAM_END:
                //Set this up
                break;

            case STREAM_END_ACK:
                //Set this up
                break;
            
            case LIST_STREAMS:
                handleListStreams(packet, clientIP);
                break;
            
            case LIST_STREAMS_ACK:
                handleListStreamsAck(packet, clientIP);
                break;


            case LEAVE_ACK:
                System.out.println("Leaving");
                // Close all sockets
                // Go to the main screen so it can become active once again
                break;
        }
    }

    private void handleListStreamsAck(TCPPacket packet, String clientIp){
        if(this.node.getNodeType()==Node.type.client){
            ArrayList<String> streams = packet.getStreams();
            this.streams=new HashMap<Integer, String>();
            for(int i=0; i<streams.size(); i++){
                this.streams.put(i, streams.get(i));
            }
            this.streamsUpdated=true;
        }
        else{
            //Forward the packet
            if (packet.getSrc() == null || packet.getSrc().equals("") || packet.getSrc().equals("null")) {
                packet.setSrc(clientIp);
            }
            String dest = packet.getDest();
            ArrayList<String> bestPath = this.getBestPath(dest);
            this.sendPacket(bestPath.get(bestPath.size()-1), packet);
            System.out.println("Packet sent to "+ bestPath.get(bestPath.size()-1));
        }
    }


    private void handleListStreams(TCPPacket packet, String clientIP){
        //Forward the packet
        if (packet.getSrc() == null || packet.getSrc().equals("") || packet.getSrc().equals("null")) {
            packet.setSrc(clientIP);
        }
        //This is only executed by the rp
        //Send the list of streams to the client
        if(this.node.getNodeType()==Node.type.rp){
            System.out.println("Got request for the list of streams");
            TCPPacket ack = new TCPPacket(TCPPacket.Type.LIST_STREAMS_ACK);
            ack.setDest(packet.getSrc());
            System.out.println("Streams: "+ this.udpManager.streams.values());
            ack.setStreams(new ArrayList<String>(this.udpManager.streams.values()));
            ArrayList<String> path = this.getBestPath(packet.getSrc());
            System.out.println("Sending to "+ path.get(path.size()-1));
            this.sendPacket(path.get(path.size()-1), ack);
        }
        else{
            System.out.println("Rello");
            String dest = packet.getDest();
            ArrayList<String> bestPath = this.getBestPath(dest);
            this.sendPacket(bestPath.get(0), packet);
        }
    }

    private void handleRequestStream(TCPPacket packet, String clientIP){
        //First we have to verify if our node is already redirecting the packages
        //If so we just create a new thread and start redirecting  UDP packets to it
        //If not we have to forward this packet until it reaches the rp

        if (packet.getSrc() == null || packet.getSrc().equals("") || packet.getSrc().equals("null")) {
            packet.setSrc(clientIP);
        }
        if(this.node.getNodeType()==Node.type.node){
            System.out.println("Got request for a stream: "+packet.getStreamId());
            if(this.udpManager.streamSockets.containsKey(packet.getStreamId())){
                System.out.println("Stream already running");
                //We are already redirecting the packets
                //We just have to send the ack
                TCPPacket ack = new TCPPacket(TCPPacket.Type.REQUEST_STREAM_ACK);
                ack.setStreamId(packet.getStreamId());
                ack.setDest(packet.getSrc());

                ArrayList<String> path = this.getBestPath(packet.getSrc());
                ack.setOutgoingPath(path);
                this.sendPacket(path.get(0), ack);

                //add this "pipeline" to the stream
                //*TODO* Verify this
                this.udpManager.streamSockets.get(packet.getStreamId()).add(clientIP);
            }
            else{
                String dest = packet.getDest();
                ArrayList<String> bestPath = this.getBestPath(dest);
                this.sendPacket(bestPath.get(0), packet);
                System.out.println("Packet sent to "+ bestPath.get(0));
            }
        }
        if(this.node.getNodeType()==Node.type.rp){
            if(this.udpManager.streamSockets.containsKey(packet.getStreamId())){
                System.out.println("Stream already running");
                //We are already redirecting the packets
                //We just have to send the ack
                TCPPacket ack = new TCPPacket(TCPPacket.Type.REQUEST_STREAM_ACK);
                ack.setStreamId(packet.getStreamId());
                ack.setDest(packet.getSrc());

                ArrayList<String> path = this.getBestPath(packet.getSrc());
                ack.setOutgoingPath(path);
                this.sendPacket(path.get(0), ack);

                //add this "pipeline" to the stream
                this.udpManager.streamSockets.get(packet.getStreamId()).add(clientIP);
            }
            else{
                //Start the streaming process
                System.out.println("Starting the stream to the node that requested it: "+ packet.getSrc());
                //this.udpManager.streamSockets.put(packet.getStreamId(), new ArrayList<>());
                this.udpManager.streamSockets.get(packet.getStreamId()).add(packet.getSrc());
            }
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
        // Send a clone of the packet to all of the neighboring nodes
        // It should send to all of the neighboring nodes except the one it came from
        if (packet.getSrc() == null || packet.getSrc().equals("") || packet.getSrc().equals("null")) {
            packet.setSrc(ClientIP);
        }
        if (this.node.getNodeType() == Node.type.rp) {
            // If it is the rp, it doesn't redirect the packet to the neighboring nodes, it sends the ack packet
            // The ack packet should have the initial timestamp
            packet.addToIncomingPath(ClientIP);
            System.out.println("Join request received from: " + packet.getSrc());
            System.out.println("Path from me: " + packet.getIncomingPath().toString());
            
            ArrayList<String> routeToMe = packet.getOutgoingPath();
            ArrayList<String> routeToDest = packet.getIncomingPath();

            TCPPacket returnPacket = new TCPPacket(TCPPacket.Type.JOIN_ACK);
            
            returnPacket.setOutgoingPath(routeToMe);
            returnPacket.setIncomingPath(routeToDest);
            returnPacket.setTimeStampInit(LocalDateTime.now());
            returnPacket.setSrc(null);

            //Rever este routing
            // Send the packet back to the client
            this.sendPacket(ClientIP, returnPacket);
            Float latency = Math.abs(packet.getLatency());
            // Create the route info
            RouteInfo route = new RouteInfo(packet.getSrc(), latency, packet.getIncomingPath());
            addRoute(route, packet.getSrc());

        } else {

            for (String neighbourGateway : this.node.getGateways()) {
                TCPPacket clone = packet.clone();
                if (!ClientIP.equals(neighbourGateway)) {
                    clone.addToIncomingPath(ClientIP);
                    clone.addToOutgoingPath(neighbourGateway);
                    // Add the current node
                    this.sendPacket(neighbourGateway, clone);
                }
            }

            if(this.node.getNodeType() == Node.type.node){
                //This adds the routes to the node in order to know the best path
                //This is the route to the node
                TCPPacket clone = packet.clone();
                clone.addToIncomingPath(ClientIP);
                System.out.println("Route Join"+ clone.getIncomingPath().toString()); 
                String src = clone.getSrc();
                float latency = clone.getLatency();
                RouteInfo route = new RouteInfo(src, Math.abs(latency), new ArrayList<>(clone.getIncomingPath()));
                this.addRoute(route, clone.getSrc());
            }

        }
    }

    private void handleJoinAck(TCPPacket packet, String clientIP) {
        // Send a clone of the packet to all of the neighboring nodes
        // It should send to all of the neighboring nodes except the one it came from
        if (packet.getSrc() == null || packet.getSrc().equals("") || packet.getSrc().equals("null")) {
            packet.setSrc(clientIP);
        }
        if (clientIP.equals(packet.getOutgoingPath().get(0))) {
            // This is the node that tried to join
            packet.setTimeStampEnd();
            Float latency = packet.getLatency();
            ArrayList<String> temp = packet.getOutgoingPath();
            RouteInfo routeInfo = new RouteInfo(packet.getSrc(), Math.abs(latency), temp);
            // add this path
            addRoute(routeInfo, packet.getSrc());
            this.node.estado = Node.state.on;
        } else {

            for (String neighbourGateway : this.node.getGateways()) {
                if (packet.getIncomingPath().contains(neighbourGateway)) {
                    // I want to redirect the packet to the next node in the path
                    int index = packet.getIncomingPath().indexOf(neighbourGateway);
                    this.sendPacket(packet.getIncomingPath().get(index), packet);
                }
            }

            if(this.node.getNodeType() == Node.type.node){
                //I only want to know the route from me to rp and from the 
                //This adds the routes to the node in order to know the best path
                TCPPacket clone = packet.clone();
                RouteInfo route;
                //System.out.println("Route Ack"+ clone.getOutgoingPath().toString()); 
                for(String gateways: this.node.getGateways()){
                    if(clone.getOutgoingPath().contains(gateways)){
                        int indexOfMyGateway = clone.getOutgoingPath().indexOf(gateways);
                        //System.out.println("Path "+ clone.getOutgoingPath().toString() + " index of gateway "+ indexOfMyGateway);
                        //System.out.println("Path "+ clone.getOutgoingPath().toString() + " index of gateway "+ indexOfMyGateway);
                        for(int i=0; i<indexOfMyGateway; i++) {
                            clone.getOutgoingPath().remove(0);
                        }
                        route = new RouteInfo(packet.getSrc(), Math.abs(packet.getLatency()), clone.getOutgoingPath());
                        //System.out.println("Route: "+ route.getPath().toString());
                        this.addRoute(route, packet.getSrc());
                    }
                }
            }
        }

     
    }

    private void handleStream(TCPPacket packet, String clientIP) {
        if (packet.getSrc() == null || packet.getSrc().equals("") || packet.getSrc().equals("null")) {
            packet.setSrc(clientIP);
            ArrayList<String> bestPath = this.getBestPath();
            packet.setDest(bestPath.get(bestPath.size()-1));
        }
        // Handle the stream packet
        if(this.node.getNodeType()==Node.type.rp){
            //send the stream ack
            System.out.println("Got stream request from "+packet.getSrc());
            String requestFrom = packet.getSrc();
            //In the first place we have to generate the StreamId and then we send the ack
            int id = this.udpManager.addStream(packet.getPathToFile());
            //Lets send the ack
            TCPPacket ack = new TCPPacket(TCPPacket.Type.STREAM_ACK);
            ack.setStreamId(id);
            ArrayList<String> path = this.getBestPath(requestFrom);
            ack.setDest(requestFrom);
            ack.setPathToFile(packet.getPathToFile());
            System.out.println("Path to the file: "+ packet.getPathToFile());
            System.out.println("Sending ack to "+ requestFrom + "via "+ path.get(path.size()-1));
            this.sendPacket(path.get(path.size()-1), ack);
        }
        else{
            String dest = packet.getDest();
            ArrayList<String> bestPath = this.getBestPath(dest);
            this.sendPacket(bestPath.get(0), packet);
            System.out.println("Packet sent to "+ bestPath.get(0));
    
        }
    }

    private void handleStreamAck(TCPPacket packet, String clientIP) {
        // Handle the stream acknowledgment packet
        if (packet.getSrc() == null || packet.getSrc().equals("") || packet.getSrc().equals("null")) {
            packet.setSrc(clientIP);
        }
        if (this.node.getNodeType() == Node.type.client) {
            System.out.println("I was the node that sent the stream");
            //Start the unicast streaming of the stream packets to the rp
            System.out.println("Starting the stream: "+packet.getStreamId()+ " path to file: "+packet.getPathToFile());
            this.udpManager.startStream(packet.getStreamId(), packet.getPathToFile());

        }
        else{
            String dest = packet.getDest();
            ArrayList<String> bestPath = this.getBestPath(dest);
            System.out.println("Sending stream ack to "+ dest + "via "+ bestPath.get(bestPath.size()-1));
            this.sendPacket(bestPath.get(bestPath.size()-1), packet);
            System.out.println("Packet sent to "+ bestPath.get(bestPath.size()-1));
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
        if(packet.getSrc() == null || packet.getSrc().equals("") || packet.getSrc().equals("null")){
            packet.setSrc(clientIP);
        }
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
                    }
                }
            }
        }
        else{
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