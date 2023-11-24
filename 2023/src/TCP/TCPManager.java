

// package TCP;

// import java.io.*;
// import java.net.*;
// import Structs.*;
// import java.util.*;

// /**
//  * The TCPManager class handles the management of TCP connections and communication.
//  * It provides methods for creating listening and writing sockets, sending packets,
//  * and handling incoming packets.
//  */
// public class TCPManager {
//     private Socket writeSocket;
//     private ServerSocket listeningSocket;
//     private BufferedReader reader = null;
//     private PrintWriter writer = null;
//     private Node node;
//     private int tcpDftPort = 44000;
//     private HashMap<String, Socket> gateways;

//     public TCPManager(Node node) throws IOException {
//         this.node = node;
//         this.gateways = new HashMap<>(); // Initialize the gateways HashMap
//         new Thread(() -> {
//             createListening(); // Call the method asynchronously
//         }).start();
//     }

//     public void createListening() {
//         try {
//             System.out.println("Creating read socket");
//             this.listeningSocket = new ServerSocket(tcpDftPort);
//             read();
//             System.out.println("Listening socket created");
//         } catch (IOException e) {
//             e.printStackTrace();
//             System.out.println("Error creating listening socket");
//             System.exit(1);
//         }
//     }

//     public void createWriting(String gateway) {
//         try {
//             System.out.println("Creating write socket " + tcpDftPort + " to " + gateway);
//             if (!this.gateways.containsKey(gateway)) {
//                 Socket socket = new Socket(gateway, tcpDftPort); // Create a new Socket and connect it to the gateway
//                 this.gateways.put(gateway, socket);
//             } else {
//                 System.out.println("Socket already created");
//             }
//         } catch (IOException e) {
//             e.printStackTrace();
//             System.out.println("Error creating write socket");
//             System.exit(1);
//         }
//     }

//     public void sendPacket(String gateway, TCPPacket packet) {
//         try {
//             if (this.gateways.containsKey(gateway)) {
//                 ObjectOutputStream objectOutputStream = new ObjectOutputStream(this.gateways.get(gateway).getOutputStream());
//                 objectOutputStream.writeObject(packet);
//                 objectOutputStream.flush();
//             } else {
//                 createWriting(gateway);
//                 sendPacket(gateway, packet);
//             }
//         } catch (Exception e) {
//             System.out.println("Error sending packet: " + packet);
//         }
//     }

//     public void read() {
//         new Thread(() -> {
//             while (this.listeningSocket == null) {
//                 try {
//                     Thread.sleep(100);
//                 } catch (Exception e) {
//                     System.out.println("Error sleeping");
//                 }
//             }
//             while (!this.listeningSocket.isClosed()) {
//                 try {
//                     Socket clientSocket = this.listeningSocket.accept();
                    
//                     new Thread(() -> {
//                         try {
//                             System.out.println("Debug2");
//                             ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
                            
//                             TCPPacket packet;
//                             if ((packet = (TCPPacket) objectInputStream.readObject()) != null) {
//                                 InetAddress clientAddress = clientSocket.getInetAddress();
//                                 String clientIP = clientAddress.getHostAddress();
//                                 System.out.println("Got message from: "+clientIP);
//                                 handleResponse(packet, clientIP);
                                
//                                 // Process the received data here
//                             }
//                             objectInputStream.close();
//                             //clientSocket.close();
//                         }  catch (IOException e) {
//                             System.out.println("Something went wrong reading data from client");
//                         } catch (ClassNotFoundException e) {
//                             System.out.println("Error deserializing object");
//                         }
//                         //try and reup the socket if that is the case that is breaking
//                     }).start();

//                 } catch (IOException e) {
//                     e.printStackTrace();
//                     System.out.println("Something went wrong accepting connections(Server Socket)");
//                     System.exit(1);
//                 }
//             }
//             System.out.println("Socket is disconnected");
//         }).start();
//     }

//     private void handleResponse(TCPPacket packet, String clientIP) {
//         switch (packet.getType()) {
//             case JOIN:
//             case JOIN_ACK:
//                 vascoDaGama(packet, clientIP);
//                 break;

