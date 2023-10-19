package overlay.bootstrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;

import overlay.TCP.TCPCommunicator;
import overlay.TCP.TCPHandler;
import overlay.TCP.TCPMessageSender;
import overlay.state.Graph;
import overlay.state.Vertex;

public class BStrapper extends Thread{
    private Graph graph;
    public static final int PORT = 6666;

    public BStrapper(Graph graph){
        this.graph = graph;
    }

    public void run(){
        try{
            ServerSocket server = new ServerSocket(PORT);
            
            while(true){
                Socket client = server.accept();
                treatClient(client);
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public void treatClient(Socket client) throws IOException{
        PrintWriter out = new PrintWriter(client.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        TCPMessageSender sender = new TCPMessageSender(out);
        
        while(true){
            String msg = in.readLine();
            System.out.println("B: " + msg);

            if (msg.equals("hello")){
                String nodeName = this.graph.getNameFromIP(client.getInetAddress());
                this.graph.setNodeState(nodeName, Vertex.ON);
                List<InetAddress> ips = this.graph.getNodeIPList(nodeName);
                Map<String, List<InetAddress>> adjs = this.graph.getNodeAdjacents(nodeName);
                Map<String, Integer> adjsState = this.graph.getNodeAdjacentsState(nodeName);
                sender.initialMessageBootstrapper(nodeName, ips, adjs, adjsState);
            }
            else if (TCPHandler.isPrefixOf(msg, "node closed")){
                String closedNode = TCPHandler.getSuffixFromPrefix(msg, "node closed: ");

                if (this.graph.getNodeState(closedNode) == Vertex.ON){
                    Map<String, List<InetAddress>> adjs = this.graph.getNodeAdjacents(closedNode);
                    Map<String, Integer> adjsState = this.graph.getNodeAdjacentsState(closedNode);

                    for(Map.Entry<String, Integer> entry: adjsState.entrySet()){
                        if (entry.getValue() == Vertex.ON){
                            InetAddress ip = adjs.get(entry.getKey()).get(0);
                            Thread warnThread = new Thread(new TCPCommunicator(null, ip, TCPCommunicator.CLOSED_NODE, closedNode));
                            warnThread.start();
                        }
                    }
                    
                    this.graph.setNodeState(closedNode, Vertex.OFF);
                }
            }
            else if (msg.equals("ack"))
                break;
            else if (msg.equals("end"))
                break;
        }

        client.close();
    }
}
