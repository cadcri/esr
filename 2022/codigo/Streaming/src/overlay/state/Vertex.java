package overlay.state;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Vertex {
    private String name;
    // lista de interfaces do próprio nodo
    private List<InetAddress> ipList;
    // interfaces dos adjacentes do nodo
    private Map<String, List<InetAddress>> adjacents;
    // estado dos adjacented do nodo
    private Map<String, Integer> adjsState;
    // estado do nodo (ON / OFF)
    private int state;

    public static final int ON = 1;
    public static final int OFF = 2;

    // Graph Vertex
    public Vertex(String name, List<InetAddress> ips, int state){
        this.name = name;
        this.ipList = ips;
        this.adjacents = new HashMap<>();
        this.adjsState = null;
        this.state = state;
    }

    // NodeState vertex
    public Vertex(String name, List<InetAddress> ips, Map<String, List<InetAddress>> adjacents, Map<String, Integer> adjsState, int state){
        this.name = name;
        this.ipList = ips;
        this.adjacents = adjacents;
        this.adjsState = adjsState;
        this.state = state;
    }

    public String getName(){
        return this.name;
    }

    public List<InetAddress> getIPList(){
        return this.ipList;
    }

    public Map<String, List<InetAddress>> getAdjacents(){
        return this.adjacents;
    }

    public Map<String, Integer> getAdjacentsState(){
        return this.adjsState;
    }

    public int getState(){
        return this.state;
    }

    public void setAdjacents(Map<String, List<InetAddress>> adjacents){
        this.adjacents = adjacents;
    }

    public void setState(int state){
        this.state = state;
    }

    public int getAdjState(String key){
        if (this.adjsState.containsKey(key))
            return this.adjsState.get(key);
        else
            return 0;
    }

    public void setAdjState(String node, int state){
        if (this.adjsState.containsKey(node))
            this.adjsState.put(node, state);
    }

    public List<InetAddress> findAddressesFromNode(String key){
        if (this.adjacents.containsKey(key))
            return this.adjacents.get(key);
        else
            return null;
    }

    public String findNodeFromAddress(InetAddress ip){
        String name = "";
        boolean found = false;

        for(Map.Entry<String, List<InetAddress>> entry: this.adjacents.entrySet()){
            for(InetAddress address: entry.getValue()){
                if (ip.equals(address)){
                    name = entry.getKey();
                    break;
                }
            }
            if (found)
                break;
        }

        return name;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        
        sb.append("Node: " + this.name + "\n");
        if (this.state == OFF)
            sb.append("\tState: OFF\n");
        else
            sb.append("\tState: ON\n");

        if (ipList != null){
            for (InetAddress ip: this.ipList)
                sb.append("\tAvailable at: " + ip.getHostAddress() + "\n");
        }

        if (adjacents != null){
            sb.append("\tAdjacents: ");
            for (Map.Entry<String, List<InetAddress>> entry: this.adjacents.entrySet())
                sb.append(entry.getKey() + " ");
            sb.append("\n");
        }

        if (adjsState != null){
            sb.append("\tAdjacents' State:\n");
            for (Map.Entry<String, Integer> entry: this.adjsState.entrySet()){
                if (entry.getValue() == OFF)
                    sb.append("\t\t" + entry.getKey() + ": OFF\n");
                else
                    sb.append("\t\t" + entry.getKey() + ": ON\n"); 
            }
        }

        return sb.toString();
    }

    public Vertex cloneGraphVertex(){
        List<InetAddress> ips = new ArrayList<>();
        for(InetAddress ip: this.ipList)
            ips.add(ip);

        return new Vertex(this.name, ips, this.state);
    }
}
