package TCP;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;



public class TCPSender {
    private PrintWriter out;

    public TCPSender(PrintWriter out){
        this.out = out;
    }

    public void sendMessage(String msg){
        out.println(msg);
        out.flush();
    }

    public void JoinMessage(){
        TCPPacket packet = new TCPPacket(TCPPacket.Type.JOIN);
        String serializedPacket = packet.serializa();
        System.out.println("Sending JOIN message: " + serializedPacket);
        sendMessage(serializedPacket);
    }
   
}
