package overlay.TCP;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import overlay.state.NodeLink;
import overlay.state.NodeState;
import overlay.state.StreamLink;
import overlay.state.Vertex;
import streaming.UDP.UDPMiddleMan;
import streaming.UDP.UDPServer;

// gestor do nodo
// recebe mensagens e define como reagir a estas e como estas afetam o estado do nodo
public class TCPHandler {
    private NodeState state;
    private int nodeType;
    private Map<String, UDPServer> senders;
    private UDPServer server;
    private UDPMiddleMan middleman;

    public static final int PORT = 6667;

    public static final int NORMAL_NODE = 1;
    public static final int SERVER_NODE = 2;

    public TCPHandler(NodeState state, int nodeType){
        this.middleman = null;
        this.state = state;
        this.nodeType = nodeType;
        if (nodeType == NORMAL_NODE)
            this.senders = null;
        else{
            this.senders = new HashMap<>();
            this.state.addServer(this.state.getSelf());
        }
    }

    public void run(){
        try {
            ServerSocket server = new ServerSocket(PORT);

            startInitialClientThreads();

            if (this.nodeType == SERVER_NODE){
                startUDPServer();
                startMonitoring();
            }

            while(true){
                Socket client = server.accept();
                treatClient(client);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // define como reagir às mensagens recebidas
    public void treatClient(Socket client) throws Exception{
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

        while(true){
            String msg = in.readLine();
            System.out.println("S: " + msg);

            if (isPrefixOf(msg, "hello")){
                readHello(client, in, msg); break;
            }
            else if (isPrefixOf(msg, "probe")){
                readProbe(client, in, msg); break;
            }
            else if (isPrefixOf(msg, "new link")){
                readNewLink(client, in, msg, false); break;
            }
            else if (isPrefixOf(msg, "fixer new link")){
                readNewLink(client, in, msg, true); break;
            }
            else if (isPrefixOf(msg, "routes from")){
                readRoutes(in, msg); break;
            }
            else if (isPrefixOf(msg, "monitoring")){
                readMonitoring(client, in, msg); break;
            }
            else if (isPrefixOf(msg, "i want a stream")){
                sendStreamRequest(); break;
            }
            else if (isPrefixOf(msg, "want streaming")){
                readAskStreaming(in, msg); break;
            }
            else if (isPrefixOf(msg, "open UDP middleman")){
                readOpenUDPMiddleMan(client, in, msg); break;
            }
            else if (isPrefixOf(msg, "ack open UDP middleman")){
                readACKOpenUDPMiddleMan(client, in, msg); break;
            }
            else if (isPrefixOf(msg, "node closed")){
                readNodeClosed(client, msg); break;
            }
            else if (isPrefixOf(msg, "?")){
                readRequestLink(client, msg); break;
            }
            else if (isPrefixOf(msg, "fix stream")){
                readFixStream(false, in, msg); break;
            }
            else if (isPrefixOf(msg, "ack fix stream")){
                readFixStream(true, in, msg); break;
            }
            else if (isPrefixOf(msg, "change stream")){
                readChangeStream(false, in, msg); break;
            }
            else if (isPrefixOf(msg, "ack change stream")){
                readChangeStream(true, in, msg); break;
            }
            else if (isPrefixOf(msg, "stream changed")){
                readStreamChanged(in, msg); break;
            }
            else if (isPrefixOf(msg, "stream broken client")){
                sendCancelStream();
                sendStreamRequest();
                break;
            }
            else if (isPrefixOf(msg, "cancel stream client")){
                sendCancelStream(); break;
            }
            else if (isPrefixOf(msg, "cancel stream")){
                readCancelStream(msg); break;
            }
            else if (isPrefixOf(msg, "end"))
                break;
        }

        client.close();
    }


    /* READ FUNCTIONS */

    public void readHello(Socket client, BufferedReader in, String msg) throws Exception{
        String nodeName = this.state.findAdjNodeFromAddress(client.getInetAddress());
        boolean isServer = false;

        while(true){
            msg = in.readLine();

            if (isPrefixOf(msg, "i am server")){
                isServer = true;
            }
            // envia hello e ping de volta
            else if (isPrefixOf(msg, "end")){
                if (this.state.getAdjState(nodeName) == Vertex.OFF){
                    this.state.setAdjState(nodeName, Vertex.ON);
                    if (isServer)
                        this.state.addServer(nodeName);
                    startInitialClientThread(nodeName);
                }
                sendProbe(nodeName, true);
                break;
            }
        }
    }
    
    public void readProbe(Socket client, BufferedReader in, String msg) throws Exception{
        boolean initialMsg = isPrefixOf(msg, "probe: initial");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timestamp = getTimestampFromProbe(msg, initialMsg);
        Duration duration = Duration.between(timestamp, now);

        String nodeName = this.state.findAdjNodeFromAddress(client.getInetAddress());
        NodeLink link = new NodeLink(nodeName, nodeName, client.getInetAddress(), Math.abs(duration.toNanos()));

        // se o link criado pelo ping, for melhor que o atual
        // ou se as condições da ligação se modificarem
        // mudar o link na tabela e avisar adjacentes
        if(this.state.isLinkModified(nodeName, link)){
            this.state.addLink(nodeName, link);
            sendNewLinkToAdjacents(nodeName);
        }
        if(initialMsg){
            // se for o "handshake" inicial, enviar ao adjacente os melhores links que tem
            sendRoutesToNewAdj(nodeName);

            msg = in.readLine();
            if (msg == null)
                ;
            else if (isPrefixOf(msg, "i am server"))
                this.state.addServer(nodeName);
        }
    }

    public void readNewLink(Socket client, BufferedReader in, String msg, boolean fixer) throws Exception{
        String name, destination = "";
        if (fixer)
            name = getSuffixFromPrefix(msg, "fixer new link: ");
        else
            name = getSuffixFromPrefix(msg, "new link: ");
        String viaNode = "";
        InetAddress viaInterface = null;
        int hops = 0;
        long cost = 0;

        while(true){
            msg = in.readLine();

            if (isPrefixOf(msg, "via node")){
                viaNode = getSuffixFromPrefix(msg, "via node: ");
            }
            else if (isPrefixOf(msg, "hops")){
                String hopsString = getSuffixFromPrefix(msg, "hops: ");
                hops = Integer.parseInt(hopsString) + 1;
            }
            else if (isPrefixOf(msg, "cost")){
                String costString = getSuffixFromPrefix(msg, "cost: ");
                cost = Long.parseLong(costString);
                NodeLink adj = this.state.getLinkTo(viaNode);
                if (adj != null)
                    cost += adj.getCost();
            }
            else if (isPrefixOf(msg, "give to")){
                destination = getSuffixFromPrefix(msg, "give to: ");
            }
            else if (isPrefixOf(msg, "end")){
                List<InetAddress> ips = this.state.findAddressesFromAdjNode(viaNode);
                viaInterface = ips.get(0);
                NodeLink newLink = new NodeLink(name, viaNode, viaInterface, hops, cost);

                // se o novo tivesse sido dado como desligado, retira-o da lista de desligados
                if (this.state.isCloseNode(name))
                    this.state.removeClosedNode(name);

                // muda de link se as condições se alterarem ou se for melhor que o link atual
                // ou se o link reparar ligações cortadas
                // e envia aos seus adjacentes (exceto o nodo que enviou)
                if((fixer && destination.equals(this.state.getSelf())) || this.state.isLinkModified(name, newLink)){
                    NodeLink oldLink = this.state.getLinkTo(name);
                    this.state.addLink(name, newLink);
                    
                    sendNewLinkToAdjacents(name, viaNode);

                        if ((this.state.isNodeReceivingStream(name))){
                            StreamLink stream = this.state.getStreamFromReceivingNode(name);
                            // se o link reparar uma rota morta, envia aviso para reparar rota
                            if ((fixer || stream.getActive() == false) 
                                    && this.state.getSelf().equals(stream.getReceivingNode()) == false
                                    && viaNode.equals(stream.getServer()) == false){
                                sendFixStream(viaNode, String.valueOf(stream.getStreamID()), stream.getReceivingNode(), this.state.getSelf(), new String[0]);
                            }
                            else if (this.state.getSelf().equals(stream.getReceivingNode()) == false 
                                        && oldLink != null
                                        && oldLink.getViaNode().equals(viaNode) == false
                                        && viaNode.equals(stream.getServer()) == false)
                                // se encontrar um caminho melhor para um nodo que está a receber stream, envia aviso para modificar rota
                                sendChangeStream(viaNode, String.valueOf(stream.getStreamID()), stream.getReceivingNode(), this.state.getSelf(), new String[0]);
                        }

                    printState();
                }
                else if (destination.equals("") == false){
                    // se o link for destinado a outro nodo, reencaminha para esse nodo
                    sendNewLinkToAdjacent(destination, name, true);
                }
                else
                    System.out.println("new link refused");

                break;
            }
        }
    }

    // lê os melhores links que um nodo adjacente enviou
    public void readRoutes(BufferedReader in, String msg) throws Exception{
        String dest = "";
        String viaNode = "";
        InetAddress viaInterface = null;
        int hops = 0;
        long cost = 0;
        boolean isServer = false;

        while(true){
            msg = in.readLine();

            if (isPrefixOf(msg, "link to")){
                dest = getSuffixFromPrefix(msg, "link to: ");
            }
            else if (isPrefixOf(msg, "via node")){
                viaNode = getSuffixFromPrefix(msg, "via node: ");
            }
            else if (isPrefixOf(msg, "hops")){
                String hopsString = getSuffixFromPrefix(msg, "hops: ");
                hops = Integer.parseInt(hopsString) + 1;
            }
            else if (isPrefixOf(msg, "cost")){
                String costString = getSuffixFromPrefix(msg, "cost: ");
                cost = Long.parseLong(costString);
                NodeLink adj = this.state.getLinkTo(viaNode);
                cost += adj.getCost();
            }
            else if (isPrefixOf(msg, "is server")){
                isServer = true;
            }
            else if (isPrefixOf(msg, "route done")){
                List<InetAddress> ips = this.state.findAddressesFromAdjNode(viaNode);
                viaInterface = ips.get(0);
                NodeLink newLink = new NodeLink(dest, viaNode, viaInterface, hops, cost);

                // muda de link se as condições se alterarem ou se for melhor que o link atual
                // e envia aos seus adjacentes (exceto o nodo que enviou)
                if(this.state.isLinkModified(dest, newLink)){
                    this.state.addLink(dest, newLink);
                    sendNewLinkToAdjacents(dest, viaNode);
                }
                if (isServer){
                    this.state.addServer(dest);
                    isServer = false;
                }
            }
            else if (isPrefixOf(msg, "end"))
                break;
        }

        printState();
    }

    // lê mensagem de monitorização
    // (foi retirado o que estava no relatório sobre enviar a apenas um adjacente de cada vez a mensagem)
    public void readMonitoring(Socket client, BufferedReader in, String msg) throws Exception{
        String[] args = getNodesVisited(msg, "monitoring: ");

        String fromNode = args[args.length - 1];
        sendProbe(fromNode, false);
        sendMonitoringToAdjacents(args);
        String server = args[0];
        this.state.addServer(server);

        while(true){
            msg = in.readLine();
            
            if(isPrefixOf(msg, "probe")){
                readProbe(client, in, msg);
            }
            else if (isPrefixOf(msg, "end"))
                break;
        }
    }

    public void readAskStreaming(BufferedReader in, String msg) throws Exception{
        String server = "";
        String dest = getSuffixFromPrefix(msg, "want streaming: ");

        while(true){
            msg = in.readLine();

            if (isPrefixOf(msg, "from server")){
                server = getSuffixFromPrefix(msg, "from server: ");
            }
            else if (isPrefixOf(msg, "end"))
                break;
        }

        // se o nodo for servidor, avisa os nodos no percurso até quem está pedir stream para preparem o intermediário de UDP
        if(this.state.getSelf().equals(server)){
            int streamNr = getNodeNr(this.state.getSelf()) * 100 + (this.state.getNrStreams() + 1);
            String[] args = {String.valueOf(streamNr)};
            sendOpenUDPMiddleMan(args, dest);
        }
        else{
            // se o nodo estiver capacitado para reproduzir a stream (se passar por ele uma rota ativa), 
            // avisa os nodos no percurso até quem está pedir stream para preparem o intermediário de UDP
            if(this.state.anyActiveStreamWithoutDefects() == true){
                int streamNr = getNodeNr(this.state.getSelf()) * 100 + (this.state.getNrStreams() + 1);
                String[] args = {String.valueOf(streamNr)};
                sendOpenUDPMiddleMan(args, dest);
            }
            // em último caso, reencaminho o pedido de stream
            else
                sendStreamRequest(dest, server);
        }
    }

    public void readOpenUDPMiddleMan(Socket client, BufferedReader in, String msg) throws Exception{
        String dest = getSuffixFromPrefix(msg, "open UDP middleman: ");
        
        String[] args = {};
        while(true){
            msg = in.readLine();
            
            if(isPrefixOf(msg, "sent to")){
                args = getNodesVisited(msg, "sent to: ");
            }
            else if (isPrefixOf(msg, "end"))
                break;
        }

        // se não for o nodo for quem pediu stream, redireciona a mensagem e prepara o intermediário de UDP
        if (!this.state.getSelf().equals(dest)){
            sendOpenUDPMiddleMan(args, dest);
            startUDPMiddleMan();
        }
        else{
            // se o nodo for quem pediu stream, envia ack para ativar a rota
            String[] newArgs = new String[args.length + 1];
            for(int i = 0; i < args.length; i++)
                newArgs[i] = args[i];
            newArgs[args.length] = this.state.getSelf();

            String[] streamArgs = new String[args.length];
            for(int i = 0; i < args.length; i++)
                streamArgs[i] = newArgs[i + 1];

            StreamLink stream = new StreamLink(streamArgs, Integer.parseInt(newArgs[0]));
            this.state.addStream(stream);
            startUDPMiddleMan();
            sendACKOpenUDPMiddleMan(newArgs);
        }
    }

    public void readACKOpenUDPMiddleMan(Socket client, BufferedReader in, String msg) throws Exception{
        String[] args = {};

        while(true){
            msg = in.readLine();
            
            if(isPrefixOf(msg, "sent to")){
                args = getNodesVisited(msg, "sent to: ");
            }
            else if (isPrefixOf(msg, "end"))
                break;
        }

        String[] streamArgs = new String[args.length - 1];
        for(int i = 1; i < args.length; i++)
            streamArgs[i - 1] = args[i];
    
        StreamLink stream = new StreamLink(streamArgs, Integer.parseInt(args[0]));
        this.state.addStream(stream);

        // se for o nodo de onde se inicia a stream (servidor ou outro capaz), pára de reencaminhar
        if (this.state.getSelf().equals(args[1])){
            String from = this.state.findAdjNodeFromAddress(client.getInetAddress());

            if(this.state.isServer(this.state.getSelf()) == false)
                this.middleman.sendTo(stream);
        }
        // reencaminha a mensagem, caso contrário
        else{
            sendACKOpenUDPMiddleMan(args);
        }
    }

    public void readNodeClosed(Socket client, String msg) throws Exception{
        String closedNode = getSuffixFromPrefix(msg, "node closed: ");

        boolean isAdj = false;
        for(Map.Entry<String, List<InetAddress>> entry: this.state.getNodeAdjacents().entrySet())
            if (entry.getKey().equals(closedNode)){
                isAdj = true;
                break;
            }

        // reencaminha a mensagem que um certo nodo se fechou e pede link para os links que tinha dependentes desse nodo
        if ((this.state.getAdjState(closedNode) == Vertex.ON || isAdj == false) && this.state.isCloseNode(closedNode) == false){
            this.state.addCloseNode(closedNode);
            String from = this.state.findAdjNodeFromAddress(client.getInetAddress());
            List<String> lostNodes = this.state.handleClosedNode(closedNode);
            sendNodeClosed(from, closedNode);
            if (lostNodes.size() > 0)
                askLinkTo(lostNodes);

            printState();
        }
    }

    public void readRequestLink(Socket client, String msg) throws Exception{
        String from = this.state.findAdjNodeFromAddress(client.getInetAddress());
        String to = getSuffixFromPrefix(msg, "? ");

        boolean lostNode = this.state.removeDependentLink(from, to);
        // se receber esta mensagem de outro nodo (n1), a pedir para um nodo (n2)
        // verifica se o seu link para n2 passa por n1, se sim, remove esse link
        // e pede também nova ligação
        if (lostNode){
            List<String> lostNodes = new ArrayList<>();
            lostNodes.add(to);
            askLinkTo(lostNodes);
        }

        // se tiver link, envia-o
        if (this.state.getLinkTo(to) != null){
            sendNewLinkToAdjacent(from, to, true);
        }
    }

    public void readCancelStream(String msg) throws Exception{
        String[] args = getNodesVisited(msg, "cancel stream: ");

        StreamLink stream = this.state.getStreamFromArgs(args);

        if (this.state.getSelf().equals(args[0])){
            if (this.state.isServer(this.state.getSelf())){
                this.state.removeStream(stream);
            }
            // cancela reencaminhamento por esta rota
            else
                this.middleman.doNotSendTo(stream);
        }
        else{
            // reencaminha a mensagem de cancelar stream
            String nextNode = stream.findNextNode(this.state.getSelf(), true);

            List<InetAddress> ips = this.state.findAddressesFromAdjNode(nextNode);
            Thread client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.CANCEL_STREAM, args));
            client.run();

            if (this.middleman.hasDependentStreams(stream))
                this.middleman.removeAllMyStreams();
        }

        this.state.removeStream(stream);
    }
    

    public void readFixStream(boolean ack, BufferedReader in, String msg) throws Exception{
        String streamID;
        if (ack)
            streamID = getSuffixFromPrefix(msg, "ack fix stream: ");
        else
            streamID = getSuffixFromPrefix(msg, "fix stream: ");
        String rcv = "";
        String orderedBy = "";
        String[] nodesVisited = {};

        while(true){
            msg = in.readLine();

            if(isPrefixOf(msg, "leading to"))
                rcv = getSuffixFromPrefix(msg, "leading to: ");
            else if(isPrefixOf(msg, "ordered by"))
                orderedBy = getSuffixFromPrefix(msg, "ordered by: ");
            else if (isPrefixOf(msg, "going through")){
                nodesVisited = getNodesVisited(msg, "going through: ");
            }
            else if (isPrefixOf(msg, "end")){
                break;
            }
        }

        StreamLink oldStream = this.state.getStreamFromID(Integer.parseInt(streamID));
        if (oldStream == null || oldStream.getActive() == false){
            if (ack){
                StreamLink stream = this.state.fixStream(streamID, rcv, nodesVisited, orderedBy);
            
                if(stream != null){
                    // se não for o nodo responsável pela stream, reencaminha a mensagem
                    if(this.state.getSelf().equals(stream.getServer()) == false){
                        sendAckFixStream(streamID, stream, orderedBy);
                    }
                }
                // se o nodo for novo na rota da stream, adiciona-a        
                else{
                    stream = new StreamLink(nodesVisited, rcv, Integer.parseInt(streamID), true, orderedBy, this.state.getSelf());
                    this.state.addStream(stream);
                    sendAckFixStream(streamID, stream, orderedBy);
                }
            }
            else{
                // se não for o destinatário da stream, reencaminha a mensagem e prepara intermediário de UDP
                if(this.state.getSelf().equals(rcv) == false){
                    startUDPMiddleMan();
                    sendFixStream(rcv, streamID, rcv, orderedBy, nodesVisited);
                }
                // se for o destinário da stream, envia confirmação para ativar a rota
                else{
                    StreamLink stream = this.state.fixStream(streamID, rcv, nodesVisited, orderedBy);
                    sendAckFixStream(streamID, stream, orderedBy);
                }
            }
        }
    }

    public void readChangeStream(boolean ack, BufferedReader in, String msg) throws Exception{
        String streamID;
        if (ack)
            streamID = getSuffixFromPrefix(msg, "ack change stream: ");
        else
            streamID = getSuffixFromPrefix(msg, "change stream: ");
        String rcv = "";
        String orderedBy = "";
        String[] nodesVisited = {};

        while(true){
            msg = in.readLine();

            if(isPrefixOf(msg, "leading to"))
                rcv = getSuffixFromPrefix(msg, "leading to: ");
            else if(isPrefixOf(msg, "ordered by"))
                orderedBy = getSuffixFromPrefix(msg, "ordered by: ");
            else if (isPrefixOf(msg, "going through")){
                nodesVisited = getNodesVisited(msg, "going through: ");
            }
            else if (isPrefixOf(msg, "end")){
                break;
            }
        }

        if (ack){
            StreamLink stream = this.state.changeStream(streamID, rcv, nodesVisited, orderedBy);
            
            if(stream != null){
                // se não for o nodo responsável pela stream, reencaminha a mensagem
                if(this.state.getSelf().equals(stream.getServer()) == false){
                    sendAckChangeStream(streamID, stream, orderedBy);
                }
            }
            // se o nodo for novo na rota da stream, adiciona-a
            else{
                stream = new StreamLink(nodesVisited, rcv, Integer.parseInt(streamID), true, orderedBy, this.state.getSelf());
                this.state.addStream(stream);
                sendAckChangeStream(streamID, stream, orderedBy);
            }
        }
        else{
            // se não for o destinatário da stream, reencaminha a mensagem e prepara intermediário de UDP
            if(this.state.getSelf().equals(rcv) == false){
                startUDPMiddleMan();
                sendChangeStream(rcv, streamID, rcv, orderedBy, nodesVisited);
            }
            // se for o destinário da stream, envia confirmação para ativar a rota
            else{
                StreamLink stream = this.state.changeStream(streamID, rcv, nodesVisited, orderedBy);
                sendAckChangeStream(streamID, stream, orderedBy);
            }
        }
    }

    public void readStreamChanged(BufferedReader in, String msg) throws Exception{
        String streamID = getSuffixFromPrefix(msg, "stream changed: ");
        String rcv = "";
        String[] nodesVisited = {};

        while(true){
            msg = in.readLine();

            if(isPrefixOf(msg, "leading to"))
                rcv = getSuffixFromPrefix(msg, "leading to: ");
            else if (isPrefixOf(msg, "going through")){
                nodesVisited = getNodesVisited(msg, "going through: ");
            }
            else if (isPrefixOf(msg, "end")){
                break;
            }
        }

        StreamLink stream = this.state.getStreamFromID(Integer.parseInt(streamID));
        if (stream != null){
            if (state.getSelf().equals(stream.getReceivingNode()) == false)
                sendStreamChanged(streamID, stream);
        }
    }


    /* THREAD FUNCTIONS */

    public void startInitialClientThreads() throws InterruptedException{
        Map<String, Integer> adjsState = this.state.getNodeAdjacentsState();
        Map<String, List<InetAddress>> adjs = this.state.getNodeAdjacents();

        for(Map.Entry<String, Integer> entry: adjsState.entrySet()){
            if (entry.getValue() == Vertex.ON){
                List<InetAddress> ips = adjs.get(entry.getKey());
                
                Thread client;
                client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.HELLO));
                client.start();
                client.join();
            }
        }
    }

    public void startInitialClientThread(String key) throws InterruptedException{
        List<InetAddress> ips = this.state.findAddressesFromAdjNode(key);

        Thread client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.HELLO));
        client.start();
        client.join();
    }

    public void startUDPServer(){
        UDPServer server = new UDPServer(this.state);
        this.server = server;
        server.start();
    }

    public void startUDPMiddleMan(){
        if (this.middleman == null){
            UDPMiddleMan middleman = new UDPMiddleMan(this.state);
            this.middleman = middleman;
            middleman.start();
        }
    }

    public void startMonitoring(){
        Timer timer = new Timer();
        timer.schedule(new TCPMonitor(state), 0, 1000);
    }

    public void sendStreamRequest(){
        NodeLink link = this.state.getClosestServer();
        Thread client = new Thread(new TCPCommunicator(this.state, link.getViaInterface(), TCPCommunicator.ASK_STREAMING, link.getDest()));
        client.run();
    }

    public void sendStreamRequest(String dest, String server){
        NodeLink link = this.state.getLinkTo(server);
        String[] args = {dest, server};
        Thread client = new Thread(new TCPCommunicator(this.state, link.getViaInterface(), TCPCommunicator.REDIRECT_ASK_STREAMING, args));
        client.run();
    }

    public void sendCancelStream() throws Exception{
        StreamLink myStream = this.state.getMyStream();

        if(myStream != null){
            String[] args = myStream.convertLinkToArgs();
            String nextNode = myStream.findNextNode(this.state.getSelf(), true);
            List<InetAddress> ips = this.state.findAddressesFromAdjNode(nextNode);
            
            if (ips != null){
                Thread client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.CANCEL_STREAM, args));
                client.run();
            }
            this.state.removeStream(myStream);
        }
    }

    public void sendProbe(String key, boolean initial) throws InterruptedException{
        List<InetAddress> ips = this.state.findAddressesFromAdjNode(key);

	for(InetAddress ip: ips){
        Thread client;
            if (initial)
            	client = new Thread(new TCPCommunicator(this.state, ip, TCPCommunicator.PROBE_INITIAL));
            else
            	client = new Thread(new TCPCommunicator(this.state, ip, TCPCommunicator.PROBE_REGULAR));
            client.start();
            client.join();
	}
    }

    public void sendNewLinkToAdjacent(String fromNode, String to, boolean fixer) throws InterruptedException{
        List<InetAddress> ips = this.state.findAddressesFromAdjNode(fromNode);

        Thread client;
        if (ips != null){
            if (fixer)
                client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.SEND_NEW_LINK_FIXER, to));
            else
                client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.SEND_NEW_LINK, to));
            client.start();
            client.join();
        }
    }

    public void sendNewLinkToAdjacents(String fromNode) throws InterruptedException{
        Map<String, Integer> adjsState = this.state.getNodeAdjacentsState();
        Map<String, List<InetAddress>> adjs = this.state.getNodeAdjacents();

        for(Map.Entry<String, Integer> entry: adjsState.entrySet()){
            if (entry.getValue() == Vertex.ON){
                if(!entry.getKey().equals(fromNode)){
                    List<InetAddress> ips = adjs.get(entry.getKey());
                    Thread client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.SEND_NEW_LINK, fromNode));
                    client.start();
                    client.join();
                }
            }
        }
    }

    public void sendNewLinkToAdjacents(String fromNode, String viaNode) throws InterruptedException{
        Map<String, Integer> adjsState = this.state.getNodeAdjacentsState();
        Map<String, List<InetAddress>> adjs = this.state.getNodeAdjacents();

        for(Map.Entry<String, Integer> entry: adjsState.entrySet()){
            if (entry.getValue() == Vertex.ON){
                if(!entry.getKey().equals(fromNode) && !entry.getKey().equals(viaNode)){
                    List<InetAddress> ips = adjs.get(entry.getKey());
                    Thread client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.SEND_NEW_LINK, fromNode));
                    client.start();
                    client.join();
                }
            }
        }
    }

    public void sendRoutesToNewAdj(String fromNode) throws InterruptedException{
        List<InetAddress> ips = this.state.findAddressesFromAdjNode(fromNode);

        Thread client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.SEND_ROUTES, fromNode));
        client.start();
        client.join();
    }

    public void sendMonitoringToAdjacents(String[] nodesVisited) throws InterruptedException{
        Map<String, Integer> adjsState = this.state.getNodeAdjacentsState();
        Map<String, List<InetAddress>> adjs = this.state.getNodeAdjacents();

        for(Map.Entry<String, Integer> entry: adjsState.entrySet()){
            if (entry.getValue() == Vertex.ON){
                if(!isNodeInArray(nodesVisited, entry.getKey())){
                    List<InetAddress> ips = adjs.get(entry.getKey());
                    Thread client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.MONITORING, nodesVisited));
                    client.start();
                    client.join();
                }
            }
        }
    }

    public void sendOpenUDPMiddleMan(String[] nodesInfo, String dest){
        String[] newInfo = new String[nodesInfo.length + 2];
        for(int i = 0; i < nodesInfo.length; i++)
            newInfo[i] = nodesInfo[i];
        
        newInfo[nodesInfo.length] = this.state.getSelf();
        newInfo[nodesInfo.length + 1] = dest;

        NodeLink link = this.state.getLinkTo(dest);
        Thread client = new Thread(new TCPCommunicator(this.state, link.getViaInterface(), TCPCommunicator.OPEN_UDP_MIDDLEMAN, newInfo));
        client.start();
    }

    public void sendACKOpenUDPMiddleMan(String[] args){
        String dest = "";
        for(int i = 0; i < args.length; i++){
            if (args[i].equals(this.state.getSelf())){
                dest = args[i - 1];
                break;
            }
        }

        List<InetAddress> ips = this.state.findAddressesFromAdjNode(dest);
        Thread client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.ACK_OPEN_UDP_MIDDLEMAN, args));
        client.start();
    }

    public void sendNodeClosed(String from, String node) throws Exception{
        Map<String, Integer> adjsState = this.state.getNodeAdjacentsState();
        Map<String, List<InetAddress>> adjs = this.state.getNodeAdjacents();

        for(Map.Entry<String, Integer> entry: adjsState.entrySet()){
            if (entry.getValue() == Vertex.ON && entry.getKey().equals(from) == false){
                List<InetAddress> ips = adjs.get(entry.getKey());
                Thread client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.CLOSED_NODE, node));
                client.start();
                client.join();
            }
        }
    }

    public void askLinkTo(List<String> lostLinks) throws Exception{
        Map<String, Integer> adjsState = this.state.getNodeAdjacentsState();
        Map<String, List<InetAddress>> adjs = this.state.getNodeAdjacents();

        for(Map.Entry<String, Integer> entry: adjsState.entrySet()){
            if (entry.getValue() == Vertex.ON){
                List<InetAddress> ips = adjs.get(entry.getKey());
                for(String node: lostLinks){
                    Thread client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.REQUEST_LINK, node));
                    client.start();
                    client.join();
                }
            }
        }
    }

    public void sendFixStream(String dest, String streamID, String rcvNode, String orderedBy, String[] nodesVisited){
        String[] args = new String[4 + nodesVisited.length];
        args[0] = String.valueOf(streamID); 
        args[1] = rcvNode;
        args[2] = orderedBy;
        int i;
        for(i = 0; i < nodesVisited.length; i++){
            args[i + 3] = nodesVisited[i];
        }
        args[i + 3] = this.state.getSelf();

        NodeLink link = this.state.getLinkTo(dest);
        StreamLink stream = this.state.getStreamFromID(Integer.parseInt(streamID));
        if ((stream == null || dest.equals(stream.getServer()) == false) && link != null){
            Thread client = new Thread(new TCPCommunicator(this.state, link.getViaInterface(), TCPCommunicator.FIX_STREAM, args));
            client.start();
        }
    }

    public void sendAckFixStream(String streamID, StreamLink stream, String orderedBy) throws Exception{
        List<String> path = stream.getStream();
        String[] args = new String[2 + path.size()];
        args[0] = streamID;
        args[1] = path.get(path.size() - 1);
        args[2] = stream.getChangeAt();
        for(int i = 0; i < path.size() - 1; i++)
            args[i + 3] = path.get(i);

        String nextNode = stream.findNextNode(this.state.getSelf(), true);
        List<InetAddress> ips = this.state.findAddressesFromAdjNode(nextNode);
        Thread client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.ACK_FIX_STREAM, args));
        client.run();
    }

    public void sendChangeStream(String dest, String streamID, String rcvNode, String orderedBy, String[] nodesVisited){
        String[] args = new String[4 + nodesVisited.length];
        args[0] = String.valueOf(streamID); 
        args[1] = rcvNode;
        args[2] = orderedBy;
        int i;
        for(i = 0; i < nodesVisited.length; i++){
            args[i + 3] = nodesVisited[i];
        }
        args[i + 3] = this.state.getSelf();

        NodeLink link = this.state.getLinkTo(dest);
        StreamLink stream = this.state.getStreamFromID(Integer.parseInt(streamID));
        if ((stream == null || dest.equals(stream.getServer()) == false) && link != null){
            Thread client = new Thread(new TCPCommunicator(this.state, link.getViaInterface(), TCPCommunicator.CHANGE_STREAM, args));
            client.start();
        }
    }


    public void sendAckChangeStream(String streamID, StreamLink stream, String orderedBy) throws Exception{
        List<String> path = stream.getStream();
        if(path != null){
            String[] args = new String[2 + path.size()];
            args[0] = streamID;
            args[1] = path.get(path.size() - 1);
            args[2] = orderedBy;
            for(int i = 0; i < path.size() - 1; i++)
                args[i + 3] = path.get(i);

            String nextNode = stream.findNextNode(this.state.getSelf(), true);
            List<InetAddress> ips = this.state.findAddressesFromAdjNode(nextNode);
            Thread client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.ACK_CHANGE_STREAM, args));
            client.run();
        }
    }

    public void sendStreamChanged(String streamID, StreamLink stream) throws Exception{
        List<String> path = stream.getStream();
        String[] args = new String[1 + path.size()];
        args[0] = streamID;
        args[1] = path.get(path.size() - 1);
        for(int i = 0; i < path.size() - 1; i++)
            args[i + 2] = path.get(i);

        String nextNode = stream.findNextNode(this.state.getSelf(), true);
        List<InetAddress> ips = this.state.findAddressesFromAdjNode(nextNode);
        Thread client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.STREAM_CHANGED_COURSE, args));
        client.run();
    }


    /* AUXILIARY READ FUNCTIONS */

    public static boolean isPrefixOf(String msg, String prefix){
        boolean res = true;
        char[] msgv = msg.toCharArray();
        char[] pv = prefix.toCharArray();

        for(int i = 0; i < pv.length && i < msgv.length && res; i++)
            if (pv[i] != msgv[i])
                res = false;

        return res;
    }

    public boolean isNodeInArray(String[] nodes, String node){
        boolean res = false;

        for(String s: nodes)
            if (node.equals(s)){
                res = true;
                break;
            }

        return res;
    }

    public void printState(){
        System.out.println("\n__________________________________________________\n\nESTADO");
        System.out.println(this.state.toString());
        System.out.println("__________________________________________________");
    }


    public static String getSuffixFromPrefix(String msg, String prefix){
        StringBuilder sb = new StringBuilder();
        char[] msgv = msg.toCharArray();

        for(int i = prefix.length(); i < msgv.length; i++)
            sb.append(msgv[i]);

        return sb.toString();
    }

    public LocalDateTime getTimestampFromProbe(String msg, boolean initial){
        String probe;

        if (initial)
            probe = getSuffixFromPrefix(msg, "probe: initial: ");
        else
            probe = getSuffixFromPrefix(msg, "probe: regular: ");

        return LocalDateTime.parse(probe);
    }

    public String[] getNodesVisited(String msg, String prefix){
        String nodes = getSuffixFromPrefix(msg, prefix);
        return nodes.split(" ");
    }

    public String[] getServers(String msg){
        String nodes = getSuffixFromPrefix(msg, "servers: ");
        return nodes.split(" ");
    }

    public int getNodeNr(String node){
        char[] bytes = node.toCharArray();

        StringBuilder sb = new StringBuilder();
        for(int i = 1; i < bytes.length; i++)
            sb.append(bytes[i]);

        return Integer.parseInt(sb.toString());
    }

    public String getNodeNrName(String node){
        char[] bytes = node.toCharArray();

        StringBuilder sb = new StringBuilder();
        for(int i = 1; i < bytes.length; i++)
            sb.append(bytes[i]);

        return sb.toString();
    }
}
