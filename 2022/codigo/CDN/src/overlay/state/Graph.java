package overlay.state;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph {
    private Map<String, Vertex> nodes;

    public Graph(Map<String, Vertex> nodes){
        this.nodes = nodes;
    }

    public List<InetAddress> getNodeIPList(String node){
        if (this.nodes.containsKey(node)){
            Vertex v = this.nodes.get(node);
            return v.getIPList();
        }
        else
            return null;
    }

    public String getNameFromIP(InetAddress ip){
        String ipString = ip.getHostAddress();
        String nodeName = "";

        for (Map.Entry<String, Vertex> entry: this.nodes.entrySet()){
            List<InetAddress> ipList = entry.getValue().getIPList();
            for(InetAddress address : ipList){
                if(ipString.equals(address.getHostAddress())){
                    nodeName = entry.getKey();
                    break;
                }
            }
            if (!nodeName.equals(""))
                break;
        }

        return nodeName;
    }

    public int getNodeState(String key){
        if (this.nodes.containsKey(key)){
            Vertex v = this.nodes.get(key);
            return v.getState();
        }
        return Vertex.OFF;
    }

    public void setNodeState(String key, int state){
        if (this.nodes.containsKey(key)){
            Vertex v = this.nodes.get(key);
            v.setState(state);
        }
    }

    public void setAdjacentsInNode(String nodeName, Map<String, List<InetAddress>> adjs){
        if (this.nodes.containsKey(nodeName)){
            Vertex v = this.nodes.get(nodeName);
            v.setAdjacents(adjs);
            this.nodes.put(nodeName, v);
        }
    }

    public Map<String, List<InetAddress>> getNodeAdjacents(String key){
        Vertex node = this.nodes.get(key);
        return node.getAdjacents();
    }

    public Map<String, Integer> getNodeAdjacentsState(String key){
        Map<String, Integer> states = new HashMap<>();
        Map<String, List<InetAddress>> adjs = getNodeAdjacents(key);

        for(String nodeName: adjs.keySet()){
            Vertex v = this.nodes.get(nodeName);
            states.put(nodeName, v.getState());
        }

        return states;
    }

    public NodeState graphToNodeState(String self){
        List<InetAddress> ips = getNodeIPList(self);
        Map<String, List<InetAddress>> adjs = getNodeAdjacents(self);
        Map<String, Integer> adjsState = getNodeAdjacentsState(self);
        Vertex v = new Vertex(self, ips, adjs, adjsState, Vertex.ON);
        return new NodeState(v, null);
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Vertex> node: this.nodes.entrySet()){
            if (node.getValue() == null)
                sb.append("Node: " + node.getKey() + "\n");
            else
                sb.append(node.getValue().toString() + "\n");
        }

        return sb.toString();
    }
}
