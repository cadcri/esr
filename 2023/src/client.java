import java.net.Socket;
import java.util.Scanner;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.Thread;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.System;


public class Client {

    public int streamPort=30001;
    public int listeningPort=30000;
    private OutputStream os;
    private Socket readSocket;
    private Socket writeSocket;
    private PrintWriter pw;
    private BufferedReader in;

    private enum Message{
    PROBE,
    ACK,
    JOIN,
    JOIN_ACK,
    LEAVE,
    LEAVE_ACK,
    TEXT
    }


    public Client(String rpIP){
        System.out.println("RP: "+rpIP);
        new Thread(() ->{
            createSockets(rpIP);
        }).start();
        
        try{
            read(this.readSocket);
            Scanner sc = new Scanner(System.in);
            String option = "";
            while(!option.equals("4")){
                System.out.println("What do you want to do?");
                System.out.println("1. Add content to stream");
                System.out.println("2. Get neighbor list");
                System.out.println("3. Request content");
                System.out.println("4. Exit");

                option = sc.nextLine();

                switch(option){
                    case("1"):
                        System.out.println("Adding content to stream");
                        // System.out.println("What do you want to add?");
                        // String content = sc.nextLine();
                        // sendMessage(content, rpIP);
                        break;

                    case("2"):
                        System.out.println("Getting neighbor list");
                        // sendText("getNeighbours", rpIP);
                        break;

                    case("3"):
                        System.out.println("Requesting content");
                        break;
                }


            }

        }
        finally{
            try{
                this.readSocket.close();
                this.writeSocket.close();
                return;
            }
            catch(Exception e){
                System.out.println("Error closing sockets");
            }
        }
    }

    private void createSockets(String rpIp){
        
        try{

            //fechar o socket
            System.out.println("Creating write socket"+streamPort);
            this.writeSocket = new Socket(rpIp, streamPort);
            System.out.println("Creating read socket");
            ServerSocket socket = new ServerSocket(listeningPort);
            System.out.println("Reading socket created");
            this.readSocket = socket.accept();
            in = new BufferedReader(new InputStreamReader(this.readSocket.getInputStream()));
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Error creating sockets");
            System.exit(1);
        }

    }

    
    public void sendText(Message message, String to){
        
        System.out.println("Sending text: "+message+" to: "+to);

        try{
            //Should close the socket 
            System.out.println("Connected to "+to+" on port "+streamPort);
            pw = new PrintWriter(writeSocket.getOutputStream(), true);
            pw.println(message);
        }
        catch(Exception e){
            System.out.println("Error sending message: "+message);
        }      
    }

    public void read(Socket socket){

        new Thread(() -> {
            while (this.readSocket==null){
                try{
                    Thread.sleep(100);
                }
                catch(Exception e){
                    System.out.println("Error sleeping");
                }
            }
            while(this.readSocket.isConnected()){
                try{
                    String inputLine;
                    while ((inputLine=in.readLine()) != null) {
                        System.out.println("Received data: " + inputLine);
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                    System.out.println("Something went wrong accepting connections(Server Socket)");
                    System.exit(1);
                }
            }
            System.out.println("Socket is disconnected");
        }).start();
    }


}
