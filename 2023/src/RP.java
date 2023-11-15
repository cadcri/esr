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


public class RP{

    private int streamPort = 30000;
    private int listeningPort = 30001;
    private int UDPPORT = 4445;
    //private int probePort = 30002;

    private HashMap<String, Node> nodes = new HashMap<String, Node>();
    private HashMap<String, Node> graph = new HashMap<String, Node>();
    private HashMap<String, Socket> nodeSockets = new HashMap<String, Socket>();
    private UDPManager udpManager;
    private Node node;



    private Socket listeningSocket;

    private enum Status{
        //Set specific error status
        success,
        error
    }

    private enum Type{
        PROBE,
        ACK,
        JOIN,
        JOIN_ACK,
        LEAVE,
        LEAVE_ACK,
        TEXT
    }

    public RP(Node node){
        this.node=node;
        udpManager =new UDPManager(this.node);
        // new Thread(() -> {
        //     try{
        //         ServerSocket socketTemp = new ServerSocket(listeningPort);
        //         this.listeningSocket = socketTemp.accept();
        //     }
        //     catch(Exception e){
        //         e.printStackTrace();
        //     }
        // }).start();

        //udpHandler();
        

        //System.out.println(node.toString());
        this.nodes=node.getNeighbors();
        //System.out.println(this.nodes.size());

        String option = "";
        Scanner scanner = new Scanner(System.in);  

        while(!option.equals("4")){
            System.out.println("What do you want to do?");
            System.out.println("1. List all nodes");
            System.out.println("2. Probe all nodes");
            System.out.println("3. Send text to all nodes");
            System.out.println("4. Exit");
            option = scanner.nextLine();

            switch(option){

                case("1"):
                    listAllNodes();
                    break;

                case("2"):
                    probeAllNodes();
                    break;
                
                case("3"):
                    sendTextToAllNodes();
                    break;
            }

        }
        scanner.close();
    }

    private void listAllNodes(){
        System.out.println("Listing all nodes");
        for(Node nodo: this.nodes.values()){
            System.out.println(nodo.getNodeName());
            System.out.println(nodo.toString());
        }
    }

    private void probeAllNodes(){
        for(Node nodo: this.node.getNeighbors().values()){
            probeNode(nodo);
        }
    }

    private void probeNode(Node node){
        try{
            System.out.println("Probing node: "+node.getNodeIP());
            this.udpManager.sendPacket(Type.PROBE.toString(), node.getNodeIP(), UDPPORT);
        }
        catch(Exception e){
            System.out.println("Error probing node: "+node.getNodeIP());
        }
    }

    private void sendTextToAllNodes(){
        Scanner sc = new Scanner(System.in);
        System.out.println("What do you want to send?");
        String text = sc.nextLine();
        // for (Node nodo: this.nodes.values()){
        //    sendMessage(Type.TEXT, nodo.getNodeIP());
        // }
    }

    private void sendMessage(Message message, String ip){
        try{
            //Status status = Status.error;
            new Thread(() -> {
                //Probably should probe the node before streaming
                try{
                    if(this.nodeSockets.containsKey(ip) && this.nodes.get(ip).getState() == Node.state.on){
                        Socket socket = this.nodeSockets.get(ip);
                        PrintWriter pw = new PrintWriter(socket.getOutputStream());
                        pw.println(message);
                        pw.flush();
                        //status=Status.success;
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                    System.out.println("Error sending message: "+e);
                }
            }).start();
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Error sending message: "+e);
            //return Status.error;
        }
    }

}