package streaming.UDP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.TimerTask;

import overlay.TCP.TCPCommunicator;
import overlay.state.NodeLink;
import overlay.state.NodeState;
import overlay.state.StreamLink;

public class VideoSender extends TimerTask{
    private DatagramPacket senddp;
    private DatagramSocket RTPsocket;
    private int RTP_PORT = 25000;
    private InetAddress ownIP;
    private byte[] buf;
    public static int bufLength = 15000;

    public static int FRAME_PERIOD = 100;
    private int imagenb = 0;
    private VideoStream video;
    public static int VIDEO_LENGTH = 500;
    private boolean running;
    private StreamLink stream;
    private NodeState state;
    

    public VideoSender(InetAddress ownIP, VideoStream videoStream, StreamLink stream, NodeState state){
        this.buf = new byte[bufLength];
        this.running = true;
        
        try {
            this.RTPsocket = new DatagramSocket();
            this.ownIP = ownIP;
            this.stream = stream;
            this.video = videoStream;
            this.state = state;
        } catch (SocketException e) {
            System.out.println("Servidor: erro no socket: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Servidor: erro no video: " + e.getMessage());
        }
    }

    public void pause(){
        this.running = false;
    }

    public void resume(){
        this.running = true;
    }

    public void run(){
        if(running){
            if (imagenb < VIDEO_LENGTH){
                imagenb++;

                try {
                    int imageLength = video.getImageLength(imagenb);
                    buf = video.getFrame(imagenb);
            
                    RTPPacket RTPPacket = new RTPPacket(this.stream.getStreamID(), imagenb, imagenb * FRAME_PERIOD, buf, imageLength);
                    int packetLength = RTPPacket.getlength();
  
                    byte[] packet_bits = new byte[packetLength];
                    RTPPacket.getpacket(packet_bits);
  
                    int streamID = RTPPacket.getStreamID();
                    StreamLink stream = this.state.getStreamFromID(streamID);

                    if (stream.getActive() == true){
                        String nextNode = stream.findNextNode(this.state.getSelf(), false);
                        NodeLink link = this.state.getLinkTo(nextNode);
                        if (link != null){
                            senddp = new DatagramPacket(packet_bits, packetLength, link.getViaInterface(), RTP_PORT);
                            RTPsocket.send(senddp);
  
                            RTPPacket.printheader();
                        }
                        else
                            imagenb--;
                    }
                    else
                        imagenb--;
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
            else{
                this.running = false;
                TCPCommunicator client;
                client = new TCPCommunicator(null, this.ownIP, TCPCommunicator.END_STREAM_CLIENT, this.stream.convertLinkToArgs());
                client.run();
            }
        }
    }
}
