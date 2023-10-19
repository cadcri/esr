package overlay.bootstrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import overlay.TCP.TCPMessageSender;
import overlay.state.NodeState;
import overlay.state.Vertex;


public class BStrapperClient{

    // nodo cria o seu estado inicial a partir da ligação ao bootstrapper
    public static NodeState readInitialMsg(String bstrapper){
        try {
            // nodo ao ligar-se à rede, envia "hello" ao bootstrapper e recebe informação sobre ele próprio e sobre os seus vizinhos
            InetAddress bstrapperIP = InetAddress.getByName(bstrapper);

            Socket socket = new Socket(bstrapperIP, BStrapper.PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            TCPMessageSender sender = new TCPMessageSender(out);

            sender.hello();
            NodeState state = getInitialMsg(in, socket, sender, bstrapperIP);
            socket.close();

            return state;
        } 
        catch (Exception e) {
            System.out.println("Erro ao ler a informação enviado pelo bootstrapper!");
            System.out.println("Verifique que o bootstrapper está ativo.");
            return null;
        }
    }

    public static NodeState getInitialMsg(BufferedReader in, Socket socket, TCPMessageSender sender, InetAddress bstrapperIP) throws IOException{
        String name = "";
        List<InetAddress> ips = new ArrayList<>();
        Map<String, List<InetAddress>> adjs = new HashMap<>();
        Map<String, Integer> adjsState = new HashMap<>();

        String currentAdj = "";
        while(true){
            String msg = in.readLine();
            if(msg.equals("end")){
                sender.ack();
                break;
            }

            String[] tokens = msg.split(": ");
            if (tokens[0].equals("YOU"))
                name = tokens[1];

            else if (tokens[0].equals("ADJ")){
                currentAdj = tokens[1];
                
                if (tokens[2].equals("OFF"))
                    adjsState.put(currentAdj, Vertex.OFF);
                else if (tokens[2].equals("ON"))
                    adjsState.put(currentAdj, Vertex.ON);

                adjs.put(currentAdj, new ArrayList<>());
            }

            else if (tokens[0].equals("You're available at")){
                ips.add((InetAddress) InetAddress.getByName(tokens[1]));
            }

            else if (tokens[0].equals("Available at")){
                List<InetAddress> list = adjs.get(currentAdj);
                list.add((InetAddress) InetAddress.getByName(tokens[1]));
                adjs.put(currentAdj, list);
            }
        }

        Vertex v = new Vertex(name, ips, adjs, adjsState, Vertex.ON);
        return new NodeState(v, bstrapperIP);
    }
}
