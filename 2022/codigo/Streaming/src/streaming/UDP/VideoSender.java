package streaming.UDP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.List;
import java.util.TimerTask;

import overlay.state.NodeLink;
import overlay.state.NodeState;
import overlay.state.StreamLink;

// envia o vídeo, só usado em servidores
public class VideoSender extends TimerTask{
    private DatagramPacket senddp;
    private DatagramSocket RTPsocket;
    private int RTP_PORT = 25000;
    private byte[] buf;
    public static int bufLength = 15000;

    public static int FRAME_PERIOD = 42;
    private int imagenb = 0;
    private VideoStream video;
    public static int VIDEO_LENGTH = 500;
    private boolean running;
    private NodeState state;
    

    public VideoSender(String videoFileName, NodeState state){
        this.buf = new byte[bufLength];
        this.running = true;
        
        try {
            this.RTPsocket = new DatagramSocket();
            this.video = new VideoStream(videoFileName);
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
            
                    List<StreamLink> streams = this.state.getStreamLinks();

                    // envia para todas as streams ativas que partem dele
                    for(StreamLink slink: streams){
                        if(slink != null){
                            RTPPacket RTPPacket = new RTPPacket(slink.getStreamID(), imagenb, imagenb * FRAME_PERIOD, buf, imageLength);
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
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
            // reinicia o loop
            else{
                imagenb = 0;
            }
        }
    }
}
