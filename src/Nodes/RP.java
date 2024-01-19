package Nodes;

import Structs.*;
import UDP.*;
import TCP.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Scanner;
import java.net.Socket;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.concurrent.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;


public class RP{
    //Paths hashmap, contains the destination and the nodes in should go through
    //it has a Hashmap of paths as value since if there is more than one path it will send the data to the one
    //with the lowest cost
    //Content streams hashmap, contains the streamname and the nodes that are available to stream it
    //It should also have if the stream is ongoing and how many nodes are consuming it


    private UDPManager udpManager;
    private Node node;
    private TCPManager tcpManager;

    public RP(Node node)throws IOException{
        this.node = node;

        System.out.println("Num dft gateways: " + this.node.getGateways().size());
        System.out.println("Dft gateways: " + this.node.getGateways().toString());

        // Initialize the tcpManager HashMap
        this.udpManager = new UDPManager(node);
        tcpManager=new TCPManager(this.node, this.udpManager);

        String option = "";
        Scanner scanner = new Scanner(System.in);  

        while(!option.equals("4")){
            System.out.println("What do you want to do?");
            System.out.println("1. Available streams");
            System.out.println("2. List routes");
            System.out.println("4. Exit");
            option = scanner.nextLine();

            switch(option){

                case("1"):
                    //this.tcpManager.listStreams();
                    break;

                case("2"):
                    this.tcpManager.listRoutes();
                    break;
                
                case("3"):
                    //sendTextToAllNodes();
                    break;
            }

        }
        scanner.close();
    }

}