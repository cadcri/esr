package Structs;
import java.util.HashMap;
import java.util.ArrayList;

public class Node {
    
    public enum type{
        node,
        rp, 
        client
    }

    public enum state{
        on, 
        off
    }

    private String name;
    private type nodeType;
    private state nodeState;
    private ArrayList<String> gateways;

    public Node(){
        this.nodeType=null;
        this.name="";
        this.nodeState=nodeState.off;
        this.gateways=new ArrayList<String>();
    }
    
    public Node(type nodeType, String nodeIP){
        this.nodeType = nodeType;
        this.nodeState = nodeState.off;
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

    public ArrayList<String> getGateways(){
        return this.gateways;
    }

    public void setNodeName(String name){
        this.name=name;
    }

    public void setNodeType(type nodeType){
        this.nodeType=nodeType;
    }

    public void setNodeType(String nodeType){
        switch(nodeType){
            case("node"):
                this.nodeType = type.node;
                break;
            case("rp"):
                this.nodeType=type.rp;
                break;
            case("client"):
                this.nodeType=type.client;
                break;
        }
    }

    public void setGateways(ArrayList<String> gateways){
        this.gateways=gateways;
    }

    public void setNodeState(state nodeState){
        this.nodeState = nodeState;
    }
    public void addGateway(String gateway){
        this.gateways.add(gateway);
    }

    public void removeGateway(String gateway){
        this.gateways.remove(gateway);
    }

    public String toString(){
        String ret = "Node type: "+this.nodeType+"\nNode name: "+this.name+"\nNode state: "+this.nodeState+"\nGateways: ";
        for (String gateway : this.gateways){
            ret+=gateway+" ";
        }
        return ret;
    }

    public Node clone(){
        Node clone = new Node();
        clone.setNodeName(this.name);
        clone.setNodeType(this.nodeType);
        clone.setNodeState(this.nodeState);
        clone.setGateways(this.gateways);
        return clone;
    }

}
