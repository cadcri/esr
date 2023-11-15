import java.net.*;
import java.util.HashMap;
import java.util.Scanner;
import java.io.*;
import java.awt.List;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.util.ArrayList;



public class Servidor {
    
    //private HashMap<String, Content> content = new HashMap<String, Content>();
    private static ArrayList<String> connectedClients;
    private static String option = "";
    private static Scanner scanner = new Scanner(System.in);
    //PORTS LIST
    //Accept connections 3000
    //Send files 5000
    //Request files 4000

    public static void main(String[] args){
        try{
            connectionManager();
            while(true){
                //Add feature to add the content and content type to a hashmap, meanwhile, we could be caching the content to make it faster
                System.out.println("Instructions:\nlist: lists all connected clients\nsend (filename): send file to all connected clients\nexit: exit program\n");
                System.out.println("What would you like to do:");
                option= scanner.nextLine();
                if (option.equals("list")) {
            
                        System.out.println("Connected clients:");
                        System.out.println(connectedClients.toString());
                
                }
                else if(option.equals("exit")){
                        System.exit(1);
                        //This should notify everyone in the network that this client is leaving
                        System.out.println("Exiting program");
                }
                else if(option.matches("send (.*)")){
                     String[] parts = option.split(" ");
                            String file = parts[1];
                            new Thread(() -> {
                                System.out.println("Sending file to all connected clients");
                                ContentSharing(connectedClients, file);
                            }).start();
                }
                else{
                    System.out.println("Invalid option");
                }
            }
        }catch(Exception e){
            e.printStackTrace();
            System.out.println("Error accepting connection");
        }
    }

    public static void connectionManager() {
        try  {
            // Creating a new UDP socket
            //This is a bad coding practice, i should include a finnally so i can close the socket before killing the program but since this is just a test i will leave it like this
            DatagramSocket socket = new DatagramSocket(3000);
            // Creating a new thread to listen for new connections
            new Thread(() -> {
                while (true) {
                    try {
                        byte[] buffer = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String data = new String(packet.getData()).trim();
                        System.out.println("Data received: " + data);
                        connectedClients.add(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Error receiving data");
                    }
                }

            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error creating socket");
        } 
        /* 
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        */
    }

    public static void ReceiveData(){
        try{
            ServerSocket socket = new ServerSocket(4000);
            new Thread(() -> {
                while(true){
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
            }).run();
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Something went wrong(Server Socket)");
        }
    }


    public static void ContentSharing(ArrayList<String> connectedClients, String file) {
        try {
            for(String i: connectedClients){
                // send file to client i
                Socket socket = new Socket(i, 5000);
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(file.getBytes());
                outputStream.flush();
                socket.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error sending data");
        }
    }
    

}
