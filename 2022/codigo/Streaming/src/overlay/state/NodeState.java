package overlay.state;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;


// representa o estado de um nodo
public class NodeState {
    private Vertex node;
    private InetAddress bstrapperIP;
    // tabela de distâncias de vetores
    private DistancesTable table;
    // lista de streams que passam ou passaram pelo nodo
    private List<StreamLink> streams;
    // lista de servidores na topologia
    private List<String> servers;
    // lista de nodos que já se fecharam (para evitar floods infinitos)
    private List<String> closedNodes;
    private final ReentrantLock lock;
    
    public NodeState(Vertex node, InetAddress bstrapperIP){
        this.node = node;
        this.bstrapperIP = bstrapperIP;
        this.table = new DistancesTable();
        this.streams = new ArrayList<>();
        this.servers = new ArrayList<>();
        this.closedNodes = new ArrayList<>();
        this.lock = new ReentrantLock();
    }

    public InetAddress getBstrapperIP(){
        return this.bstrapperIP;
    }

    public DistancesTable getTable(){
        return this.table;
    }

    public List<String> getServers(){
        return this.servers;
    }

    public List<StreamLink> getStreamLinks(){
        return this.streams;
    }

    public int getNrStreams(){
        return this.streams.size();
    }

    public String getSelf(){
        return this.node.getName();
    }

    public List<InetAddress> getSelfIPs(){
        return this.node.getIPList();
    }

    public void addLink(String dest, NodeLink newLink){
        this.lock.lock();
        try{
            this.table.addLink(dest, newLink);
        }
        finally{
            this.lock.unlock();
        }
    }

    public void addLink(String dest, String viaNode, InetAddress viaInterface, long cost){
        this.lock.lock();
        try{
            this.table.addLink(dest, viaNode, viaInterface, cost);
        }
        finally{
            this.lock.unlock();
        }
    }

    public void addStream(StreamLink stream){
        this.lock.lock();
        try{
            if (!this.streams.contains(stream))
                this.streams.add(stream);
        }
        finally{
            this.lock.unlock();
        }
    }

    public void removeStream(StreamLink stream){
        this.lock.lock();
        try{
            for(int i = 0; i < this.streams.size(); i++){
                StreamLink tmp = this.streams.get(i);
                if (tmp != null){
                    if (tmp.getReceivingNode().equals(stream.getReceivingNode()) && tmp.getServer().equals(stream.getServer())){
                        this.streams.remove(i);
                        this.streams.add(i, null);
                    }
                }
            }
        }
        finally{
            this.lock.unlock();
        }
    }

    public void addServer(String server){
        this.lock.lock();
        try{
            if(!server.equals("")){
                boolean isPresent = isServer(server);

                if (isPresent == false)
                    this.servers.add(server);
            }
        }
        finally{
            this.lock.unlock();
        }
    }

    public void removeServer(String server){
        this.lock.lock();
        try{
            if (!this.servers.contains(server))
                this.servers.remove(server);
        }
        finally{
            this.lock.unlock();
        }
    }

    public void addCloseNode(String node){
        this.lock.lock();
        try{
            if(!node.equals("")){
                boolean isPresent = isCloseNode(node);

                if (isPresent == false)
                    this.closedNodes.add(node);
            }
        }
        finally{
            this.lock.unlock();
        }
    }

    public void removeClosedNode(String node){
        this.lock.lock();
        try{
            if (!this.servers.contains(node))
                this.servers.remove(node);
        }
        finally{
            this.lock.unlock();
        }
    }

    public boolean isServer(String server){
        boolean isPresent = false;
            
        for(String s: this.servers){
            if(s.equals(server)){
                isPresent = true;
                break;
            }
        }

        return isPresent;
    }

    public boolean isCloseNode(String node){
        boolean isPresent = false;
            
        for(String s: this.closedNodes){
            if(s.equals(node)){
                isPresent = true;
                break;
            }
        }

        return isPresent;
    }

    public void setAdjState(String adj, int state){
        this.lock.lock();
        try{
            this.node.setAdjState(adj, state);
        }
        finally{
            this.lock.unlock();
        }
    }

    public Map<String, List<InetAddress>> getNodeAdjacents(){
        return this.node.getAdjacents();
    }

    public Map<String, Integer> getNodeAdjacentsState(){
        return this.node.getAdjacentsState();
    }

    public int getAdjState(String key){
        return this.node.getAdjState(key);
    }

    public List<InetAddress> findAddressesFromAdjNode(String key){
        return this.node.findAddressesFromNode(key);
    }

    public String findAdjNodeFromAddress(InetAddress ip){
        return this.node.findNodeFromAddress(ip);
    }

    public NodeLink getLinkTo(String key){
        return this.table.getLinkTo(key);
    }

    public NodeLink getClosestServer(){
        return this.table.getClosestFromList(this.servers);
    }

    public boolean isLinkModified(String key, NodeLink newLink){
        return this.table.isLinkModified(getSelf(), key, newLink);
    }

    // devolve uma stream que tem este nodo como destino
    public StreamLink getMyStream(){
        StreamLink res = null;

        for(StreamLink stream: this.streams){
            if (stream != null){
                if (stream.getReceivingNode().equals(getSelf())){
                    res = stream;
                    break;
                }
            }
        }

        return res;
    }

    public StreamLink getStreamFromID(int ID){
        StreamLink res = null;

        for(StreamLink stream: this.streams){
            if (stream != null){
                if (stream.getStreamID() == ID){
                    res = stream;
                    break;
                }
            }
        }

        return res;
    }