//             case LEAVE:
//                 System.out.println("Leave request received");
//                 // Send the leave ack to the gateway
//                 // Set this gateway as inactive
//                 break;

//             case LEAVE_ACK:
//                 System.out.println("Leaving");
//                 // Close all sockets
//                 // Go to the main screen so it can become active once again
//                 break;
//         }
//     }

//     private void vascoDaGama(TCPPacket packet, String clientIP) {
//         try {
//             switch (packet.getType()) {
//                 case JOIN:
//                     System.out.println("Join request received from: " + clientIP);
//                     if (this.node.getNodeType() == Node.type.rp) {
//                         // If it is the rp, it doesn't redirect the packet to the neighboring nodes, it sends the ack packet
//                         // The ack packet should have the initial timestamp
//                         System.out.println("Path to me: " + packet.getOutgoingPath().toString());
//                         TCPPacket clone = packet.clone();
//                         clone.addToIncomingPath(clientIP);
//                         clone.setType(TCPPacket.Type.JOIN_ACK);
//                         this.sendPacket(clientIP, clone);
//                     } else {
//                         // Send a clone of the packet to all of the neighboring nodes
//                         // It should send to all of the neighboring nodes except the one it came from
//                         if (packet.getSrc() == null) {
//                             // Get the IP of the socket that received this
//                             packet.setSrc(clientIP);
//                         }
//                         for (String neighbourGateway : this.node.getGateways()) {
//                             TCPPacket clone = packet.clone();
//                             if (!clientIP.equals(neighbourGateway)) {
//                                 clone.addToOutgoingPath(neighbourGateway);
//                                 clone.addToIncomingPath(clientIP);
//                                 // Add the current node
//                                 this.sendPacket(neighbourGateway, clone);
//                             }
//                         }
//                     }
//                     break;

//                 case JOIN_ACK:
//                     if (clientIP.equals(packet.getOutgoingPath().get(0))) {
//                         // This is the node that tried to join
//                         System.out.println("I was the node that joined");
//                     } else {
//                         System.out.println("Debug");
//                         for (String neighbourGateway : this.node.getGateways()) {
//                             if (packet.getIncomingPath().contains(neighbourGateway)) {
//                                 // I want to redirect the packet to the next node in the path
//                                 int index = packet.getIncomingPath().indexOf(neighbourGateway);
//                                 System.out.println("Sending the packet to: " + packet.getIncomingPath().get(index));
//                                 this.sendPacket(packet.getIncomingPath().get(index), packet);
//                             }
//                         }
//                     }
//                     break;
//             }
//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }
// }



// package TCP;

// import java.io.*;
// import java.net.*;
// import Structs.*;
// import java.util.*;

// /**
//  * The TCPManager class handles the management of TCP connections and communication.
//  * It provides methods for creating listening and writing sockets, sending packets,
//  * and handling incoming packets.
//  */
// public class TCPManager {
//     private Socket writeSocket;
//     private ServerSocket listeningSocket;
//     private BufferedReader reader = null;
//     private PrintWriter writer = null;
//     private Node node;
//     private int tcpDftPort = 44000;
//     private HashMap<String, ObjectOutputStream> gateways;

//     public TCPManager(Node node) throws IOException {
//         this.node = node;
//         this.gateways = new HashMap<>(); // Initialize the gateways HashMap
//         new Thread(() -> {
//             createListening(); // Call the method asynchronously
//         }).start();
//     }

//     public void createListening() {
//         try {
//             System.out.println("Creating read socket");
//             this.listeningSocket = new ServerSocket(tcpDftPort);
//             read();
//             System.out.println("Listening socket created");
//         } catch (IOException e) {
//             e.printStackTrace();
//             System.out.println("Error creating listening socket");
//             System.exit(1);
//         }
//     }

//     public void createWriting(String gateway) {
//         try {
//             System.out.println("Creating write socket " + tcpDftPort + " to " + gateway);
//             if (!this.gateways.containsKey(gateway)) {
//                 Socket socket = new Socket(gateway, tcpDftPort); // Create a new Socket and connect it to the gateway
//                 ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
//                 this.gateways.put(gateway, objectOutputStream);
//             } else {
//                 System.out.println("Socket already created");
//             }
//         } catch (Exception e) {
//             e.printStackTrace();
//             System.out.println("Error creating write socket");
//             System.exit(1);
//         }
//     }

