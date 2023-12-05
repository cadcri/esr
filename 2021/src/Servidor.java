package src;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Objects;


public class Servidor extends JFrame implements ActionListener {

    static Graph rede;
    static Node startNode;
    private static Node startNode1;

    public static void main(String[] argv) {
       new Servidor().pack();
       startNode1 = new Node("10.0.18.1");
        Node nodeA = new Node("10.0.5.2");
        Node nodeB = new Node("10.0.9.2");
        Node nodeC = new Node("10.0.9.1");
        Node nodeD = new Node("10.0.4.2");
        startNode.addDestination(nodeA);
        startNode.addDestination(nodeB);
        nodeA.addDestination(startNode);
        nodeA.addDestination(nodeC);
        nodeB.addDestination(startNode);
        nodeB.addDestination(nodeC);
        nodeC.addDestination(nodeA);
        nodeC.addDestination(nodeB);
        nodeC.addDestination(nodeD);
        nodeD.addDestination(nodeC);
        startNode.addComputer("10.0.18.20");
        startNode.addComputer("10.0.0.20");
        nodeB.addComputer("10.0.19.20");
        nodeB.addComputer("10.0.19.21");
        nodeC.addComputer("10.0.2.21");
        nodeD.addComputer("10.0.20.20");
        nodeD.addComputer("10.0.20.21");
        rede = new Graph();
        rede.addNode(startNode);
        rede.addNode(nodeA);
        rede.addNode(nodeB);
        rede.addNode(nodeC);
        rede.addNode(nodeD);
        rede = Graph.calculateShortestPathFromSource(rede,startNode);

    }

    InetAddress ClientIPAddr;
    int ClientIpPort;

    /////////
    int imagenb = 0; //image nb of the image currently transmitted
    VideoStream video; //src.VideoStream object used to access video frames
    static int FRAME_PERIOD = 100; //Frame period of the video to stream, in ms
    static int VIDEO_LENGTH = 500; //length of the video in frames

    ////////////
    DatagramSocket RTPsocket;
    Timer sTimer; //timer used to send the images at the video frame rate
    byte[] buff; //buffer used to store the images to send to the client
    String finalPaths = "";

    DatagramSocket ReceveSocket;
    ArrayList<String> clients_connected = new ArrayList();

    public Servidor() {
        super();

        sTimer = new Timer(FRAME_PERIOD, this);
        sTimer.setInitialDelay(0);
        sTimer.setCoalesce(true);
        buff = new byte[150000];

        try {
            ClientIpPort = 25000;
            RTPsocket = new DatagramSocket();
            video = new VideoStream("movie.Mjpeg");
        } catch (SocketException e) {
            System.out.println("src.Servidor: erro no socket: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("src.Servidor: erro no video: " + e.getMessage());
        }

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                sTimer.stop();
                System.exit(0);
            }
        });

        try {
            ReceveSocket = new DatagramSocket(25000);
            ReceveSocket.setSoTimeout(10);
        } catch (SocketException e) {
            System.out.println("src.Cliente: erro no socket: " + e.getMessage());
        }

        sTimer.start();
    }

    public void actionPerformed(ActionEvent e) {
        //// RECEVE PEDIDOS
        DatagramPacket rcvdp = new DatagramPacket(buff, buff.length);
        try {
            ReceveSocket.receive(rcvdp);
            CelsoPacket packet = new CelsoPacket(rcvdp.getData(), rcvdp.getLength());

            int ip_byte_size = packet.getIPBytes(buff);
            byte[] string = new byte[ip_byte_size];
            for (int i = 0; i < ip_byte_size; i++){
                string[i] = buff[i];
            }
            String ip = new String(string);
            System.out.println("===============================");
            if(packet.continueStream()){
                System.out.println("NEW CLIENT : "+ip);
                clients_connected.add(ip);
            } else {
                System.out.println("CLIENT GONE AWAY : "+ip);
                clients_connected.remove(ip);
            }

            /////////// CALCULATE PATH
            finalPaths = "";

            for (String ipClient: clients_connected){
                String path = "";
                for(Node node : rede.nodes){
                    if (node.hasComputer(ipClient)){
                        for(Node n : node.shortestPath){
                            path += n.name+":";
                        }
                        path += node.name;
                    }
                }
                path = path+":"+ipClient; // add ip of client
                if (!finalPaths.equals("")) // add to the total of paths
                    finalPaths = finalPaths+"-"+path;
                else
                    finalPaths = path;
            }
            if (!finalPaths.equals("")){
                System.out.println(finalPaths);
                System.out.println("===============================");
            }

        } catch (IOException ex) {
           // ex.printStackTrace();
        }


        ////////////////////////////////////////// SEND VIDEO



        ///////// SEND
        if (imagenb >= VIDEO_LENGTH) {
            try {
                video = new VideoStream("movie.Mjpeg");
                imagenb=0;
            } catch (Exception a) {
                System.out.println("src.Servidor: erro no video: " + a.getMessage());
                return;
            }
        }

        imagenb++;
        try {
            int image_length = video.getnextframe(buff);
            CelsoPacket packet = new CelsoPacket((byte) 0x2, buff, image_length, finalPaths.getBytes(), finalPaths.getBytes().length);
            int size = packet.getPacketBytes(buff);
            DatagramPacket senddp = new DatagramPacket(buff, size, InetAddress.getByName(startNode.name), ClientIpPort);
            if (!Objects.equals(finalPaths, ""))
                RTPsocket.send(senddp);
        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }
    }
}