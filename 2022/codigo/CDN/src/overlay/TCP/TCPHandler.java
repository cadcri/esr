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
import streaming.UDP.VideoStream;


public class TCPHandler {
    private NodeState state;
    private int nodeType;
    private Map<String, UDPServer> senders;
    private UDPMiddleMan middleman;
    private VideoStream video;

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
                this.video = new VideoStream("movie.Mjpeg");
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

    public void treatClient(Socket client) throws Exception{
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

        while(true){
            String msg = in.readLine();
            System.out.println("S: " + msg);

            if (isHello(msg)){
                readHello(client, in, msg); break;
            }
            else if (isProbe(msg)){
                readProbe(client, in, msg); break;
            }
            else if (isNewLink(msg)){
                readNewLink(client, in, msg, false); break;
            }
            else if (isRoutes(msg)){
                readRoutes(in, msg); break;
            }
            else if (isMonitoring(msg)){
                readMonitoring(client, in, msg); break;
            }
            else if (isStreamClient(msg)){
                sendStreamRequest(); break;
            }
            else if (isAskStreaming(msg)){
                readAskStreaming(in, msg); break;
            }
            else if (isOpenUDPMiddleMan(msg)){
                readOpenUDPMiddleMan(client, in, msg); break;
            }
            else if (isACKOpenUDPMiddleMan(msg)){
                readACKOpenUDPMiddleMan(client, in, msg); break;
            }
            else if (isNodeClosed(msg)){
                readNodeClosed(client, msg); break;
            }
            else if (isRequestLink(msg)){
                readRequestLink(client, msg); break;
            }
            else if (isPauseStreamClient(msg)){
                sendPauseStreaming(); break;
            }
            else if (isPauseStream(msg)){
                readPauseStream(msg); break;
            }
            else if (isCancelStreamClient(msg)){
                sendCancelStream(); break;
            }
            else if (isCancelStream(msg)){
                readCancelStream(msg); break;
            }
            else if (isEndStreamClient(msg)){
                sendEndStream(msg); break;
            }
            else if (isEndStream(msg)){
                readEndStream(msg); break;
            }
            else if (isNewLinkFixer(msg)){
                readNewLink(client, in, msg, true); break;
            }
            else if (isFixStream(msg)){
                readFixStream(false, in, msg); break;
            }
            else if (isAckFixStream(msg)){
                readFixStream(true, in, msg); break;
            }
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
            else if (isEnd(msg)){
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
        boolean initialMsg = isProbeInitial(msg);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timestamp = getTimestampFromProbe(msg, initialMsg);
        Duration duration = Duration.between(timestamp, now);

        String nodeName = this.state.findAdjNodeFromAddress(client.getInetAddress());
        NodeLink link = new NodeLink(nodeName, nodeName, client.getInetAddress(), Math.abs(duration.toNanos()));
        if(this.state.isLinkModified(nodeName, link)){
            this.state.addLink(nodeName, link);
            sendNewLinkToAdjacents(nodeName);
        }
        if(initialMsg){
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
            else if (isEnd(msg)){
                List<InetAddress> ips = this.state.findAddressesFromAdjNode(viaNode);
                viaInterface = ips.get(0);
                NodeLink newLink = new NodeLink(name, viaNode, viaInterface, hops, cost);

                if((fixer && destination.equals(this.state.getSelf())) || this.state.isLinkModified(name, newLink)){
                    NodeLink oldLink = this.state.getLinkTo(name);
                    this.state.addLink(name, newLink);
                    
                    sendNewLinkToAdjacents(name, viaNode);

                        if ((this.state.isNodeReceivingStream(name))){
                            StreamLink stream = this.state.getStreamFromReceivingNode(name);
                            System.out.println("you here bro?");
                            if ((fixer || stream.getActive() == false) 
                                    && this.state.getSelf().equals(stream.getReceivingNode()) == false
                                    && viaNode.equals(stream.getServer()) == false){
                                        System.out.println("why no send");
                                sendFixStream(viaNode, String.valueOf(stream.getStreamID()), stream.getReceivingNode(), this.state.getSelf(), new String[0]);
                            }
                        }

                    printState();
                }
                else if (destination.equals("") == false){
                    sendNewLinkToAdjacent(destination, name, true);
                }
                else
                    System.out.println("new link refused");

                break;
            }
        }
    }

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
                if(this.state.isLinkModified(dest, newLink)){
                    this.state.addLink(dest, newLink);
                    sendNewLinkToAdjacents(dest, viaNode);
                }
                if (isServer){
                    this.state.addServer(dest);
                    isServer = false;
                }
            }
            else if (isEnd(msg))
                break;
        }