//     public void sendPacket(String gateway, TCPPacket packet) {
//         try {
//             if (this.gateways.containsKey(gateway)) {
//                 ObjectOutputStream objectOutputStream = this.gateways.get(gateway);
//                 objectOutputStream.writeObject(packet);
//                 objectOutputStream.flush();
//             } else {
//                 createWriting(gateway);
//                 sendPacket(gateway, packet);
//             }
//         } catch (Exception e) {
//             System.out.println("Error sending packet: " + packet);
//         }
//     }

//     public void read() {
//         new Thread(() -> {
//             while (this.listeningSocket == null) {
//                 try {
//                     Thread.sleep(100);
//                 } catch (Exception e) {
//                     System.out.println("Error sleeping");
//                 }
//             }
//             while (true) {
//                 try {
//                     Socket clientSocket = this.listeningSocket.accept();
                    
//                     new Thread(() -> {
//                         try {
//                             ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
                            
//                             TCPPacket packet;
//                             if ((packet = (TCPPacket) objectInputStream.readObject()) != null) {
//                                 InetAddress clientAddress = clientSocket.getInetAddress();
//                                 String clientIP = clientAddress.getHostAddress();
//                                 System.out.println("Got message from: "+clientIP);
//                                 handleResponse(packet, clientIP);
                                
//                                 // Process the received data here
//                             }
                            
//                             //clientSocket.close();
//                         }  catch (IOException e) {
//                             System.out.println("Something went wrong reading data from client");
//                         } catch (ClassNotFoundException e) {
//                             System.out.println("Error deserializing object");
//                         }
//                         catch(Exception e){
//                             System.out.println("Exception out of bounds");
//                             e.printStackTrace();
//                         }
//                         finally{
//                             //objectInputStream.close();

//                         }
//                         //try and reup the socket if that is the case that is breaking
//                     }).start();

//                 } catch (Exception e) {
//                     e.printStackTrace();
//                     System.out.println("Something went wrong accepting connections(Server Socket)");
//                     System.exit(1);
//                 }
//             }
//             //System.out.println("Socket is disconnected");
//         }).start();
//     }

//     private void handleResponse(TCPPacket packet, String clientIP) {
//         switch (packet.getType()) {
//             case JOIN:
//             case JOIN_ACK:
//                 vascoDaGama(packet, clientIP);
//                 break;

//             case LEAVE:
//                 System.out.println("Leave request received");
//                 // Send the leave ack to the gateway
//                 // Set this gateway as inactive
//                 break;

//             case LEAVE_ACK:
//                 System.out.println("Leaving");
//                 // Close all sockets
//                 // Go to the main screen so it can become active once again
//                 break;
//         }
//     }

//     private void vascoDaGama(TCPPacket packet, String clientIP) {
//         try {
//             switch (packet.getType()) {
//                 case JOIN:
//                     System.out.println("Join request received from: " + clientIP);
//                     if (this.node.getNodeType() == Node.type.rp) {
//                         // If it is the rp, it doesn't redirect the packet to the neighboring nodes, it sends the ack packet
//                         // The ack packet should have the initial timestamp
//                         System.out.println("Path to me: " + packet.getOutgoingPath().toString());
//                         TCPPacket clone = packet.clone();
//                         clone.addToIncomingPath(clientIP);
//                         clone.setType(TCPPacket.Type.JOIN_ACK);
//                         this.sendPacket(clientIP, clone);
//                     } else {
//                         // Send a clone of the packet to all of the neighboring nodes
//                         // It should send to all of the neighboring nodes except the one it came from
//                         if (packet.getSrc() == null) {
//                             // Get the IP of the socket that received this
//                             packet.setSrc(clientIP);
//                         }
//                         for (String neighbourGateway : this.node.getGateways()) {
//                             TCPPacket clone = packet.clone();
//                             if (!clientIP.equals(neighbourGateway)) {
//                                 clone.addToOutgoingPath(neighbourGateway);
//                                 clone.addToIncomingPath(clientIP);
//                                 // Add the current node
//                                 this.sendPacket(neighbourGateway, clone);
//                             }
//                         }
//                     }
//                     break;

