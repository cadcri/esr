package overlay;

import overlay.TCP.TCPHandler;
import overlay.bootstrapper.BStrapper;
import overlay.bootstrapper.BStrapperClient;
import overlay.bootstrapper.ConfigParser;
import overlay.state.Graph;
import overlay.state.NodeState;

public class NodeManager {
    public static void main(String[] args){
        
        // executa um nodo normal
        if (args.length == 1){
            NodeState state = BStrapperClient.readInitialMsg(args[0]);

            if (state != null){
                TCPHandler server = new TCPHandler(state, TCPHandler.NORMAL_NODE);
                server.run();
            }
        }

        // executa um servidor
        else if (args.length == 2 && args[1].equals("server")){
            NodeState state = BStrapperClient.readInitialMsg(args[0]);

            if (state != null){
                TCPHandler server = new TCPHandler(state, TCPHandler.SERVER_NODE);
                server.run();
            }
        }

        // executa o bootstrapper como nodo normal
        else if (args.length == 2 && args[0].equals("config")){
            ConfigParser parser = new ConfigParser(args[1]);
            Graph graph = parser.parseXML();

            Thread bstrapper = new Thread(new BStrapper(graph));
            bstrapper.start();

            NodeState state = graph.graphToNodeState(parser.getBootstrapperName());
            TCPHandler server = new TCPHandler(state, TCPHandler.NORMAL_NODE);
            server.run();

            try {
                bstrapper.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // executa o bootstrapper como servidor
        else if (args.length == 3 && args[0].equals("config") && args[2].equals("server")){
            ConfigParser parser = new ConfigParser(args[1]);
            Graph graph = parser.parseXML();

            Thread bstrapper = new Thread(new BStrapper(graph));
            bstrapper.start();

            NodeState state = graph.graphToNodeState(parser.getBootstrapperName());
            TCPHandler server = new TCPHandler(state, TCPHandler.SERVER_NODE);
            server.run();

            try {
                bstrapper.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
