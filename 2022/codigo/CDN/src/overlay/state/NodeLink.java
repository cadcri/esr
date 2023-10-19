package overlay.state;

import java.net.InetAddress;

public class NodeLink {
    private String dest;
    private String viaNode;
    private InetAddress viaInterface;
    private int hops;
    private long cost;

    public NodeLink(){}

    public NodeLink(String dest, String viaNode, InetAddress viaInterface, int hops, long cost){
        this.dest = dest;
        this.viaNode = viaNode;
        this.viaInterface = viaInterface;
        this.hops = hops;
        this.cost = cost;
    }

    // creating adjacent
    public NodeLink(String dest, String viaNode, InetAddress viaInterface, long cost){
        this.dest = dest;
        this.viaNode = viaNode;
        this.viaInterface = viaInterface;
        this.hops = 1;
        this.cost = cost;
    }

    public String getDest(){
        return this.dest;
    }

    public String getViaNode(){
        return this.viaNode;
    }

    public InetAddress getViaInterface(){
        return this.viaInterface;
    }

    public int getHops(){
        return this.hops;
    }

    public long getCost(){
        return this.cost;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();

        sb.append("\tDestination: " + this.dest + "\n");
        sb.append("\t\tVia Node: " + this.viaNode + "\n");
        sb.append("\t\tVia Interface: " + this.viaInterface + "\n");
        sb.append("\t\tHops: " + this.hops + "\n");
        sb.append("\t\tCost: " + this.cost + "\n");

        return sb.toString();
    }
}
