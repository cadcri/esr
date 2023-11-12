package src;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.Thread;

public class client extends node{

    public int acceptPort=5000;
    public int sendPort=5001;
    
    public void sendText(String text, String to){
        
        System.out.println("Sending text: "+text+" to: "+to);
        try{
            Socket socket = new Socket(to, sendPort);
            socket.getOutputStream().write(text.getBytes());
            socket.close();
            System.out.println("Text sent");
        }
        catch(Exception e){
            System.out.println("Error sending text");
        }

    }

    public void read(){
        System.out.println("Reading...");
        try{
            new Thread(() -> {
                try{
                    ServerSocket socket = new ServerSocket(acceptPort);
                    Socket clientSocket = socket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));;
                    while(true){
                        String response = in.readLine();
                        System.out.println("Received text: "+response);
                    }
                }
                catch(Exception e){
                    System.out.println("Error reading");
                }
            }).run();

        }
        catch(Exception e){
            System.out.println("Error reading");
        }
    }

}
