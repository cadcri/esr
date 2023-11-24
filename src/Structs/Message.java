package Structs;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;


public class Message implements Serializable{

    public enum Type{
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
    private ArrayList<String> path;
    private LocalDateTime timeStampInit;
    private String src;

    public Message(Type tipo) {
        this.tipo = tipo;
        switch(this.tipo){
            case PROBE:
                //This could lead to problems if the message is not sent immediately and 
                //if the clocks are not synchronized
                this.timeStampInit = LocalDateTime.now();
                break;

            case PROBE_ACK:
                //This could lead to problems if the message is not sent immediately and
                //if the clocks are not synchronized
                //this.timeStampEnd = LocalDateTime.now();
                break;

        }
    }

    public Message(Type tipo, Packet packet){
        this.tipo = tipo;
        switch(this.tipo){
            case PROBE_ACK:
                //this.packet = packet;
                break;
        }
    }

    public Message(Type tipo, ArrayList<Node> vizinhos){
        this.tipo = tipo;
        switch(this.tipo){
            case JOIN_ACK:
                //this.vizinhos=vizinhos;
                break;

        }
    }

    public Type getTipo(){
        return this.tipo;
    }

    public ArrayList<String> getPath(){
        return this.path;
    }

    public LocalDateTime getTimeStampInit(){
        return this.timeStampInit;
    }


    public void addNodeToPath(String node){
        this.path.add(node);
    }

}