    // devolve uma stream a partir de um array com o seu percurso
    public StreamLink getStreamFromArgs(String[] args){
        StreamLink res = null;

        for(StreamLink stream: this.streams){
            if (stream != null){
                if (args[0].equals(stream.getServer()) && args[args.length - 1].equals(stream.getReceivingNode())){
                    res = stream;
                    break;
                }
            }
        }

        return res;
    }

    public boolean anyActiveStream(){
        boolean res = false;

        for (StreamLink stream: this.streams){
            if (stream != null){
                res = true;
                break;
            }
        }

        return res;
    }

    public boolean anyActiveStreamWithoutDefects(){
        boolean res = false;

        for (StreamLink stream: this.streams){
            if (stream != null && stream.getActive() == true){
                res = true;
                break;
            }
        }

        return res;
    }

    public boolean isNodeInStream(String node){
        boolean res = false;

        for (StreamLink stream: this.streams){
            if (stream != null)
                res = stream.isNodeInStream(node);
            if (res == true)
                break;
        }

        return res;
    }

    public List<StreamLink> getStreamsWithNode(String node){
        List<StreamLink> res = new ArrayList<>();

        for (StreamLink stream: this.streams){
            if (stream != null){
                if (stream.isNodeInStream(node)){
                    res.add(stream);
                }
            }
        }

        return res;
    }

    public boolean isNodeReceivingStream(String node){
        boolean res = false;

        for (StreamLink stream: this.streams){
            if (stream != null){
                if(node.equals(stream.getReceivingNode())){
                    res = true;
                    break;
                }
            }
        }

        return res;
    }

    public StreamLink getStreamFromReceivingNode(String node){
        StreamLink res = null;

        for (StreamLink stream: this.streams){
            if (stream != null){
                if(node.equals(stream.getReceivingNode())){
                    res = stream;
                    break;
                }
            }
        }

        return res;
    }

    public List<String> handleClosedNode(String key){
        setAdjState(key, Vertex.OFF);
        if (isServer(key))
            removeServer(key);

        if (isNodeInStream(key)){
            for(StreamLink stream: this.streams){
                if (stream != null){
                    if (stream.isNodeInStream(key)){
                        stream.setActive(false);
                        stream.setWithChange(true);
                        stream.setChangeAt(key);
                    }
                }
            }
        }

        return this.table.handleClosedNode(key);
    }

    // remove ligação dependente de nodo que se fechou
    public boolean removeDependentLink(String viaNode, String to){
        NodeLink link = this.table.getLinkTo(to);
        if (link == null)
            return false;
        else if (link.getViaNode().equals(viaNode)){
            this.table.removeLink(to);
            return true;
        }
        else
            return false;
    }

    public int getNrActiveAdjs(){
        int res = 0;

        for(Map.Entry<String, Integer> entry: this.node.getAdjacentsState().entrySet()){
            if (entry.getValue() == Vertex.ON)
                res++;
        }

        return res;
    }

    // repara uma stream que estava inativa
    public StreamLink fixStream(String streamIDs, String rcvNode, String[] nodesVisited, String orderedBy){
        StreamLink res = null;
        int streamID = Integer.parseInt(streamIDs);

        for(StreamLink stream: this.streams){
            if (stream != null){
                if (stream.getStreamID() == streamID){
                    List<String> newPath = new ArrayList<>();
                    stream.setActive(true);

                    StreamLink oldStream = getStreamFromID(streamID);
                    for(String node: oldStream.getStream()){
                        if (node.equals(orderedBy))
                            break;
                        newPath.add(node);
                    }

                    for (int i = 0; i < nodesVisited.length; i++){
                        if (newPath.contains(nodesVisited[i]) == false)
                            newPath.add(nodesVisited[i]);
                    }
                    newPath.add(rcvNode);

                    stream.setStream(newPath);
                    stream.setChangeAt(orderedBy);
                    stream.setChangeAfterMe(getSelf());
                    res = stream;
                }
            }
        }

        return res;
    }

    // muda o estado de uma stream, quando esta muda de percurso
    public StreamLink changeStream(String streamIDs, String rcvNode, String[] nodesVisited, String orderedBy){
        StreamLink res = null;
        int streamID = Integer.parseInt(streamIDs);

        for(StreamLink stream: this.streams){
            if (stream != null){
                if (stream.getStreamID() == streamID){
                    List<String> newPath = new ArrayList<>();
                    stream.setActive(true);

                    StreamLink oldStream = getStreamFromID(streamID);
                    for(String node: oldStream.getStream()){
                        if (node.equals(orderedBy))
                            break;
                        newPath.add(node);
                    }
                    
                    for (int i = 0; i < nodesVisited.length; i++){
                        if (newPath.contains(nodesVisited[i]) == false)
                            newPath.add(nodesVisited[i]);
                    }
                    newPath.add(rcvNode);


                    stream.setWithChange(true);
                    stream.setChangeAt(orderedBy);
                    stream.setStream(newPath);
                    stream.setChangeAfterMe(getSelf());
                    res = stream;
                }
            }
        }

        return res;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        
        sb.append("Vertex:\n");
        sb.append(this.node.toString());
        sb.append("\n");
        sb.append("Table:\n");
        sb.append(this.table.toString());
        sb.append("\n");
        sb.append("Servers: ");
        for(String server: this.servers)
            sb.append(server + " ");
        sb.append("\n\n");
        sb.append("Streams:\n");
        for(int i = 0; i < this.streams.size(); i++){
            sb.append("\tSTREAM " + (i+1) + ":\n");
            StreamLink stream = this.streams.get(i);
            if (stream != null)
                sb.append(stream.toString() + "\n");
            else
                sb.append("\t\tit's done\n");
        }

        return sb.toString();
    }
}
