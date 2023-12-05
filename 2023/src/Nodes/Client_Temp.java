package Nodes;

import TCP.*;
import UDP.*;
import Structs.*;

import java.util.Scanner;
import java.io.*;
import java.lang.System;
import java.util.Base64;


public class Client_Temp {
    private OutputStream os;
    private PrintWriter pw;
    private BufferedReader in;
    private Node node;

    private UDPManager udpManager;
    private TCPManager tcpManager;

    private Boolean connected = false;


    public Client_Temp(Node node) {
        this.node = node;

        //this.tcpHandler = new Manager(this.node);
        

        System.out.println("Num dft gateways: " + this.node.getGateways().size());
        System.out.println("Dft gateways: " + this.node.getGateways().toString());


        // udpManager = new UDPManager(this.node);
        try {
            this.udpManager = new UDPManager(this.node, this.tcpManager);
            this.tcpManager = new TCPManager(this.node, this.udpManager);
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
                System.out.println("5. Join stream");
                System.out.println("6. List streams");
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
                            //packet.addToIncomingPath(gateway);
                            this.tcpManager.sendPacket(gateway, packet);
                        }
                        break;
                    
                    case "2":
                        System.out.println("Starting streaming");
                        System.out.println("Enter the full path of the file you want to stream: ");
                        String path = sc.nextLine();
                        TCPPacket packet = new TCPPacket(TCPPacket.Type.STREAM);
                        packet.setPathToFile(path);
                        packet.setSrc(null);
                        packet.setDest(null);
                        this.tcpManager.sendPacket(null,packet);
                        break;

                    case "3":
                        this.tcpManager.listRoutes();
                        break;

                    case "5":
                        System.out.println("Joining stream");
                        System.out.println("Enter the stream id you want to join: ");
                        String streamId = sc.nextLine();
                        TCPPacket packet2 = new TCPPacket(TCPPacket.Type.REQUEST_STREAM);
                        packet2.setStreamId(Integer.parseInt(streamId));
                        this.tcpManager.sendPacket(null, packet2);
                        break;

                    case "6":
                        System.out.println("Listing streams");
                        TCPPacket pac = new TCPPacket(TCPPacket.Type.LIST_STREAMS);
                        this.tcpManager.sendPacket(null, pac);
                }
            }
        } finally {
            try {
                //udpManager.leave();
                //tcpManager.leave();
                return;
            } catch (Exception e) {
                System.out.println("Error closing sockets");
            }
        }
    }

    public String encode(String path){
        byte[] bytes = Base64.getEncoder().encode(path.getBytes());
        return new String(bytes);
    }
}
