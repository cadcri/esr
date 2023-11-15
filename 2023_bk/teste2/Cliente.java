import java.net.*;
import java.util.HashMap;
import java.util.Scanner;
import java.io.*;
import java.awt.List;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.util.ArrayList;



public class Cliente {
    
    //private HashMap<String, Content> content = new HashMap<String, Content>();
    private static String option = "";
    private static final String rp = "10.0.1.21";
    private static Scanner scanner = new Scanner(System.in);
    //PORTS LIST
    //Accept connections 3000
    //Send files 5000
    //Request files 4000
    public static void main(String[] args){
        connectToRP();
        System.out.println("Cliente");
        //listenFromRP();
        ServerSocket socket = new ServerSocket(5000);
        Socket client = socket.accept();
        while(true){
            System.out.println("Connection accepted with ip: "+ client.getInetAddress().getHostAddress());
            try{
                Socket clientSocket = socket.accept();
                System.out.println("Connection accepted");
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Received data: " + inputLine);
                }
                in.close();
                clientSocket.close();
            }
            catch(Exception e){
                e.printStackTrace();
                System.out.println("Something went wrong accepting connections(Server Socket)");
            }
        }    

    }

    //Estabelecer conexão com o RP
    public static void connectToRP(){
        try{
            Socket socket = new Socket(rp, 3000);
            System.out.println("Connected to RP");
            socket.close();
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Error connecting to RP");
        }
    }

    //Pedir coisas ao RP

    //Estar a ouvir coisas do RP
    public static void listenFromRP(){
        new Thread(() -> {
            try{
            ServerSocket socket = new ServerSocket(5000);
            Socket client = socket.accept();
            while(true){
                System.out.println("Connection accepted with ip: "+ client.getInetAddress().getHostAddress());
                try{
                    Socket clientSocket = socket.accept();
                    System.out.println("Connection accepted");
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        System.out.println("Received data: " + inputLine);
                    }
                    in.close();
                    clientSocket.close();
                }
                catch(Exception e){
                    e.printStackTrace();
                    System.out.println("Something went wrong accepting connections(Server Socket)");
                }
            }    
            }

            catch(Exception e){
                e.printStackTrace();
                System.out.println("Error ");
            }
            
        });
    }


}
