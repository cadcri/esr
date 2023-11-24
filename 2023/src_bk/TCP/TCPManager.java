

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

/**
 * The TCPManager class handles the management of TCP connections and communication.
 * It provides methods for creating listening and writing sockets, sending packets,
 * and handling incoming packets.
 */
public class TCPManager {
    private Socket writeSocket;
    private ServerSocket listeningSocket;
    private BufferedReader reader = null;
    private PrintWriter writer = null;
    private Node node;
    private int tcpDftPort = 44000;
    private HashMap<String, Socket> gateways;

    public TCPManager(Node node) throws IOException {
        this.node = node;
        this.gateways = new HashMap<>(); // Initialize the gateways HashMap
        new Thread(() -> {
            createListening(); // Call the method asynchronously
        }).start();
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

    public void createWriting(String gateway) {
        try {
            System.out.println("Creating write socket " + tcpDftPort + " to " + gateway);
            if (!this.gateways.containsKey(gateway)) {
                Socket socket = new Socket(gateway, tcpDftPort); // Create a new Socket and connect it to the gateway
                this.gateways.put(gateway, socket);
            } else {
                System.out.println("Socket already created");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error creating write socket");
            System.exit(1);
        }
    }

    public void sendPacket(String gateway, TCPPacket packet) {
        try {
            if (this.gateways.containsKey(gateway)) {
                String serializedPacket = packet.serializa();
                Socket socket = this.gateways.get(gateway);
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                writer.println(serializedPacket);
                writer.flush();
                this.gateways.get(gateway).close();
                this.gateways.remove(gateway);
            } else {
                createWriting(gateway);
                sendPacket(gateway, packet);
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
                    this.handleReponseMiddle(clientSocket);

                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Something went wrong accepting connections(Server Socket)");
                    System.exit(1);
                }
            }
            //System.out.println("Socket is disconnected");
        }).start();
    }

    private void handleReponseMiddle(Socket socket)throws Exception{
        System.out.println("Got message from: "+socket.getInetAddress().getHostAddress());
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        while(true){
            String packet = reader.readLine();
            if(packet != null){
                InetAddress clientAddress = socket.getInetAddress();
                String clientIP = clientAddress.getHostAddress();
                new Thread(() -> {
                    TCPPacket pacote = TCPPacket.deserialize(packet);
                    this.handleResponse(pacote, clientIP);

                }).start();
            }

        }

    }

    private void handleResponse(TCPPacket packet, String clientIP) {
        switch (packet.getType()) {
            case JOIN:
            case JOIN_ACK:
                System.out.println("Join or JoinAck request received");
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
                    System.out.println("Join request received from: " + clientIP);
                    if (this.node.getNodeType() == Node.type.rp) {
                        // If it is the rp, it doesn't redirect the packet to the neighboring nodes, it sends the ack packet
                        // The ack packet should have the initial timestamp
                        System.out.println("Path to me: " + packet.getOutgoingPath().toString());
                        TCPPacket clone = packet.clone();
                        clone.addToIncomingPath(clientIP);
                        clone.setType(TCPPacket.Type.JOIN_ACK);
                        this.sendPacket(clientIP, clone);
                    } else {
                        // Send a clone of the packet to all of the neighboring nodes
                        // It should send to all of the neighboring nodes except the one it came from
                        if (packet.getSrc() == null) {
                            // Get the IP of the socket that received this
                            packet.setSrc(clientIP);
                        }
                        for (String neighbourGateway : this.node.getGateways()) {
                            TCPPacket clone = packet.clone();
                            if (!clientIP.equals(neighbourGateway)) {
                                clone.addToOutgoingPath(neighbourGateway);
                                clone.addToIncomingPath(clientIP);
                                // Add the current node
                                this.sendPacket(neighbourGateway, clone);
                            }
                        }
                    }
                    break;

                case JOIN_ACK:
                    if (clientIP.equals(packet.getOutgoingPath().get(0))) {
                        // This is the node that tried to join
                        System.out.println("I was the node that joined");
                    } else {
                        System.out.println("Debug");
                        for (String neighbourGateway : this.node.getGateways()) {
                            if (packet.getIncomingPath().contains(neighbourGateway)) {
                                // I want to redirect the packet to the next node in the path
                                int index = packet.getIncomingPath().indexOf(neighbourGateway);
                                System.out.println("Sending the packet to: " + packet.getIncomingPath().get(index));
                                this.sendPacket(packet.getIncomingPath().get(index), packet);
                            }
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
