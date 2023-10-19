package streaming.UDP;

import java.util.Timer;

import overlay.state.NodeState;

public class UDPServer extends Thread{
    public static final int PORT = 25000;
    private Timer timer;
    private VideoSender sender;

    public UDPServer(NodeState state){
        this.sender = new VideoSender("movie.Mjpeg", state);
    }
    
    public void run(){
        timer = new Timer();
        timer.schedule(this.sender, 0, VideoSender.FRAME_PERIOD);
    }
}
