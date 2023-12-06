package Nodes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import TCP.*;
import UDP.*;
import Structs.*;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.Map;
import javax.swing.JOptionPane;

public class Client extends JFrame implements ActionListener {

    private JLabel iconLabel;
    private Timer cTimer;
    private byte[] cBuf;
    private OutputStream os;
    private PrintWriter pw;
    private BufferedReader in;
    private Node node;

    private UDPManager udpManager;
    private TCPManager tcpManager;
    private int lastDisplayedPacket=-1;
    private int streamId = -1;

    private boolean play;

    private Boolean connected = false;
    JButton playButton;
    JButton pauseButton;
    JButton juntarButton;
    JButton initButton;
    JPanel mainPanel;
    JPanel buttonPanel;
    JButton listStreamButton;

    public Client(Node node) {


        this.node = node;

        //this.tcpHandler = new Manager(this.node);
        

        System.out.println("Num dft gateways: " + this.node.getGateways().size());
        System.out.println("Dft gateways: " + this.node.getGateways().toString());


        // udpManager = new UDPManager(this.node);
        try {
            this.udpManager = new UDPManager(this.node);
            this.tcpManager = new TCPManager(this.node, this.udpManager);
        } catch (Exception e) {
            System.out.println("Error creating TCPManager");
        }

        //Try to join the network, while it cant join, it will keep trying
        while(this.node.estado!=Node.state.on){
            System.out.println("Trying to join network");
            try {
                for (String gateway : this.node.getGateways()) {
                    //this.tcpManager.createWriting(gateway);
                    TCPPacket packet = new TCPPacket(TCPPacket.Type.JOIN);
                    packet.addToOutgoingPath(gateway);
                    //packet.addToIncomingPath(gateway);
                    this.tcpManager.sendPacket(gateway, packet);
                    try{
                        Thread.sleep(2000);
                    }catch(Exception e){
                        System.out.println("Error sleeping");
                    }

                }
            } catch (Exception e) {
                System.out.println("Error joining network");
            }
        }

        this.tcpManager.listRoutes();

        new Thread(() -> {
            while(true) this.videoWatcher();
        }).start();
    
        playButton = new JButton("Play");
        pauseButton = new JButton("Pause");
        juntarButton = new JButton("Juntar Stream");
        initButton = new JButton("Iniciar Stream");
        listStreamButton = new JButton("Listar Streams");
        mainPanel = new JPanel();
        buttonPanel = new JPanel();
        iconLabel = new JLabel();
        buttonPanel.setLayout(new GridLayout(1, 0));
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(listStreamButton);
        buttonPanel.add(juntarButton);
        buttonPanel.add(initButton);
        iconLabel.setIcon(null);
        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);
        iconLabel.setBounds(0, 0, 380, 280);
        buttonPanel.setBounds(0, 280, 380, 50);
        getContentPane().add(mainPanel, BorderLayout.CENTER);

        setSize(new Dimension(390, 370));
        setVisible(true);


        playButton.addActionListener(e -> {
            System.out.println("Play button pressed");
            cTimer.start();
            playButtonPressed();
        });
        pauseButton.addActionListener(e -> {
            System.out.println("Pause button pressed");
            cTimer.stop();
            pauseButtonPressed();
        });
        juntarButton.addActionListener(e -> {
            System.out.println("Juntar Stream button pressed");
            cTimer.stop();
            juntarButtonPressed();
        });
        initButton.addActionListener(e -> {
            System.out.println("Iniciar Stream button pressed");
            cTimer.stop();
            initButtonPressed();
        });
        listStreamButton.addActionListener(e -> {
            System.out.println("Listar Streams button pressed");
            cTimer.stop();
            listStreamButtonPressed();
        });