        printState();
    }

    public void readMonitoring(Socket client, BufferedReader in, String msg) throws Exception{
        String[] args = getNodesVisited(msg, "monitoring: ");

        String fromNode = args[args.length - 1];
        sendProbe(fromNode, false);
        sendMonitoringToAdjacents(args);
        String server = args[0];
        this.state.addServer(server);

        while(true){
            msg = in.readLine();
            
            if(isProbe(msg)){
                readProbe(client, in, msg);
            }
            else if (isEnd(msg))
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
            else if (isEnd(msg))
                break;
        }

        if(this.state.getSelf().equals(server)){
            int streamNr = getNodeNr(this.state.getSelf()) * 100 + (this.state.getNrStreams() + 1);
            String[] args = {String.valueOf(streamNr)};
            sendOpenUDPMiddleMan(args, dest);
        }
        else{
            if(this.state.anyActiveStreamWithoutDefects() == true){
                int streamNr = getNodeNr(this.state.getSelf()) * 100 + (this.state.getNrStreams() + 1);
                String[] args = {String.valueOf(streamNr)};
                sendOpenUDPMiddleMan(args, dest);
            }
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
            else if (isEnd(msg))
                break;
        }

        if (!this.state.getSelf().equals(dest)){
            sendOpenUDPMiddleMan(args, dest);
            startUDPMiddleMan();
        }
        else{
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
            else if (isEnd(msg))
                break;
        }

        String[] streamArgs = new String[args.length - 1];
        for(int i = 1; i < args.length; i++)
            streamArgs[i - 1] = args[i];
    
        StreamLink stream = new StreamLink(streamArgs, Integer.parseInt(args[0]));
        this.state.addStream(stream);

        if (this.state.getSelf().equals(args[1])){
            String from = this.state.findAdjNodeFromAddress(client.getInetAddress());

            if(this.state.isServer(this.state.getSelf()))
                startVideoSender(stream, from);
            else
                this.middleman.sendTo(stream);
        }
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

        if (this.state.getAdjState(closedNode) == Vertex.ON || isAdj == false){
            String from = this.state.findAdjNodeFromAddress(client.getInetAddress());
            NodeLink link = this.state.getLinkTo(closedNode);

            if (link != null){
                List<String> lostNodes = this.state.handleClosedNode(closedNode);

                sendNodeClosed(from, closedNode);
                if (lostNodes.size() > 0)
                    askLinkTo(lostNodes);
            }

            printState();
        }
    }

    public void readRequestLink(Socket client, String msg) throws Exception{
        String from = this.state.findAdjNodeFromAddress(client.getInetAddress());
        String to = getSuffixFromPrefix(msg, "? ");

        boolean lostNode = this.state.removeDependentLink(from, to);
        if (lostNode){
            List<String> lostNodes = new ArrayList<>();
            lostNodes.add(to);
            askLinkTo(lostNodes);
        }

        if (this.state.getLinkTo(to) != null){
            sendNewLinkToAdjacent(from, to, true);
        }
    }

    public void readPauseStream(String msg) throws Exception{
        String[] args = getNodesVisited(msg, "pause stream: ");

        if (this.state.getSelf().equals(args[0])){
            StreamLink stream = this.state.getStreamFromArgs(args);
            if (this.state.isServer(this.state.getSelf()))
                this.senders.get(stream.getReceivingNode()).pauseSender();
            else
                this.middleman.pauseSender(stream.getStreamID());
        }
        else{
            StreamLink stream = this.state.getStreamFromArgs(args);
            String nextNode = stream.findNextNode(this.state.getSelf(), true);
            NodeLink link = this.state.getLinkTo(nextNode);

            Thread client = new Thread(new TCPCommunicator(this.state, link.getViaInterface(), TCPCommunicator.PAUSE_STREAMING, args));
            client.run();
        }
    }

    public void readCancelStream(String msg) throws Exception{
        String[] args = getNodesVisited(msg, "cancel stream: ");

        StreamLink stream = this.state.getStreamFromArgs(args);

        if (this.state.getSelf().equals(args[0])){
            if (this.state.isServer(this.state.getSelf())){
                this.senders.get(stream.getReceivingNode()).cancelSender();
                this.senders.remove(stream.getReceivingNode());
            }
            else
                this.middleman.turnOffSender(stream.getStreamID());
        }
        else{
            String nextNode = stream.findNextNode(this.state.getSelf(), true);
            NodeLink link = this.state.getLinkTo(nextNode);

            Thread client = new Thread(new TCPCommunicator(this.state, link.getViaInterface(), TCPCommunicator.CANCEL_STREAM, args));
            client.run();
        }

        this.state.removeStream(stream);
    }

    public void readEndStream(String msg) throws Exception{
        String[] args = getNodesVisited(msg, "end stream: ");
        StreamLink stream = this.state.getStreamFromArgs(args);

        if (this.state.getSelf().equals(stream.getReceivingNode()) == false){
            String nextNode = stream.findNextNode(this.state.getSelf(), false);
            NodeLink link = this.state.getLinkTo(nextNode);

            Thread client = new Thread(new TCPCommunicator(this.state, link.getViaInterface(), TCPCommunicator.END_STREAM, args));
            client.run();
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
            System.out.println(msg);

            if(isPrefixOf(msg, "leading to"))
                rcv = getSuffixFromPrefix(msg, "leading to: ");
            else if(isPrefixOf(msg, "ordered by"))
                orderedBy = getSuffixFromPrefix(msg, "ordered by: ");
            else if (isPrefixOf(msg, "going through")){
                nodesVisited = getNodesVisited(msg, "going through: ");
            }
            else if (isEnd(msg)){
                break;
            }
        }

        StreamLink oldStream = this.state.getStreamFromID(Integer.parseInt(streamID));
        if (oldStream == null || oldStream.getActive() == false){
            if (ack){
                StreamLink stream = this.state.fixStream(streamID, rcv, nodesVisited, orderedBy);
            
                if(stream != null){
                    if(this.state.getSelf().equals(stream.getServer()) == false){
                        sendAckFixStream(streamID, stream, orderedBy);
                    }
                }
                else{
                    stream = new StreamLink(nodesVisited, rcv, Integer.parseInt(streamID), true, orderedBy, this.state.getSelf());
                    this.state.addStream(stream);
                    sendAckFixStream(streamID, stream, orderedBy);
                }
            }
            else{
                if(this.state.getSelf().equals(rcv) == false){
                    startUDPMiddleMan();
                    sendFixStream(rcv, streamID, rcv, orderedBy, nodesVisited);
                }
                else{
                    StreamLink stream = this.state.fixStream(streamID, rcv, nodesVisited, orderedBy);
                    sendAckFixStream(streamID, stream, orderedBy);
                }
            }
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
                if (this.nodeType == TCPHandler.NORMAL_NODE)
                    client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.HELLO));
                else
                    client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.HELLO_SERVER));
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

    public void startVideoSender(StreamLink stream, String dest){
        List<InetAddress> ips = this.state.getSelfIPs();
        UDPServer UDPServer = new UDPServer(ips.get(0), stream, this.state, video);
        UDPServer.start();
        this.senders.put(stream.getReceivingNode(), UDPServer);
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

    public void sendPauseStreaming(){
        StreamLink myStream = this.state.getMyStream();
        String[] args = myStream.convertLinkToArgs();
        String nextNode = myStream.findNextNode(this.state.getSelf(), true);
        NodeLink link = this.state.getLinkTo(nextNode);

        Thread client = new Thread(new TCPCommunicator(this.state, link.getViaInterface(), TCPCommunicator.PAUSE_STREAMING, args));
        client.run();
    }

    public void sendCancelStream() throws Exception{
        StreamLink myStream = this.state.getMyStream();

        if(myStream != null){
            String[] args = myStream.convertLinkToArgs();
            String nextNode = myStream.findNextNode(this.state.getSelf(), true);
            NodeLink link = this.state.getLinkTo(nextNode);

            Thread client = new Thread(new TCPCommunicator(this.state, link.getViaInterface(), TCPCommunicator.CANCEL_STREAM, args));
            client.run();
            this.state.removeStream(myStream);
        }
    }

    public void sendEndStream(String msg) throws Exception{
        String[] args = getNodesVisited(msg, "end stream client: ");
        StreamLink stream = this.state.getStreamFromArgs(args);
        String nextNode = stream.findNextNode(this.state.getSelf(), false);
        NodeLink link = this.state.getLinkTo(nextNode);

        Thread client = new Thread(new TCPCommunicator(this.state, link.getViaInterface(), TCPCommunicator.END_STREAM, args));
        client.run();

        if (this.state.isServer(this.state.getSelf())){
            this.senders.get(stream.getReceivingNode()).cancelSender();
            this.senders.remove(stream.getReceivingNode());
        }
        this.state.removeStream(stream);
    }

    public void sendProbe(String key, boolean initial) throws InterruptedException{
        List<InetAddress> ips = this.state.findAddressesFromAdjNode(key);

        Thread client;
        if (initial)
            client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.PROBE_INITIAL));
        else
            client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.PROBE_REGULAR));

        client.start();
        client.join();
    }

    public void sendNewLinkToAdjacent(String fromNode, String to, boolean fixer) throws InterruptedException{
        NodeLink link = this.state.getLinkTo(fromNode);

        Thread client;
        if (fixer)
            client = new Thread(new TCPCommunicator(this.state, link.getViaInterface(), TCPCommunicator.SEND_NEW_LINK_FIXER, to));
        else
            client = new Thread(new TCPCommunicator(this.state, link.getViaInterface(), TCPCommunicator.SEND_NEW_LINK, to));
        client.start();
        client.join();
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
        NodeLink link = this.state.getLinkTo(dest);
        Thread client = new Thread(new TCPCommunicator(this.state, link.getViaInterface(), TCPCommunicator.ACK_OPEN_UDP_MIDDLEMAN, args));
        client.start();
    }

    public void sendNodeClosed(String from, String node) throws Exception{
        Map<String, Integer> adjsState = this.state.getNodeAdjacentsState();
        Map<String, List<InetAddress>> adjs = this.state.getNodeAdjacents();

        for(Map.Entry<String, Integer> entry: adjsState.entrySet()){
            if (entry.getValue() == Vertex.ON){
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
        if (stream == null || link.getViaNode().equals(stream.getServer()) == false){
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
        NodeLink link = this.state.getLinkTo(nextNode);
        Thread client = new Thread(new TCPCommunicator(this.state, link.getViaInterface(), TCPCommunicator.ACK_FIX_STREAM, args));
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

    public boolean isHello(String msg){
        return isPrefixOf(msg, "hello");
    }

    public boolean isProbe(String msg){
        return isPrefixOf(msg, "probe");
    }

    public boolean isProbeInitial(String msg){
        return isPrefixOf(msg, "probe: initial");
    }

    public boolean isNewLink(String msg){
        return isPrefixOf(msg, "new link");
    }

    public boolean isRoutes(String msg){
        return isPrefixOf(msg, "routes from");
    }

    public boolean isMonitoring(String msg){
        return isPrefixOf(msg, "monitoring");
    }

    public boolean isStreamClient(String msg){
        return isPrefixOf(msg, "i want a stream");
    }

    public boolean isAskStreaming(String msg){
        return isPrefixOf(msg, "want streaming");
    }

    public boolean isNewStreamSignal(String msg){
        return isPrefixOf(msg, "sending stream to");
    }

    public boolean isOpenUDPMiddleMan(String msg){
        return isPrefixOf(msg, "open UDP middleman");
    }

    public boolean isACKOpenUDPMiddleMan(String msg){
        return isPrefixOf(msg, "ack open UDP middleman");
    }

    public boolean isNodeClosed(String msg){
        return isPrefixOf(msg, "node closed");
    }

    public boolean isRequestLink(String msg){
        return isPrefixOf(msg, "?");
    }

    public boolean isPauseStreamClient(String msg){
        return isPrefixOf(msg, "pause stream client");
    }

    public boolean isPauseStream(String msg){
        return isPrefixOf(msg, "pause stream");
    }

    public boolean isCancelStreamClient(String msg){
        return isPrefixOf(msg, "cancel stream client");
    }

    public boolean isCancelStream(String msg){
        return isPrefixOf(msg, "cancel stream");
    }

    public boolean isEndStreamClient(String msg){
        return isPrefixOf(msg, "end stream client");
    }

    public boolean isEndStream(String msg){
        return isPrefixOf(msg, "end stream");
    }

    public boolean isNewLinkFixer(String msg){
        return isPrefixOf(msg, "fixer new link");
    }

    public boolean isFixStream(String msg){
        return isPrefixOf(msg, "fix stream");
    }

    public boolean isAckFixStream(String msg){
        return isPrefixOf(msg, "ack fix stream");
    }

    public boolean isChangeStream(String msg){
        return isPrefixOf(msg, "change stream");
    }

    public boolean isEnd(String msg){
        return isPrefixOf(msg, "end");
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
