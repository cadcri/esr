package TCP;

import java.io.*;
import java.time.*;
import java.util.*;

public class TCPPacket implements Serializable {

    public enum Type {
        PROBE,
        PROBE_ACK,
        ACK,
        JOIN,
        JOIN_ACK,
        LEAVE,
        LEAVE_ACK,
        STREAM,
        STREAM_ACK,
        REQUEST_STREAM,
        REQUEST_STREAM_ACK,
        STREAM_END,
        STREAM_END_ACK
    }

    private String src=null;
    private LocalDateTime timeStampInit;
    private LocalDateTime timeStampEnd;
    private Type tipo;
    private ArrayList<String> outgoingPath=new ArrayList<String>();
    private ArrayList<String> incomingPath=new ArrayList<String>();
    private String streamName;

    public TCPPacket(Type tipo) {
        this.tipo = tipo;
        switch (this.tipo) {
            case JOIN:
            case JOIN_ACK:
                // This could lead to problems if the message is not sent immediately and
                // if the clocks are not synchronized
                this.timeStampInit = LocalDateTime.now();
                break;



        }
    }

    public Type getType() {
        return this.tipo;
    }

    public void setType(Type type){
        this.tipo=type;
    }

    public String getSrc(){
        return this.src;
    }

    public void setSrc(String src){
        this.src = src;
    }

    public void setStreamName(String streamName){
        this.streamName = streamName;
    }

    public ArrayList<String> getOutgoingPath(){
        return this.outgoingPath;
    }

    public String getStreamName(){
        return this.streamName;
    }

    public ArrayList<String> getIncomingPath(){
        return this.incomingPath;
    }

    public LocalDateTime getTimeStampInit(){
        return this.timeStampInit;
    }

    public LocalDateTime getTimeStampEnd(){
        return this.timeStampEnd;
    }
    
    public float getLatency(){
        Duration duration = Duration.between(this.timeStampInit, this.timeStampEnd);
        return duration.toMillis();
    }

    public void addToIncomingPath(String gateway){
        this.incomingPath.add(gateway);
    }

    public void addToOutgoingPath(String gateway){
        this.outgoingPath.add(gateway);
    }

    private void setOutgoingPath(ArrayList<String> path){
        this.outgoingPath = path;
    }

    private void setIncomingPath(ArrayList<String> path){
        this.incomingPath = path;
    }


    public void setTimeStampInit(LocalDateTime timeStampInit){
        this.timeStampInit = timeStampInit;
    }

    public void setTimeStampEnd(LocalDateTime timeStampEnd){
        this.timeStampEnd = timeStampEnd;
    }

    public void setTimeStampInit(){
        this.timeStampInit = LocalDateTime.now();
    }

    public void setTimeStampEnd(){
        this.timeStampEnd = LocalDateTime.now();
    }

    public float calculateLatency(){
        Duration duration = Duration.between(this.timeStampInit, this.timeStampEnd);
        return duration.toMillis();
    }

    public TCPPacket clone(){
        TCPPacket clone = new TCPPacket(this.tipo);
        clone.setSrc(this.src);
        clone.setTimeStampInit(this.timeStampInit);
        clone.setTimeStampEnd(this.timeStampEnd);
        clone.setIncomingPath(new ArrayList<String>(this.incomingPath));
        clone.setOutgoingPath(new ArrayList<String>(this.outgoingPath));
        return clone;
    }

    public String serializa(){
        String serialized = "";
        serialized += this.src + "$$";
        if(this.timeStampInit!=null){
            serialized += this.timeStampInit.toString() + "$$";
        }
        else{
            serialized += "$$";
        }
        if(this.timeStampEnd!=null){
            serialized += this.timeStampEnd.toString() + "$$";
        }
        else{
            serialized += "$$";
        }
        serialized += this.tipo.toString() + "$$";
        serialized += this.outgoingPath.size() + "$$";
        for(String gateway : this.outgoingPath){
            serialized += gateway + ";;";
        }
        serialized+="$$";
        serialized += this.incomingPath.size() + "$$";
        for(String gateway : this.incomingPath){
            serialized += gateway + ";;";
        }
        serialized += "$$";
        serialized += this.streamName;
        serialized += "$$";
        return serialized;
    }


    public static TCPPacket deserialize(String serializedPacket){
        String [] fields = serializedPacket.split("\\$\\$");
        TCPPacket packet = new TCPPacket(Type.valueOf(fields[3]));
        packet.setSrc(fields[0]);
        if(fields[1].equals("")){
            packet.setTimeStampInit(null);
        }
        else{
            packet.setTimeStampInit(LocalDateTime.parse(fields[1]));
        }
        if(fields[2].equals("")){
            packet.setTimeStampEnd(null);
        }
        else{
            packet.setTimeStampEnd(LocalDateTime.parse(fields[2]));
        }
        int outgoingPathSize = Integer.parseInt(fields[4]);
        for(int i = 0; i < outgoingPathSize; i++){
            packet.addToOutgoingPath(fields[5].split(";;")[i]);
        }
        int incomingPathSize = Integer.parseInt(fields[6]);
        for(int i = 0; i < incomingPathSize; i++){
            packet.addToIncomingPath(fields[7].split(";;")[i]);
        }
        packet.setStreamName(fields[8]);
        return packet;
    }

}