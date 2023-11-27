package UDP;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

 
public class RTPPacket {
    private int HEADER_SIZE = 12;

    private int streamID;
    private int sequenceNr;
    private int timestamp;
    private int ssrc;

    private byte[] header;
    private int payload_size;
    private byte[] payload;
    //how should i store the path?
    private byte[] path;


    public RTPPacket(int streamID, int frameNb, int time, byte[] data, int data_length, String path){
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(streamID);
        byte[] result = b.array();

        // fill by default header fields
        ssrc = 0;

        // add the path to the header
        byte [] pathBytes = path.getBytes();
        byte [] pathSize = new byte[4];
        pathSize[0] = (byte) (pathBytes.length >> 24);
        pathSize[1] = (byte) (pathBytes.length >> 16);
        pathSize[2] = (byte) (pathBytes.length >> 8);
        pathSize[3] = (byte) (pathBytes.length);
    
        // fill changing header fields
        this.streamID = streamID;
        sequenceNr = frameNb;
        timestamp = time;
    
        //this now has to accomodae the path and the path size for decoding
        header = new byte[HEADER_SIZE+pathBytes.length+4];
        //add path to header
        System.arraycopy(pathSize, 0, header, 0, pathBytes.length);
    
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

        payload_size = data_length;
        payload = new byte[data_length];

        for (int i=0; i < data_length; i++)
            payload[i] = data[i];
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

    public int gettimestamp() {
        return timestamp;
    }

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
        System.out.print("[RTP-Header] ");
        System.out.println("Stream ID: " + this.streamID
                           + ", SequenceNumber: " + sequenceNr
                           + ", TimeStamp: " + timestamp);
    }

    public int unsigned_int(int nb) {
        if (nb >= 0)
            return(nb);
        else
            return(256+nb);
    }
}
