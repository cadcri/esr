package Nodes;

import TCP.*;
import UDP.*;
import Structs.*;

import java.net.*;
import java.util.Scanner;
import java.io.*;
import java.lang.Thread;
import java.lang.System;
import java.util.HashMap;


public class Client {

    private int UDPPORT = 4445;
    private OutputStream os;
    private Socket readSocket;
    private Socket writeSocket;
    private PrintWriter pw;
    private BufferedReader in;
    private Node node;

    private UDPManager udpManager;
    private TCPManager tcpManager;

    private Boolean connected = false;


    public Client(Node node) {
        this.node = node;

        //this.tcpHandler = new Manager(this.node);
        

        System.out.println("Num dft gateways: " + this.node.getGateways().size());
        System.out.println("Dft gateways: " + this.node.getGateways().toString());


        // udpManager = new UDPManager(this.node);
        try {
            this.tcpManager = new TCPManager(this.node);
        } catch (Exception e) {
            System.out.println("Error creating TCPManager");
        }
    

        try {
            Scanner sc = new Scanner(System.in);
            String option = "";
            while (!option.equals("4")) {
                System.out.println("What do you want to do?");
                System.out.println("1. Join network with default gateway(s): " + this.node.getGateways().toString());
                System.out.println("2. Start Streaming");
                System.out.println("3. List routes");
                System.out.println("4. Exit");

                option = sc.nextLine();

                switch (option) {
                    case "1":
                        System.out.println("Joining network");
                        for (String gateway : this.node.getGateways()) {
                            System.out.println("Sending join message to: " + gateway);
                            //this.tcpManager.createWriting(gateway);
                            TCPPacket packet = new TCPPacket(TCPPacket.Type.JOIN);
                            packet.addToOutgoingPath(gateway);
                            this.tcpManager.sendPacket(gateway, packet);
                        }
                        break;
                    
                    case "2":
                        System.out.println("Starting streaming");
                        TCPPacket packet = new TCPPacket(TCPPacket.Type.STREAM);
                        packet.addToOutgoingPath(this.node.getGateways().get(0));
                        this.tcpManager.sendPacket(this.node.getGateways().get(0), packet);
                        break;

                    case "3":
                        this.tcpManager.listRoutes();
                        break;
                }
            }
        } finally {
            try {
                //udpManager.closeSocket();
                return;
            } catch (Exception e) {
                System.out.println("Error closing sockets");
            }
        }
    }


    // private void createSockets(String rpIp) {

    //     try {

    //         // fechar o socket
    //         System.out.println("Creating write socket" + streamPort);
    //         this.writeSocket = new Socket(rpIp, streamPort);
    //         System.out.println("Creating read socket");
    //         ServerSocket socket = new ServerSocket(listeningPort);
    //         System.out.println("Reading socket created");
    //         this.readSocket = socket.accept();
    //         in = new BufferedReader(new InputStreamReader(this.readSocket.getInputStream()));
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         System.out.println("Error creating sockets");
    //         System.exit(1);
    //     }

    // }


    // public void sendText(Message message, String to) {
    //     System.out.println("Sending text: " + message + " to: " + to);

    //     try {
    //         synchronized (writeSocket) {
    //             // Should close the socket
    //             System.out.println("Connected to " + to + " on port " + streamPort);
    //             pw = new PrintWriter(writeSocket.getOutputStream(), true);
    //             pw.println(message);
    //         }
    //     } catch (Exception e) {
    //         System.out.println("Error sending message: " + message);
    //     }
    // }

    public void read(Socket socket) {

        new Thread(() -> {
            while (this.readSocket.isConnected()) {
                try {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        System.out.println("Received data: " + inputLine);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Something went wrong accepting connections(Server Socket)");
                    System.exit(1);
                }
            }
            System.out.println("Socket is disconnected");
        }).start();
    }
}
