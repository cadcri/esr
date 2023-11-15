import java.net.Socket;
import java.util.Scanner;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.Thread;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.System;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {

    public int streamPort=30001;
    public int listeningPort=30000;
    private int UDPPORT = 4445;
    private OutputStream os;
    private Socket readSocket;
    private Socket writeSocket;
    private PrintWriter pw;
    private BufferedReader in;
    private Node node;

    private UDPManager udpManager;

    private enum Message{
        PROBE,
        ACK,
        JOIN,
        JOIN_ACK,
        LEAVE,
        LEAVE_ACK,
        TEXT
    }


    public Client(Node node){
        this.node = node;
        udpManager = new UDPManager(this.node);
        // new Thread(() ->{
        //     createSockets(rpIP);
        // }).start();

        //probeNeighbours();

        try{
            Scanner sc = new Scanner(System.in);
            String option = "";
            while(!option.equals("4")){
                System.out.println("What do you want to do?");
                System.out.println("1. Add content to stream");
                System.out.println("2. Get neighbor list");
                System.out.println("3. Request content");
                System.out.println("4. Exit");

                option = sc.nextLine();

                switch(option){
                    case("1"):
                        System.out.println("Adding content to stream");
                        // System.out.println("What do you want to add?");
                        // String content = sc.nextLine();
                        // sendMessage(content, rpIP);
                        break;

                    case("2"):
                        System.out.println("Getting neighbor list");
                        System.out.println(node.getNeighbors().toString());
                        // sendText("getNeighbours", rpIP);
                        break;

                    case("3"):
                        System.out.println("Requesting content");
                        break;
                }


            }

        }
        finally{
            try{
                this.readSocket.close();
                this.writeSocket.close();
                return;
            }
            catch(Exception e){
                System.out.println("Error closing sockets");
            }
        }
    }


    // private void probeNeighbours(){

    //     new Thread(() -> {
    //         //Create UDP Datagram Socket
    //         System.out.println(this.udpSocket.isConnected());
    //         while(this.udpSocket.isConnected()){

    //             //Send heartbeat message to all the neighbours
    //             for(Node neighbour: this.node.getNeighbors().values()){
    //                 try{
    //                     //Send heartbeat message to all the neighbours
    //                     byte[] buf = new byte[256];
    //                     buf = Message.PROBE.toString().getBytes();
    //                     DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(neighbour.getNodeIP()), UDPPORT);
    //                     this.udpSocket.send(packet);
    //                     System.out.println("Probed node: "+neighbour.getNodeIP());
    //                     Thread.sleep(1000);
    //                 }
    //                 catch(Exception e){
    //                     System.out.println("Error sending heartbeat message");
    //                 }
    //             }
    //         }
    //     }).start();
    // }

    // private void udpHandler(){

    //     new Thread(() -> {
    //         System.out.println(this.udpSocket.isConnected());
    //         while(this.udpSocket.isConnected()){
    //             try{
    //                 byte[] buf = new byte[256];
    //                 DatagramPacket packet = new DatagramPacket(buf, buf.length);
    //                 this.udpSocket.receive(packet);
    //                 String received = new String(packet.getData(), 0, packet.getLength());
    //                 System.out.println("Received UDP message: "+received);
    //                 if(received.equals(Message.PROBE.toString())){
    //                     //Send ACK
    //                     buf = Message.ACK.toString().getBytes();
    //                     packet = new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort());
    //                     this.udpSocket.send(packet);
    //                 }
    //                 else if(received.equals(Message.ACK.toString())){
    //                      // Get the IP of the sender
    //                     String senderIP = packet.getAddress().getHostAddress();
    //                     System.out.println("Received message from: " + senderIP);
    //                     for(Node nodo: this.node.getNeighbors().values()){
    //                         if(nodo.getNodeIP().equals(senderIP)){
    //                             nodo.setNodeState(Node.state.on);
    //                             System.out.println("Node "+nodo.getNodeName()+" is on");
    //                         }
    //                     }   
    //                 }
    //             }
    //             catch(Exception e){
    //                 System.out.println("Error receiving UDP message");
    //             }
    //         }
    //     }).start();
    // }

    // private void createUDPSockets(){
    //     try{
    //         this.udpSocket = new DatagramSocket();
    //     }
    //     catch(Exception e){
    //         System.out.println("Error creating UDP socket");
    //     }
    // }

    private void createSockets(String rpIp){
        
        try{

            //fechar o socket
            System.out.println("Creating write socket"+streamPort);
            this.writeSocket = new Socket(rpIp, streamPort);
            System.out.println("Creating read socket");
            ServerSocket socket = new ServerSocket(listeningPort);
            System.out.println("Reading socket created");
            this.readSocket = socket.accept();
            in = new BufferedReader(new InputStreamReader(this.readSocket.getInputStream()));
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Error creating sockets");
            System.exit(1);
        }

    }

    
    public void sendText(Message message, String to){
        
        System.out.println("Sending text: "+message+" to: "+to);

        try{
            //Should close the socket 
            System.out.println("Connected to "+to+" on port "+streamPort);
            pw = new PrintWriter(writeSocket.getOutputStream(), true);
            pw.println(message);
        }
        catch(Exception e){
            System.out.println("Error sending message: "+message);
        }      
    }

    public void read(Socket socket){

        new Thread(() -> {
            while (this.readSocket==null){
                try{
                    Thread.sleep(100);
                }
                catch(Exception e){
                    System.out.println("Error sleeping");
                }
            }
            while(this.readSocket.isConnected()){
                try{
                    String inputLine;
                    while ((inputLine=in.readLine()) != null) {
                        System.out.println("Received data: " + inputLine);
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                    System.out.println("Something went wrong accepting connections(Server Socket)");
                    System.exit(1);
                }
            }
            System.out.println("Socket is disconnected");
        }).start();
    }


}
