import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;


public class Message implements Serializable{

    private enum Type{
        PROBE,
        PROBE_ACK,
        ACK,
        JOIN,
        JOIN_ACK,
        LEAVE,
        LEAVE_ACK,
        STREAM
    }

    private Type tipo;
    private ArrayList<Node> vizinhos;
    private LocalDateTime timeStampInit;
    private LocalDateTime timeStampEnd;
    private Packet packet;
    private String ip;

    public Message(Type tipo, String ip) {
        this.tipo = tipo;
        this.ip=ip;
        switch(this.tipo){
            case PROBE:
                //This could lead to problems if the message is not sent immediately and 
                //if the clocks are not synchronized
                this.timeStampInit = LocalDateTime.now();
                break;

            case PROBE_ACK:
                //This could lead to problems if the message is not sent immediately and
                //if the clocks are not synchronized
                this.timeStampEnd = LocalDateTime.now();
                break;

            case LEAVE:
                break;
            
            case LEAVE_ACK:
                break;

            case JOIN:
                break;
        }
    }

    public Message(Type tipo, Packet packet){
        this.tipo = tipo;
        switch(this.tipo){
            case PROBE_ACK:
                this.packet = packet;
                break;
            
            case STREAM:
                this.packet = packet;
                break;
        }
    }

    public Message(Type tipo, ArrayList<Node> vizinhos){
        this.tipo = tipo;
        switch(this.tipo){
            case JOIN_ACK:
                this.vizinhos=vizinhos;
                break;

        }
    }

    public Type getTipo(){
        return this.tipo;
    }

    public ArrayList<Node> getVizinhos(){
        return this.vizinhos;
    }

    public LocalDateTime getTimeStampInit(){
        return this.timeStampInit;
    }

    public LocalDateTime getTimeStampEnd(){
        return this.timeStampEnd;
    }

    public Packet getPacket(){
        return this.packet;
    }

    public void setTimeStampEnd(){
        this.timeStampEnd = LocalDateTime.now();
    }
}

