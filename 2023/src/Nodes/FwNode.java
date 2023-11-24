// package Nodes;

// import Structs.*;

// import java.util.HashMap;
// import java.util.ArrayList;
// import java.util.Scanner;
// import java.net.Socket;
// import java.io.PrintWriter;
// import java.net.ServerSocket;
// import java.util.concurrent.*;



// public class FwNode{
        
//         private int streamPort = 30000;
//         private int listeningPort = 30001;
//         private int UDPPORT = 4445;
//         //private int probePort = 30002;

//         private HashMap<String, Socket> nodeSockets = new HashMap<String, Socket>();
//         //Paths hashmap, contains the destination and the nodes in should go through
//         //it has a Hashmap of paths as value since if there is more than one path it will send the data to the one
//         //with the lowest cost
//         private HashMap<String, HashMap< Float, ArrayList<String>>> paths = new HashMap<>(); 
//         //Content streams hashmap,


//         private UDPManager udpManager;
//         private Node node;
//         private TCPManager tcpManager;
//         private Node node;



//     public FwNode(Node node) {
//         this.node = node;

//         // udpManager = new UDPManager(this.node);
//         try {
//             this.tcpManager = new TCPManager(this.node);
//         } catch (Exception e) {
//             System.out.println("Error creating TCPManager");
//         }
    

//         try {
//             Scanner sc = new Scanner(System.in);
//             String option = "";
//             while (!option.equals("4")) {

//                 option = sc.nextLine();

//                 switch (option) {
//                     case "1":
//                         System.out.println("Joining network");
//                         for (String gateway : this.node.getGateways()) {
//                             System.out.println("Sending join message to: " + gateway);
//                             //this.tcpManager.createWriting(gateway);
//                             TCPPacket packet = new TCPPacket(TCPPacket.Type.JOIN);
//                             packet.addToOutgoingPath(gateway);
//                             this.tcpManager.sendPacket(gateway, packet);
//                         }
//                         break;

//                     case "3":
//                         System.out.println("Requesting content");
//                         break;
//                 }
//             }
//         } finally {
//             try {
//                 udpManager.closeSocket();
//                 return;
//             } catch (Exception e) {
//                 System.out.println("Error closing sockets");
//             }
//         }
//     }


//     private void createSockets(String rpIp) {

//         try {

//             // fechar o socket
//             System.out.println("Creating write socket" + streamPort);
//             this.writeSocket = new Socket(rpIp, streamPort);
//             System.out.println("Creating read socket");
//             ServerSocket socket = new ServerSocket(listeningPort);
//             System.out.println("Reading socket created");
//             this.readSocket = socket.accept();
//             in = new BufferedReader(new InputStreamReader(this.readSocket.getInputStream()));
//         } catch (Exception e) {
//             e.printStackTrace();
//             System.out.println("Error creating sockets");
//             System.exit(1);
//         }

//     }


//     public void sendText(Message message, String to) {
//         System.out.println("Sending text: " + message + " to: " + to);

//         try {
//             synchronized (writeSocket) {
//                 // Should close the socket
//                 System.out.println("Connected to " + to + " on port " + streamPort);
//                 pw = new PrintWriter(writeSocket.getOutputStream(), true);
//                 pw.println(message);
//             }
//         } catch (Exception e) {
//             System.out.println("Error sending message: " + message);
//         }
//     }

//     public void read(Socket socket) {

//         new Thread(() -> {
//             while (this.readSocket.isConnected()) {
//                 try {
//                     String inputLine;
//                     while ((inputLine = in.readLine()) != null) {
//                         System.out.println("Received data: " + inputLine);
//                     }
//                 } catch (Exception e) {
//                     e.printStackTrace();
//                     System.out.println("Something went wrong accepting connections(Server Socket)");
//                     System.exit(1);
//                 }
//             }
//             System.out.println("Socket is disconnected");
//         }).start();
//     }


// }