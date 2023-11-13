import java.net.Socket;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.Thread;
import java.io.OutputStream;
import java.io.PrintWriter;


public class Client {

    public int acceptPort=5000;
    public int sendPort=5001;
    private OutputStream os;
    private Socket readSocket;
    private Socket writeSocket;
    private PrintWriter pw;
    private ServerSocket listeningSocket;

    public Client(String rpIP){
        System.out.println("RP: "+rpIP);
        createSockets(rpIP);
        try{
            read(this.readSocket);
        }
        finally{
            try{
                this.readSocket.close();
                this.writeSocket.close();
            }
            catch(Exception e){
                System.out.println("Error closing sockets");
            }
        }
        // try{
        //     writeSocket =new Socket("10.0.0.21", acceptPort);
        // }
        // catch(Exception e){
        //     System.out.println("Error creating socket");
        // }
    }

    private void createSockets(String rpIp){
        
        try{
            this.writeSocket = new Socket(rpIp, sendPort);
            ServerSocket socket = new ServerSocket(acceptPort);
            this.readSocket = socket.accept();
        }
        catch(Exception e){
            System.out.println("Error creating sockets");
        }

    }

    
    public void sendText(String text, String to){
        
        System.out.println("Sending text: "+text+" to: "+to);

        try{
        //Should close the socket 
        System.out.println("Connected to"+to+" on port "+sendPort);
        pw = new PrintWriter(writeSocket.getOutputStream(), true);
        pw.println(text);
        }
        catch(Exception e){
            System.out.println("Error sending text");
        }      


    }

    public void read(Socket socket){

        new Thread(() -> {
            while(true){
                try{
                    //Socket clientSocket = socket.accept();
                    System.out.println("Connection accepted");
                    BufferedReader in = new BufferedReader(new InputStreamReader(readSocket.getInputStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        System.out.println("Received data: " + inputLine);
                    }
                    in.close();
                    readSocket.close();
                }
                catch(Exception e){
                    e.printStackTrace();
                    System.out.println("Something went wrong accepting connections(Server Socket)");
                }
            }
        }).start();
    
    }


}
