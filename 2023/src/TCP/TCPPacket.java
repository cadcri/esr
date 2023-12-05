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
        STREAM_END_ACK,
        LIST_STREAMS,
        END_STREAM, LIST_STREAMS_ACK
    }

    private String src = null;
    private String dest = null;
    private LocalDateTime timeStampInit;
    private LocalDateTime timeStampEnd;
    private Type tipo;
    private ArrayList<String> outgoingPath = new ArrayList<String>();
    private ArrayList<String> incomingPath = new ArrayList<String>();
    private ArrayList<String> streams = new ArrayList<String>();
    private int streamId=-1;
    private int streamPort;
    private String streamInteface;
    private String pathToFile;

    public TCPPacket(Type tipo) {
        this.tipo = tipo;
        switch (this.tipo) {
            case JOIN:
            case JOIN_ACK:
            case PROBE:
                // This could lead to problems if the message is not sent immediately and
                // if the clocks are not synchronized
                this.timeStampInit = LocalDateTime.now();
                break;
        }
    }

    public void setType(Type type) {
        this.tipo = type;
    }

    public String getDest(){
        return this.dest;
    }

    public ArrayList<String> getStreams(){
        return this.streams;
    }

    public void addStream(String stream){
        this.streams.add(stream);
    }

    public void setStreams(ArrayList<String> streams){
        this.streams = streams;
    }

    public void setDest(String dest){
        this.dest=dest;
    }

    public void setPathToFile(String pathToFile) {
        this.pathToFile = pathToFile;
    }

    public String getPathToFile() {
        return this.pathToFile;
    }

    public void setStreamId(Integer streamId) {
        this.streamId = streamId;
    }

    public void setStreamPort(int streamPort) {
        this.streamPort = streamPort;
    }

    public int getStreamPort() {
        return this.streamPort;
    }

    public String getSrc() {
        return this.src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public void setStreamInterface(String streamInterface) {
        this.streamInteface = streamInterface;
    }

    public ArrayList<String> getOutgoingPath() {
        return this.outgoingPath;
    }

    public Integer getStreamId() {
        return this.streamId;
    }

    public ArrayList<String> getIncomingPath() {
        return this.incomingPath;
    }

    public LocalDateTime getTimeStampInit() {
        return this.timeStampInit;
    }

    public LocalDateTime getTimeStampEnd() {
        return this.timeStampEnd;
    }

    public float getLatency() {
        Duration duration = Duration.between(LocalDateTime.now(), this.timeStampInit);
        return duration.toMillis();
    }

    public Type getType() {
        return this.tipo;
    }

    public void addToIncomingPath(String gateway) {
        this.incomingPath.add(gateway);
    }

    public void addToOutgoingPath(String gateway) {
        this.outgoingPath.add(gateway);
    }

    public void setOutgoingPath(ArrayList<String> path) {
        this.outgoingPath = path;
    }

    public void setIncomingPath(ArrayList<String> path) {
        this.incomingPath = path;
    }

    public void setTimeStampInit(LocalDateTime timeStampInit) {
        this.timeStampInit = timeStampInit;
    }

    public void setTimeStampEnd(LocalDateTime timeStampEnd) {
        this.timeStampEnd = timeStampEnd;
    }

    public void setTimeStampInit() {
        this.timeStampInit = LocalDateTime.now();
    }

    public void setTimeStampEnd() {
        this.timeStampEnd = LocalDateTime.now();
    }

    public float calculateLatency() {
        Duration duration = Duration.between(this.timeStampInit, this.timeStampEnd);
        return duration.toMillis();
    }

    public TCPPacket clone() {
        TCPPacket clone = new TCPPacket(this.tipo);
        clone.setSrc(this.src);
        clone.setTimeStampInit(this.timeStampInit);
        clone.setTimeStampEnd(this.timeStampEnd);
        clone.setIncomingPath(new ArrayList<String>(this.incomingPath));
        clone.setOutgoingPath(new ArrayList<String>(this.outgoingPath));
        clone.setStreamId(this.streamId);
        clone.setStreamPort(this.streamPort);
        clone.setStreamInterface(this.streamInteface);
        clone.setPathToFile(this.pathToFile);
        clone.setDest(this.dest);
        clone.setStreams(new ArrayList<String>(this.streams));
        return clone;
    }

    public String serializa() {
        StringBuilder serialized = new StringBuilder();
        serialized.append(this.src).append("$$");
        if (this.timeStampInit != null) {
            serialized.append(this.timeStampInit.toString()).append("$$");
        } else {
            serialized.append("$$");
        }
        if (this.timeStampEnd != null) {
            serialized.append(this.timeStampEnd.toString()).append("$$");
        } else {
            serialized.append("$$");
        }
        serialized.append(this.tipo.toString()).append("$$");
        serialized.append(this.outgoingPath.size()).append("$$");
        for (String gateway : this.outgoingPath) {
            serialized.append(gateway).append(";;");
        }
        serialized.append("$$");
        serialized.append(this.incomingPath.size()).append("$$");
        for (String gateway : this.incomingPath) {
            serialized.append(gateway).append(";;");
        }
        serialized.append("$$");
        serialized.append(this.streamId).append("$$");
        serialized.append(this.streamPort).append("$$");
        serialized.append(this.streamInteface).append("$$");
        serialized.append(this.pathToFile).append("$$");
        serialized.append(this.dest).append("$$");
        serialized.append(this.streams.size()).append("$$");
        for (String stream : this.streams) {
            serialized.append(stream).append(";;");
        }
        serialized.append("$$");
        return serialized.toString();
    }

    public static TCPPacket deserialize(String serializedPacket) {
        String[] fields = serializedPacket.split("\\$\\$");
        TCPPacket packet = new TCPPacket(Type.valueOf(fields[3]));
        packet.setSrc(fields[0]);
        if (fields[1].equals("")) {
            packet.setTimeStampInit(null);
        } else {
            packet.setTimeStampInit(LocalDateTime.parse(fields[1]));
        }
        if (fields[2].equals("")) {
            packet.setTimeStampEnd(null);
        } else {
            packet.setTimeStampEnd(LocalDateTime.parse(fields[2]));
        }
        int outgoingPathSize = Integer.parseInt(fields[4]);
        for (int i = 0; i < outgoingPathSize; i++) {
            packet.addToOutgoingPath(fields[5].split(";;")[i]);
        }
        int incomingPathSize = Integer.parseInt(fields[6]);
        for (int i = 0; i < incomingPathSize; i++) {
            packet.addToIncomingPath(fields[7].split(";;")[i]);
        }
        packet.setStreamId(Integer.parseInt(fields[8]));
        packet.setStreamPort(Integer.parseInt(fields[9]));
        packet.setStreamInterface(fields[10]);
        packet.setPathToFile(fields[11]);
        packet.setDest(fields[12]);
        int streamsSize = Integer.parseInt(fields[13]);
        for (int i = 0; i < streamsSize; i++) {
            packet.addStream(fields[14].split(";;")[i]);
        }
        return packet;
    }

}