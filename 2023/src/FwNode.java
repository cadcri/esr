
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Scanner;
import java.net.Socket;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.concurrent.*;



public class FwNode{

    private int streamPort = 30000;
    private int listeningPort = 30001;
    //private int probePort = 30002;

    private HashMap<String, Node> nodes = new HashMap<String, Node>();
    private HashMap<String, Node> graph = new HashMap<String, Node>();
    private HashMap<String, Socket> nodeSockets = new HashMap<String, Socket>();



    private Socket listeningSocket;

    private enum Status{
        //Set specific error status
        success,
        error
    }

    public enum Message{
        PROBE,
        ACK,
        JOIN,
        JOIN_ACK,
        LEAVE,
        LEAVE_ACK,
        TEXT
    }

    public FwNode(Node node){
    
        // new Thread(() -> {
        //     try{
        //         ServerSocket socketTemp = new ServerSocket(listeningPort);
        //         this.listeningSocket = socketTemp.accept();
        //     }
        //     catch(Exception e){
        //         e.printStackTrace();
        //     }
        // }).start();

        

        System.out.println(node.toString());
        this.nodes=node.getNeighbors();
        System.out.println(this.nodes.size());

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
                    probeAllConnections();
                    break;
                
                case("3"):
                    sendTextToAllNodes();
                    break;
            }

        }
        scanner.close();
    }

    // public RP(HashMap<String, Node> nodes){
    
    //     new Thread(() -> {
    //         try{
    //             ServerSocket socketTemp = new ServerSocket(listeningPort);
    //             this.listeningSocket = socketTemp.accept();
    //         }
    //         catch(Exception e){
    //             e.printStackTrace();
    //         }
    //         }).start();

        
    //     this.nodes=nodes;
    //     System.out.println(this.nodes.size());

    //     String option = "";
    //     Scanner scanner = new Scanner(System.in);  

    //     while(!option.equals("4")){
    //         System.out.println("What do you want to do?");
    //         System.out.println("1. List all nodes");
    //         System.out.println("2. Probe all nodes");
    //         System.out.println("3. Send text to all nodes");
    //         System.out.println("4. Exit");
    //         option = scanner.nextLine();

    //         switch(option){

    //             case("1"):
    //                 listAllNodes();
    //                 break;

    //             case("2"):
    //                 probeAllConnections();
    //                 break;
                
    //             case("3"):
    //                 sendTextToAllNodes();
    //                 break;
    //         }

    //     }
    //     scanner.close();
    // }

    private void listAllNodes(){
        System.out.println("Listing all nodes");
        for(Node nodo: this.nodes.values()){
            System.out.println(nodo.toString());
        }
    }

    private void sendTextToAllNodes(){
        Scanner sc = new Scanner(System.in);
        System.out.println("What do you want to send?");
        String text = sc.nextLine();
        for (Node nodo: this.nodes.values()){
           sendMessage(Message.TEXT, nodo.getNodeIP());
        }
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
                    //status=Status.error;
                }
            }).start();
            //return status;
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Error sending message: "+e);
            //return Status.error;
        }
    }

    private void generateGraph(){
        //generate graph only for nodes that are "probable"
        //generate graph
        //for each node
        //  for each neighbour
        //      if neighbour is not in graph
        //          add neighbour to graph
        //      add edge between node and neighbour
        for(Node nodo: this.nodes.values()){

            // for(Node vizinho : nodo.getNeighbors()){

            // }
        }

    }


    private void probeAllConnections(){
        try{
            for (Node nodo: this.nodes.values()){
               if (probeNode(nodo.getNodeIP()) == Status.error){
                   nodo.setNodeState(Node.state.off);
                   System.out.println("Node "+nodo.getNodeName()+" is off");
               }
               else{
                    nodo.setNodeState(Node.state.on);
                    System.out.println("Node "+nodo.getNodeName()+" is on");
               }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Status probeNode(String ip){

        try{
            System.out.println("Probing node: "+ ip);
            Socket socket;
            if (!nodeSockets.containsKey(ip)) {
                socket = new Socket(ip, streamPort);
                nodeSockets.put(ip, socket);
            } else {
                socket = nodeSockets.get(ip);
            }
            
            //I have to send a message to the node and then interpret the answer if i receive the ack then the node is on
            sendMessage(Message.PROBE, ip);
            return Status.success;
        }
        catch(Exception e){
            System.out.println("Error probing node: "+ip);
            return Status.error;
        }
    }


}