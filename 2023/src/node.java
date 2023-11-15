//package src;

import java.util.HashMap;

public class Node {
    
    public enum type{
        Node,
        rp, 
        client
    }

    public enum state{
        on, 
        off
    }

    private String name;
    private type nodeType;
    private String nodeIP;
    private state nodeState;
    //private ArrayList<Node> neighbors = new ArrayList<Node>();
    //Node name, Node
    private HashMap<String, Node> neighbors = new HashMap<String, Node>();


    public Node(){
        this.nodeType=null;
        this.nodeIP="localhost";
        this.name="";
        this.nodeState=nodeState.off;
    }
    
    public Node(type nodeType, String nodeIP){
        this.nodeType = nodeType;
        this.nodeIP = nodeIP;
        this.nodeState = nodeState.off;
    }

    public String getNodeIP(){
        return this.nodeIP;
    }

    public String getNodeName(){
        return this.name;
    }

    public type getNodeType(){
        return this.nodeType;
    }

    public state getState(){
        return this.nodeState;
    }

    public HashMap<String, Node> getNeighbors(){
        return this.neighbors;
    }

    public void setNodeName(String name){
        this.name=name;
    }

    public void setNodeType(type nodeType){
        this.nodeType = nodeType;
    }

    public void setNodeIP(String nodeIP){
        this.nodeIP = nodeIP;
    }

    public void setNodeState(state nodeState){
        this.nodeState = nodeState;
    }

    public void setNeighbours(HashMap<String,Node> neighbors){
        this.neighbors = neighbors;
    }

    public void addNeighbour(Node neighbour){
        
        this.neighbors.putIfAbsent(neighbour.getNodeName(), neighbour);
    }

    public String toString(){
        String ret = "Node type: "+this.nodeType+"\nNode IP: "+this.nodeIP+"\nNode state: "+this.nodeState+"\nNeighbours: ";
        for (Node neighbour: this.neighbors.values()){
            ret+=(neighbour.getNodeIP())+" ";
        }
        return ret;
    }

    public Node clone(){
        Node clone = new Node();
        clone.setNodeIP(this.nodeIP);
        clone.setNodeName(this.name);
        clone.setNodeType(this.nodeType);
        clone.setNodeState(this.nodeState);
        clone.setNeighbours(this.neighbors);
        return clone;
    }

}
