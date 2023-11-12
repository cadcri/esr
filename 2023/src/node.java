//package src;

import java.util.ArrayList;

public class node {
    
    public enum type{
        node,
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
    private ArrayList<node> neighbors = new ArrayList<node>();

    public node(){
        this.nodeType=null;
        this.nodeIP="localhost";
        this.nodeState=nodeState.off;
    }
    
    public node(type nodeType, String nodeIP){
        this.nodeType = nodeType;
        this.nodeIP = nodeIP;
        this.nodeState = nodeState.on;
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

    public void setNeighbours(ArrayList<node> neighbors){
        this.neighbors = neighbors;
    }

    public void addNeighbour(node neighbour){
        this.neighbors.add(neighbour);
    }

}
