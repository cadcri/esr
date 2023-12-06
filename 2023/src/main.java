import Nodes.*;
import Structs.*;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.net.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;



public class main {

    private static String hostname="";

    public static void main(String[] args){
        
        XMLParser parser = new XMLParser();
        HashMap<String, Node> nodes=new HashMap<>();
        if (args.length==0){
            //System.out.println("Please specify the path to the overlay file");
            //return;
            nodes = parser.parse("teste.xml");
            getMyHostname();
        }
        else{
            System.out.println(args[0]);
            nodes = parser.parse(args[0]);
            getMyHostname();
        }

        
        //System.out.println(manager.nodes.size());
        // for(Node nodo: manager.nodes.values()){
        //     System.out.println(nodo.toString());
        // }




        // for(String nodoName: nodeManager.nodes.keySet()){
        //     System.out.println("Node name: "+nodoName);
        //     System.out.println("Vizinhos:");
        //     for (Node nodo : nodeManager.nodes.get(nodoName).getNeighbors().values()){
        //         System.out.println(nodo.getNodeName()+ " "+ nodo.getNodeIP());
        //     }
        // }

    
        try{
            Node nodo = nodes.get(hostname);
            if(nodo==null){
                System.out.println("Node not found");
                return;
            }
            else if(nodo.getNodeType()==Node.type.rp){
                System.out.println("RP");
                new RP(nodo);
            }
            else if(nodo.getNodeType()==Node.type.client){
                System.out.println("Client");
                new Client(nodo);
            }
            else if(nodo.getNodeType()==Node.type.node){
                System.out.println("Node");
                new FwNode(nodo);
                //node.start();
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        
    }


    private static void getMyHostname(){
        try {
            Process process = Runtime.getRuntime().exec("hostname");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            hostname=line;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
