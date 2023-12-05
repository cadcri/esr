package Nodes;

import TCP.*;
import UDP.*;
import Structs.*;

import java.util.Scanner;
import java.io.*;
import java.lang.System;
import java.util.Base64;


public class FwNode {
    private OutputStream os;
    private PrintWriter pw;
    private BufferedReader in;
    private Node node;

    private UDPManager udpManager;
    private TCPManager tcpManager;

    private Boolean connected = false;


    public FwNode(Node node) {
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
                        this.tcpManager.sendPacket("",packet);
                        break;

                    case "3":
                        this.tcpManager.listRoutes();
                        break;
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
