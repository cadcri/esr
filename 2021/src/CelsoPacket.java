package src;

import java.io.Serializable;

public class CelsoPacket implements Serializable {

    private byte[] header;
    private byte[] payload;

    public CelsoPacket(byte type, byte[] data, int data_length, byte[] dataIPs, int dataIPs_length) {
        header = new byte[9];
        header[0] = (byte) (data_length >> 24);
        header[1] = (byte) (data_length >> 16);
        header[2] = (byte) (data_length >> 8);
        header[3] = (byte) (data_length);
        header[4] = (byte) (dataIPs_length >> 24);
        header[5] = (byte) (dataIPs_length >> 16);
        header[6] = (byte) (dataIPs_length >> 8);
        header[7] = (byte) (dataIPs_length);
        header[8] = type;
        payload = new byte[data_length + dataIPs_length];

        for (int i = 0; i < data_length; i++)
            payload[i] = data[i];
        for (int i = 0; i < dataIPs_length; i++)
            payload[data_length + i] = dataIPs[i];

    }

    public CelsoPacket(byte[] packet, int packetSize) {
        header = new byte[9];
        for (int i = 0; i < 9; i++)
            header[i] = packet[i];
        payload = new byte[packetSize-9];
        for (int i = 9; i < packetSize; i++)
            payload[i - 9] = packet[i];
    }

    public int getPacketBytes(byte[] buff){
        for (int i = 0; i < 9; i++)
            buff[i] = header[i];
        for (int i = 0; i < payload.length; i++)
            buff[i+9] = payload[i];
        return payload.length+9;
    }

    public int getVideoBytes(byte[] buff){
        int datalength = ((header[0] & 0xFF) << 24) |
                ((header[1] & 0xFF) << 16) |
                ((header[2] & 0xFF) << 8 ) |
                ((header[3] & 0xFF) << 0 );
        for (int i = 0; i < datalength; i++)
            buff[i] = payload[i];
        return datalength;
    }

    public int getIPBytes(byte[] buff){
        int datalength = ((header[0] & 0xFF) << 24) |
                ((header[1] & 0xFF) << 16) |
                ((header[2] & 0xFF) << 8 ) |
                ((header[3] & 0xFF) << 0 );
        int dataIPs_length = ((header[4] & 0xFF) << 24) |
                ((header[5] & 0xFF) << 16) |
                ((header[6] & 0xFF) << 8 ) |
                ((header[7] & 0xFF) << 0 );

        for (int i = 0; i < dataIPs_length; i++)
            buff[i] = payload[i+datalength];
        return dataIPs_length;
    }

    public boolean continueStream(){
        return header[8] == 0x0;
    }
}