//                 case JOIN_ACK:
//                     if (clientIP.equals(packet.getOutgoingPath().get(0))) {
//                         // This is the node that tried to join
//                         System.out.println("I was the node that joined");
//                     } else {
//                         System.out.println("Debug");
//                         for (String neighbourGateway : this.node.getGateways()) {
//                             if (packet.getIncomingPath().contains(neighbourGateway)) {
//                                 // I want to redirect the packet to the next node in the path
//                                 int index = packet.getIncomingPath().indexOf(neighbourGateway);
//                                 System.out.println("Sending the packet to: " + packet.getIncomingPath().get(index));
//                                 this.sendPacket(packet.getIncomingPath().get(index), packet);
//                             }
//                         }
//                     }
//                     break;
//             }
//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }
// }




package TCP;

import java.io.*;
import java.net.*;
import Structs.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.time.LocalDateTime;


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
    private Socket writeSocket;
    private ServerSocket listeningSocket;
    private BufferedReader reader = null;
    private PrintWriter writer = null;
    private Node node;
    private int tcpDftPort = 44000;
    private HashMap<String, Socket> gateways;

    // Dest node and the latencies to it with the path it follows
    private HashMap<String, ArrayList<RouteInfo>> routes = new HashMap<>();
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private Lock gatewaysLock = new ReentrantLock();

    public TCPManager(Node node) throws IOException {
        this.node = node;
        this.gateways = new HashMap<>(); // Initialize the gateways HashMap
        new Thread(() -> {
            createListening(); // Call the method asynchronously
        }).start();

        executor.scheduleAtFixedRate(() -> {
            //System.out.println("Updating paths");
            //probeNeighbors();
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
            case JOIN:
            case JOIN_ACK:
                vascoDaGama(packet, clientIP);
                break;

            case LEAVE:
                System.out.println("Leave request received");
                // Send the leave ack to the gateway
                // Set this gateway as inactive
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
                case STREAM:
                    // handleStream(packet, clientIP);
                    break;
                case STREAM_ACK:
                    // handleStreamAck(packet, clientIP);
                    break;

                case REQUEST_STREAM:
                    break;

                case REQUEST_STREAM_ACK:
                    break;

                case STREAM_END:
                    break;
                case STREAM_END_ACK:
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
            System.out.println("Join request received from: " + packet.getSrc());
            System.out.println("Path to me: " + packet.getOutgoingPath().toString());
            TCPPacket clone = packet.clone();
            clone.addToIncomingPath(ClientIP);
            clone.setType(TCPPacket.Type.JOIN_ACK);
            this.sendPacket(ClientIP, clone);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime packetInitTime = packet.getTimeStampInit();

            Float latency = (float) (now.getNano() - packetInitTime.getNano()) / 1000000;

            // Create the route info
        
            RouteInfo route = new RouteInfo(packet.getSrc(), latency, packet.getOutgoingPath());
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
                    clone.addToOutgoingPath(neighbourGateway);
                    clone.addToIncomingPath(ClientIP);
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
            RouteInfo routeInfo = new RouteInfo(packet.getSrc(), latency, packet.getIncomingPath());
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
        // TODO: Implement stream handling logic
        if(this.node.getNodeType()==Node.type.rp){
            //send the stream ack
            TCPPacket clone = packet.clone();
            clone.setType(TCPPacket.Type.STREAM_ACK);
            this.sendPacket(clientIP, clone);
            //If i already have the stream and it is up i just neglectit

        
        }
        else{
            //send the stream to the next node
            for (String neighbourGateway : this.node.getGateways()) {
                if (packet.getIncomingPath().contains(neighbourGateway)) {
                    // I want to redirect the packet to the next node in the path
                    int index = packet.getIncomingPath().indexOf(neighbourGateway);
                    this.sendPacket(packet.getIncomingPath().get(index), packet);
                }
            }
        }
    }

    private void handleStreamAck(TCPPacket packet, String clientIP) {
        // Handle the stream acknowledgment packet
        // TODO: Implement stream acknowledgment handling logic
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