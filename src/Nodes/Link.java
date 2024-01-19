package Nodes;

public class Link{

    public enum Status{
        on,
        off
    }

    private String node1;
    private String node2;
    private Status status;
    private float latency;

    public Link(String node1, String node2){
        this.node1 = node1;
        this.node2 = node2;
        this.status = Status.off;
        this.latency = 0;
    }

    public String[] getNodes(){
        String[] nodes = {this.node1, this.node2};
        return nodes;
    }

    public Status getLinkStatus(){
        return this.status;
    }

    public float getLatency(){
        return this.latency;
    }

    public void setLinkStatus(Status status){
        this.status = status;
    }

    public void setLatency(float latency){
        this.latency = latency;
    }
}