package UDP;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class PacketManager {
    private FileInputStream fis;
    private Map<Integer, byte[]> packets;
    private Map<Integer, Integer> imageLengths;

    public PacketManager(String filename){
        try{
            fis = new FileInputStream(filename);
            this.packets = new HashMap<>();
            this.imageLengths = new HashMap<>();
            System.out.println("Reading video file...");
            readVideo();
        }catch(Exception e){
            System.out.println("An error has orccured while reading the video file");
            
        }
    }

    public void readVideo() throws Exception{
        int i = 1;
        byte[] frameLength = new byte[5];

        while(this.fis.read(frameLength, 0, 5) != -1){
            int length = 0;
            String lengthString;
            lengthString = new String(frameLength);
            length = Integer.parseInt(lengthString);
            byte[] frame = new byte[15000];
            int imageLength = fis.read(frame, 0, length);

            this.packets.put(i, frame);
            this.imageLengths.put(i, imageLength);
            i++;

            frameLength = new byte[5];
        }
    }

    public int getImageLength(int frame){
        return this.imageLengths.get(frame);
    }

    public byte[] getFrame(int frame){
        return this.packets.get(frame);
    }

    public int getNumberOfFrames(){
        return this.packets.size();
    }

    public int getNextFrame(byte[] frame) throws Exception{
        int length = 0;
        String lengthString;
        byte[] frameLength = new byte[5];

        fis.read(frameLength, 0, 5);

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < frameLength.length; i++)
            sb.append(i + ": " + frameLength[i] + "\n");
        sb.append("\n");
        System.out.println(sb);

        lengthString = new String(frameLength);
        length = Integer.parseInt(lengthString);

        return fis.read(frame, 0, length);
    }
}
