package streaming.UDP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
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
    Map<Integer, RTPPacket> videoPackets;
    Map<Integer, TemporarySender> threads;

    byte[] buf;
    int bufLength = 15000;
  
    public UDPMiddleMan(NodeState state) {
        this.state = state;

        buf = new byte[bufLength];
        try{
            sender = new DatagramSocket();
            receiver = new DatagramSocket(UDPServer.PORT);
            rcvdp = new DatagramPacket(buf, buf.length);
            videoPackets = new HashMap<>();
            threads = new HashMap<>();
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
                videoPackets.put(rtp_packet.getsequencenumber() - 1, rtp_packet);
                rtp_packet.printheader();

                int streamID = rtp_packet.getStreamID();
                StreamLink stream = this.state.getStreamFromID(streamID);

                if (stream != null){
                    String nextNode = stream.findNextNode(this.state.getSelf(), false);

                    if (nextNode.equals(this.state.getSelf())){
                        List<InetAddress> ips = this.state.getSelfIPs();
                        senddp = new DatagramPacket(buf, buf.length, ips.get(0), OTTStreaming.RTP_PORT);
                        sender.send(senddp);
                    }
                    else{
                        NodeLink link = this.state.getLinkTo(nextNode);
                        if (link != null){
                            senddp = new DatagramPacket(buf, buf.length, link.getViaInterface(), UDPServer.PORT);
                            sender.send(senddp);
                        }
                    }    
                }            
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void sendTo(StreamLink stream){
        TemporarySender tmpSender = new TemporarySender(state, videoPackets, stream);
        this.threads.put(stream.getStreamID(), tmpSender);
        Timer timer = new Timer();
        timer.schedule(tmpSender, 0, VideoSender.FRAME_PERIOD);
    }

    public void pauseSender(int streamID){
        this.threads.get(streamID).switchRunning();
    }

    public void turnOffSender(int streamID){
        this.threads.get(streamID).cancel();
        this.threads.remove(streamID);
    }

    class TemporarySender extends TimerTask{
        private int frame_nr;
        private Map<Integer, RTPPacket> videoPackets;
        private NodeState state;
        private StreamLink stream;
        private boolean running;

        TemporarySender(NodeState state, Map<Integer, RTPPacket> videoPackets, StreamLink stream){
            this.frame_nr = 0;
            this.videoPackets = videoPackets;
            this.state = state;
            this.stream = stream;
            this.running = true;
        }

        public void switchRunning(){
            if (this.running == true)
                this.running = false;
            else
                this.running = true;
        }

        public void run(){
            try{
                if (this.running){
                    if(this.frame_nr < VideoSender.VIDEO_LENGTH){
                        RTPPacket rtp_packet = this.videoPackets.get(this.frame_nr);
                        rtp_packet.changeStreamID(this.stream.getStreamID());
                        rtp_packet.printheader();
    
                        int streamID = rtp_packet.getStreamID();
                        StreamLink stream = this.state.getStreamFromID(streamID);
                        String nextNode = stream.findNextNode(this.state.getSelf(), false);
    
                        NodeLink link = this.state.getLinkTo(nextNode);
                        byte[] buffer = rtp_packet.getContent();
                        DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, link.getViaInterface(), UDPServer.PORT);
                        sender.send(sendPacket);
                        this.frame_nr ++;
                    }
                    else{
                        TCPCommunicator client;
                        List<InetAddress> ips = this.state.getSelfIPs();
                        client = new TCPCommunicator(null, ips.get(0), TCPCommunicator.END_STREAM_CLIENT, this.stream.convertLinkToArgs());
                        client.run();
                        threads.get(this.stream.getStreamID()).cancel();
                    }
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
