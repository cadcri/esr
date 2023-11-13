//package src;

import java.util.ArrayList;

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

    private type nodeType;
    private String nodeIP;
    private state nodeState;
    private ArrayList<Node> neighbors = new ArrayList<Node>();

    public Node(){
        this.nodeType=null;
        this.nodeIP="localhost";
        this.nodeState=nodeState.off;
    }
    
    public Node(type nodeType, String nodeIP){
        this.nodeType = nodeType;
        this.nodeIP = nodeIP;
        this.nodeState = nodeState.on;
    }

    public String getNodeIP(){
        return this.nodeIP;
    }

    public type getNodeType(){
        return this.nodeType;
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

    public void setNeighbours(ArrayList<Node> neighbors){
        this.neighbors = neighbors;
    }

    public void addNeighbour(Node neighbour){
        this.neighbors.add(neighbour);
    }

    public String toString(){
        String ret = "Node type: "+this.nodeType+"\nNode IP: "+this.nodeIP+"\nNode state: "+this.nodeState+"\nNeighbours: ";
        for (Node neighbour: this.neighbors){
            ret+=(neighbour.getNodeIP())+" ";
        }
        return ret;
    }

}
