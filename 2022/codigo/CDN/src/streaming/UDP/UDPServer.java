package streaming.UDP;

import java.net.InetAddress;
import java.util.Timer;

import overlay.state.NodeState;
import overlay.state.StreamLink;

public class UDPServer extends Thread{
    public static final int PORT = 25000;
    private Timer timer;
    private boolean running;
    private VideoSender sender;

    public UDPServer(InetAddress ownIP, StreamLink stream, NodeState state, VideoStream video){
        this.running = true;
        this.sender = new VideoSender(ownIP, video, stream, state);
    }
    
    public void run(){
        timer = new Timer();
        timer.schedule(this.sender, 0, VideoSender.FRAME_PERIOD);
    }

    public void pauseSender() throws Exception{
        System.out.println("pause");
        if(running){
            sender.pause();
            this.running = false;
        }
        else{
            sender.resume();
            this.running = true;
        }
    }

    public void cancelSender(){
        timer.cancel();
    }
}
