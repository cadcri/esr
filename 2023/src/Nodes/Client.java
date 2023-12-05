package Nodes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.PrintWriter;
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

    private Boolean connected = false;
    JButton playButton;
    JButton pauseButton;
    JButton juntarButton;
    JButton initButton;
    JPanel mainPanel;
    JPanel buttonPanel;
    JButton listStreamButton;

    public Client(Node node){

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
            try {
                for (String gateway : this.node.getGateways()) {
                    System.out.println("Sending join message to: " + gateway);
                    //this.tcpManager.createWriting(gateway);
                    TCPPacket packet = new TCPPacket(TCPPacket.Type.JOIN);
                    packet.addToOutgoingPath(gateway);
                    //packet.addToIncomingPath(gateway);
                    this.tcpManager.sendPacket(gateway, packet);
                    try{
                        Thread.sleep(1000);
                    }catch(Exception e){
                        System.out.println("Error sleeping");
                    }

                }
            } catch (Exception e) {
                System.out.println("Error joining network");
            }
        }
    
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
            cTimer.start();
        });
        pauseButton.addActionListener(e -> {
            cTimer.stop();
            System.exit(0);
        });
        juntarButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                juntarButtonPressed();
                }
            });
        cTimer = new Timer(20, this);
        cTimer.setInitialDelay(0);
        cTimer.setCoalesce(true);
        cBuf = new byte[15000];
        cTimer.start(); //// TOREMOVE

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onExit();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == playButton) {
            playButtonPressed();
        } else if (e.getSource() == pauseButton) {
            pauseButtonPressed();
        } else if (e.getSource() == juntarButton) {
            juntarButtonPressed();
        } else if (e.getSource() == initButton) {
            initButtonPressed();
        } else if (e.getSource() == listStreamButton) {
            listStreamButtonPressed();
        }
    }


    private void onExit(){
        TCPPacket packet = new TCPPacket(TCPPacket.Type.LEAVE);
        this.tcpManager.sendPacket(null, packet); // QUE METER EN GATEWAY ????
        System.exit(0);
    }

    private void playButtonPressed() {
        // Add your code here for when the Play button is pressed
        cTimer.start();
    }

    private void pauseButtonPressed() {
        // Add your code here for when the Pause button is pressed
        cTimer.stop();
    }

    private void juntarButtonPressed() {
        // Get the available streams and then show a window for the user to choose the stream
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

        if (selectedStream != null) {
            System.out.println("Stream escolhido: " + selectedStream);
            TCPPacket packet2 = new TCPPacket(TCPPacket.Type.REQUEST_STREAM);
            // Add your code here to handle the selected stream
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

        if (selectedStream != null) {
            System.out.println("Stream escolhido: " + selectedStream);
            TCPPacket packet2 = new TCPPacket(TCPPacket.Type.REQUEST_STREAM);
            // Add your code here to handle the selected stream
        }
    }

}
