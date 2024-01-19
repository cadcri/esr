package TCP;

import java.util.ArrayList;

public class RouteInfo {
    private String dest;
    private Float latency;
    private ArrayList<String> path;

    public RouteInfo(String dest, Float latency, ArrayList<String> path) {
        this.dest = dest;
        this.latency = latency;
        this.path = path;
    }

    public String getDest() {
        return this.dest;
    }

    public Float getLatency() {
        return this.latency;
    }

    public ArrayList<String> getPath() {
        return this.path;
    }

    public void setLatency(Float latency) {
        this.latency = latency;
    }

    public String getDestination() {
        return this.dest;
    }
}