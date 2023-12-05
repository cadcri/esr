package src;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;


public class Nodo {

    public static void main(String[] argv) {
       new Nodo(25000);
    }

    ////////////
    DatagramSocket RTPsocket;
    byte[] sBuf; //buffer used to store the images to send to the client

    public Nodo(int port) {
        super();

        sBuf = new byte[15000];

        try {
            RTPsocket = new DatagramSocket(port);
            RTPsocket.setSoTimeout(10);
        } catch (SocketException e) {
            System.out.println("src.Cliente: erro no socket: " + e.getMessage());
        }

        while (true) {
            DatagramPacket rcvdp = new DatagramPacket(sBuf, sBuf.length);
            try {
                RTPsocket.receive(rcvdp);
                CelsoPacket packet = new CelsoPacket(rcvdp.getData(), rcvdp.getLength());

                //// DECODE IPS
                int ip_byte_size = packet.getIPBytes(sBuf);
                byte[] string = new byte[ip_byte_size];
                for (int i = 0; i < ip_byte_size; i++){
                    string[i] = sBuf[i];
                }

                String oldPaths = new String(string);
                String[] oldPathsArray = oldPaths.split("-");

                List<String> toSend = new ArrayList<>();
                for(String oldPath: oldPathsArray){
                    String[] steps = oldPath.split(":");
                    if(steps.length > 1){
                        String next = steps[1];
                        if (!toSend.contains(next)){
                            toSend.add(next);
                        }
                    }

                }

                String newPaths = "";
                for(String oldPath: oldPathsArray){
                    String newPath = oldPath.substring(oldPath.indexOf(":")+1);
                    if (!newPaths.equals("")) // add to the total of paths
                        newPaths = newPaths+"-"+newPath;
                    else
                        newPaths = newPath;
                }
                System.out.println(newPaths);
                ///newPaths
                ////////////////

                int video_byte_size = packet.getVideoBytes(sBuf);

                CelsoPacket newPacket = new CelsoPacket((byte) 0x2, sBuf, video_byte_size, newPaths.getBytes(), newPaths.getBytes().length);
                int size = newPacket.getPacketBytes(sBuf);
                rcvdp = new DatagramPacket(sBuf, sBuf.length);
                for (String nextStep : toSend){
                    DatagramPacket senddp = new DatagramPacket(rcvdp.getData(), rcvdp.getLength(), InetAddress.getByName(nextStep.trim()), 25000);
                    RTPsocket.send(senddp);
                }

            } catch (InterruptedIOException iioe) {
                //System.out.println("Nothing to read");
            } catch (IOException ioe) {
                System.out.println("Exception caught: " + ioe);
            }
        }
    }
}