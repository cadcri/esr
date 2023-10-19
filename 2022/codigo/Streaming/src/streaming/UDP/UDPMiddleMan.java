package streaming.UDP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import overlay.TCP.TCPCommunicator;
import overlay.state.NodeLink;
import overlay.state.NodeState;
import overlay.state.StreamLink;
import streaming.OTTStreaming;

public class UDPMiddleMan extends Thread{
    NodeState state;
    DatagramPacket senddp;
    DatagramPacket rcvdp;
    DatagramSocket receiver;
    DatagramSocket sender;
    // stream dos quais é o nodo responsável
    Map<Integer, StreamLink> myStreams;

    byte[] buf;
    int bufLength = 15000;
  
    public UDPMiddleMan(NodeState state) {
        this.state = state;

        buf = new byte[bufLength];
        try{
            sender = new DatagramSocket();
            receiver = new DatagramSocket(UDPServer.PORT);
            rcvdp = new DatagramPacket(buf, buf.length);
            this.myStreams = new HashMap<>();
        }
        catch(SocketException e){
            System.out.println("Servidor: erro no socket: " + e.getMessage());
        }
        catch (Exception e){
            System.out.println("Servidor: erro no video: " + e.getMessage());
        }
    }

    public void run(){
        try{
            while(true){
                receiver.receive(rcvdp);

                RTPPacket rtp_packet = new RTPPacket(rcvdp.getData(), rcvdp.getLength());

                int streamID = rtp_packet.getStreamID();
                // reencaminha pacotes
                sendPacket(streamID, false, rtp_packet, 0);
                
                // replica os pacotes para as streams das quais é o nodo responsável
                for(Map.Entry<Integer, StreamLink> entry: this.myStreams.entrySet()){
                    sendPacket(entry.getValue().getStreamID(), true, rtp_packet, rcvdp.getLength());
                }

            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void sendPacket(int streamID, boolean changeID, RTPPacket rtp_packet, int packet_size) throws Exception{
        StreamLink stream = this.state.getStreamFromID(streamID);

        if (stream != null){
            byte[] buffer = {};
            if (changeID){
                rtp_packet.changeStreamID(streamID);
                buffer = rtp_packet.getContent();
            }
            else{
                buffer = buf;
            }
            rtp_packet.printheader();

            String nextNode = stream.findNextNode(this.state.getSelf(), false);

            if (nextNode.equals(this.state.getSelf())){
                List<InetAddress> ips = this.state.getSelfIPs();
                senddp = new DatagramPacket(buffer, buffer.length, ips.get(0), OTTStreaming.RTP_PORT);
                sender.send(senddp);
            }
            else{
                NodeLink link = this.state.getLinkTo(nextNode);
                if (link != null){
                    senddp = new DatagramPacket(buffer, buffer.length, link.getViaInterface(), UDPServer.PORT);
                    sender.send(senddp);
                }
            }    
        }
    }

    // adiciona stream à lista das quais é responsável
    public void sendTo(StreamLink stream){
        this.myStreams.put(stream.getStreamID(), stream);
    }

    // remove stream à lista das quais é responsável
    public void doNotSendTo(StreamLink stream){
        this.myStreams.remove(stream.getStreamID(), stream);
    }

    public void removeAllMyStreams(){
        this.myStreams = new HashMap<>();
    }

    public boolean hasDependentStreams(StreamLink stream){
        return this.myStreams.size() > 0;
    }
}
