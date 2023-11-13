
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.net.Socket;


public class RP{

    private HashMap<String, Node> nodes = new HashMap<String, Node>();

    public RP(HashMap<String, Node> nodes){
        this.nodes=nodes;
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
                    //listAllNodes();
                    break;

                case("2"):
                    probeAllConnections();
                    break;
                
                case("3"):
                    //sendTextToAllNodes();
                    break;


            }

        }
    }

    private void probeAllConnections(){
        ArrayList <Node> unsuccesfulNodes = new ArrayList<Node>();
        try{
            for (Node nodo: this.nodes.values()){
                if(nodo.getNodeType()==Node.type.rp){
                    continue;
                }
                else{
                    try{
                        System.out.println("Probing node: "+nodo.getNodeIP());
                        Socket socket = new Socket(nodo.getNodeIP(), 5000);
                        socket.close();
                    }
                    catch(Exception e){
                        unsuccesfulNodes.add(nodo);
                    }

                }
            }

            System.out.println("Down nodes: "+unsuccesfulNodes.size());
        }
        catch(Exception e){
            System.out.println("Error probing nodes");
        }
    }

}