        cTimer = new Timer(20, this);
        cTimer.setInitialDelay(0);
        cTimer.setCoalesce(true);
        cBuf = new byte[15000];
        cTimer.start(); //// TOREMOVE
    }

    private void videoWatcher(){
        //System.out.println("Debug: "+ this.udpManager.content.size());
        if(this.udpManager.content.size()>0){
            //Start reading the packets
            //System.out.println("Video watcher started");
            //System.out.println("Debug2: "+ this.udpManager.content.get(this.streamId).size() + " " + this.udpManager.content.get(this.streamId).size());
            if(this.udpManager.content.containsKey(this.streamId) && this.udpManager.content.get(this.streamId).size()>0){
                //Lets get the packet
                RTPPacket packet = this.udpManager.content.get(this.streamId).poll();
                System.out.println("Packet: " + packet.getsequencenumber());
                System.out.println("Packet size: "+ packet.payload_size);
                System.out.println("Payload len:"+ packet.getpayload_length());
                if(packet.getsequencenumber()>this.lastDisplayedPacket){
                    //Then we display the packet
                    this.lastDisplayedPacket = packet.getsequencenumber();
                    int payload_length = packet.getpayload_length();
                    byte[] payload = new byte[payload_length];
                    packet.getpayload(payload);

                    Toolkit toolkit = Toolkit.getDefaultToolkit();
                    Image image = toolkit.createImage(payload, 0, payload_length);

                    ImageIcon icon = new ImageIcon(image);

                    if (play) {
                         iconLabel.setIcon(icon);
                    }
                }
            }
        }
    }
    

    private void playButtonPressed() {
        // Add your code here for when the Play button is pressed
        cTimer.start();

        play = true;
    }

    private void pauseButtonPressed() {
        // Add your code here for when the Pause button is pressed
        //cTimer.stop();
        play = false;
    }

    private void juntarButtonPressed() {
        // Get the available streams and then show a window for the user to choose the stream
        this.tcpManager.streamsUpdated = false;
        TCPPacket packet = new TCPPacket(TCPPacket.Type.LIST_STREAMS);
        this.tcpManager.sendPacket(null, packet);
        while (!this.tcpManager.streamsUpdated) {
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                System.out.println("Error sleeping");
            }
        }
        Map<Integer, String> streams = new HashMap<>(this.tcpManager.streams);

        // Show a window with the available streams
        String[] streamOptions = streams.values().toArray(new String[0]);
        String selectedStream = (String) JOptionPane.showInputDialog(
                this,
                "Select a stream:",
                "Available Streams",
                JOptionPane.PLAIN_MESSAGE,
                null,
                streamOptions,
                null
        );

        if (selectedStream != null) {
            System.out.println("Stream escolhido: " + selectedStream);
            TCPPacket packet2 = new TCPPacket(TCPPacket.Type.REQUEST_STREAM);
            //Get the index of that stream
            for (Map.Entry<Integer, String> entry : streams.entrySet()) {
                if (entry.getValue().equals(selectedStream)) {
                    packet2.setStreamId(entry.getKey());
                    break;
                }
            }
            this.tcpManager.sendPacket(null, packet2);
            //Rever esta lógica porque a stream pode já ter acabado
            this.streamId = packet2.getStreamId();
            System.out.println("Stream ID:"+ this.streamId);
            //eliminar tudo se der join noutra stream
            //Rever isto
            if(this.udpManager.content.get(packet2.getStreamId())!=null){
                this.udpManager.content.remove(packet2.getStreamId());
            }
        }
    }

    private void initButtonPressed() {
        // Add your code here for when the Iniciar Stream button is pressed
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a file");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Files", "mp4", "mp3", "txt")); // Add the file extensions you want to allow

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            TCPPacket packet = new TCPPacket(TCPPacket.Type.STREAM);
            packet.setPathToFile(filePath);
            packet.setSrc(null);
            packet.setDest(null);
            this.tcpManager.sendPacket(null,packet);
        }
    }

    private void listStreamButtonPressed() {
        // Add your code here for when the Listar Streams button is pressed
        this.tcpManager.streamsUpdated = false;
        System.out.println("Sent packet");
        TCPPacket packet = new TCPPacket(TCPPacket.Type.LIST_STREAMS);
        System.out.println("Teste");
        this.tcpManager.sendPacket(null, packet);
        System.out.println("Teste2");
        while (!this.tcpManager.streamsUpdated) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                System.out.println("Error sleeping");
            }
        }
        Map<Integer, String> streams = new HashMap<>(this.tcpManager.streams);

        // Show a window with the available streams
        String[] streamOptions = streams.values().toArray(new String[0]);
        String selectedStream = (String) JOptionPane.showInputDialog(
                this,
                "Select a stream:",
                "Available Streams",
                JOptionPane.PLAIN_MESSAGE,
                null,
                streamOptions,
                null
        );

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        videoWatcher();
    }

}
