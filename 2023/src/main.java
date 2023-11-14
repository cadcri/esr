//package src;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.net.*;




public class main {

    private static String ip="";
    private static String rpIp="";

    public static void main(String[] args){
        
        nodeManager manager = new nodeManager();
        if (args.length==0){
            //System.out.println("Please specify the path to the overlay file");
            //return;
            manager.parseNodes("../topologia_simples1.imn");
        }
        else{
            System.out.println(args[0]);
            manager.parseNodes(args[0]);
        }
        //System.out.println(manager.nodes.size());
        // for(Node nodo: manager.nodes.values()){
        //     System.out.println(nodo.toString());
        // }

        getMyIp();

        //System.out.println(nodeManager.nodes.keySet().toString());
        for (String nodoName: nodeManager.nodes.keySet()){
            if(nodeManager.nodes.get(nodoName).getNodeIP().equals(ip)){
                Node.type nodeType = nodeManager.nodes.get(nodoName).getNodeType();
                if (nodeType == (Node.type.rp)){
                    System.out.println("I am the rendevouz point");
                    //Should clone the hashmap
                    //Removing the rp since he shouldnt send messages to himself
                    nodeManager.nodes.remove(nodoName);
                    new RP(nodeManager.nodes);
                    break;
                }
                else if(nodeType == (Node.type.client)){
                    System.out.println("I am a client");
                    new Client(nodeManager.nodes.get(nodeManager.rpName).getNodeIP());
                    break;
                }
                else{
                    System.out.println("I am a node");
                }
            }
        }
        
    }

    private static void getMyIp(){
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ip = socket.getLocalAddress().getHostAddress();
        }
        catch(Exception e){
            e.printStackTrace();
        }

    }
}
