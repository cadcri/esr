package UDP;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RTPPacket {
    private int HEADER_SIZE = 21;

    private int streamID;
    public int sequenceNr;
    public int timestamp;
    private int ssrc;

    private byte[] header;
    public int payload_size;
    public byte[] payload;
    private byte[] destIp;


    public RTPPacket(int streamID, int frameNb, int time, byte[] data, int data_length, String destIp){
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(streamID);
        byte[] result = b.array();

        this.destIp = destIp.getBytes();

        // fill by default header fields
        ssrc = 0;
    
        // fill changing header fields
        this.streamID = streamID;
        sequenceNr = frameNb;
        timestamp = time;
    
        header = new byte[HEADER_SIZE];
    
        header[0] = result[2];
        header[1] = result[3];
        header[2] = (byte)(sequenceNr >> 8);
        header[3] = (byte)(sequenceNr & 0xFF);
        header[4] = (byte)(timestamp >> 24);
        header[5] = (byte)(timestamp >> 16);
        header[6] = (byte)(timestamp >> 8);
        header[7] = (byte)(timestamp & 0xFF);
        header[8] = (byte)(ssrc >> 24);
        header[9] = (byte)(ssrc >> 16);
        header[10] = (byte)(ssrc >> 8);
        header[11] = (byte)(ssrc & 0xFF);

        byte[] destIpBytes = destIp.getBytes();
        if (destIpBytes.length <= HEADER_SIZE - 12) {
            System.arraycopy(destIpBytes, 0, header, 12, destIpBytes.length);
        } else {
            throw new IllegalArgumentException("Destination IP exceeds header size");
        }

        payload_size = data_length;
        payload = new byte[data_length];

        System.arraycopy(data, 0, payload, 0, data_length);


    }

    public RTPPacket(byte[] packet, int packet_size){
        // fill default fields
        byte[] streamIDbytes = {packet[0], packet[1]};
        ByteBuffer buffer = ByteBuffer.wrap(streamIDbytes);
        buffer.order(ByteOrder.BIG_ENDIAN);
        int result = buffer.getShort();

        streamID = result;
        ssrc = 0;

        if (packet_size >= HEADER_SIZE){

            header = new byte[HEADER_SIZE];
            for (int i=0; i < HEADER_SIZE; i++)
                header[i] = packet[i];
                
            payload_size = packet_size - HEADER_SIZE;
            payload = new byte[payload_size];
            for (int i=HEADER_SIZE; i < packet_size; i++)
                payload[i-HEADER_SIZE] = packet[i];

            // Adjust the indices for the sequence number and timestamp
            sequenceNr = unsigned_int(header[3]) + 256*unsigned_int(header[2]);
            timestamp = unsigned_int(header[7]) + 256*unsigned_int(header[6]) + 65536*unsigned_int(header[5]) + 16777216*unsigned_int(header[4]);
        }
    }

    public int getpacket(byte[] packet){
        for (int i=0; i < HEADER_SIZE; i++)
            packet[i] = header[i];
        for (int i=0; i < payload_size; i++)
            packet[i+HEADER_SIZE] = payload[i];

        return payload_size + HEADER_SIZE;
    }

    public String getDestIp(){
        String res = "";
        for(int i = 12; i < HEADER_SIZE; i++){
            res += (char)header[i];
        }
        return res;
    }

    public int getpayload(byte[] data) {
        for (int i=0; i < payload_size; i++)
            data[i] = payload[i];
        
        return payload_size;
    }

    public int getpayload_length() {
        return payload_size;
    }

    public int getlength() {
        return payload_size + HEADER_SIZE;
    }

    public int getStreamID(){
        return streamID;
    }


    // public int getpacket(byte[] packet){
    //     for (int i=0; i < HEADER_SIZE; i++)
    //         packet[i] = header[i];
    //     for (int i=0; i < payload_size; i++)
    //         packet[i+HEADER_SIZE] = payload[i];
    
    //     return payload_size + HEADER_SIZE;
    // }

    // public String getDestIp(){
    //     String res = "";
    //     for(int i = 12; i < HEADER_SIZE; i++){
    //         res += (char)header[i];
    //     }
    //     return res;
    // }

    // public int getpayload(byte[] data) {
    //     for (int i=0; i < payload_size; i++)
    //         data[i] = payload[i];
        
    //     return payload_size;
    // }

    // public int getpayload_length() {
    //     return payload_size;
    // }

    // public int getlength() {
    //     return payload_size + HEADER_SIZE;
    // }

    // public int getStreamID(){
    //     return streamID;
    // }

    // public int gettimestamp() {
    //     return timestamp;
    // }

    public int getsequencenumber() {
        return sequenceNr;
    }


    public byte[] getContent(){
        byte[] res = new byte[header.length + payload.length];
        int i = 0;

        for(byte b: header){
            res[i] = b;
            i++;
        }
        for(byte b: payload){
            res[i] = b;
            i++;
        }

        return res;
    }

    public void changeStreamID(int streamID){
        this.streamID = streamID;

        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(streamID);
        byte[] result = b.array();

        header[0] = result[2];
        header[1] = result[3];
    }

    public void printheader(){
        System.out.println("DestIP:" + destIp);
        System.out.print("[RTP-Header] ");
        System.out.println("Stream ID: " + this.streamID
                           + ", SequenceNumber: " + sequenceNr
                           + ", TimeStamp: " + timestamp
                          + ", Dest: " + new String(destIp));
    }

    public void setDest(String newDest){
        this.destIp = newDest.getBytes();
        byte[] destIpBytes = newDest.getBytes();
        if (destIpBytes.length <= HEADER_SIZE - 12) {
            System.arraycopy(destIpBytes, 0, header, 12, destIpBytes.length);
        } else {
            throw new IllegalArgumentException("Destination IP exceeds header size");
        }
    }

    public RTPPacket clone() {
        try{
            System.out.println("Debug: "+ streamID + " " + sequenceNr + " " + timestamp + " ");
            RTPPacket clonedPacket = new RTPPacket(streamID, sequenceNr, timestamp, payload.clone(), payload_size, new String(destIp));
            System.out.println("Cloned packet:");
            clonedPacket.printheader();
            clonedPacket.header = header.clone();
            return clonedPacket;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }

    }

    public int unsigned_int(int nb) {
        if (nb >= 0)
            return(nb);
        else
            return(256+nb);
    }
}