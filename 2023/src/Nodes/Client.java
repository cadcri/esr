package Nodes;

import TCP.*;
import UDP.*;
import Structs.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.*;
import java.util.Scanner;
import java.io.*;
import java.lang.Thread;
import java.lang.System;
import java.util.Base64;
import java.util.HashMap;


public class Client extends JFrame implements ActionListener {
    private OutputStream os;
    private PrintWriter pw;
    private BufferedReader in;
    private Node node;

    private UDPManager udpManager;
    private TCPManager tcpManager;

    private Boolean connected = false;

    // CELSO ADDED
    JLabel iconLabel;
    Timer cTimer;
    byte[] cBuf;

    public Client(Node node) {
        super("Clientes");
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
    

        try {
            Scanner sc = new Scanner(System.in);
            String option = "";
            while (!option.equals("4")) {
                System.out.println("What do you want to do?");
                System.out.println("1. Join network with default gateway(s): " + this.node.getGateways().toString());
                System.out.println("2. Start Streaming");
                System.out.println("3. List routes");
                System.out.println("4. Exit");

                option = sc.nextLine();

                switch (option) {
                    case "1":
                        System.out.println("Joining network");
                        for (String gateway : this.node.getGateways()) {
                            System.out.println("Sending join message to: " + gateway);
                            //this.tcpManager.createWriting(gateway);
                            TCPPacket packet = new TCPPacket(TCPPacket.Type.JOIN);
                            packet.addToOutgoingPath(gateway);
                            //packet.addToIncomingPath(gateway);
                            this.tcpManager.sendPacket(gateway, packet);
                        }
                        break;
                    
                    case "2":
                        System.out.println("Starting streaming");
                        System.out.println("Enter the full path of the file you want to stream: ");
                        String path = sc.nextLine();
                        TCPPacket packet = new TCPPacket(TCPPacket.Type.STREAM);
                        packet.setPathToFile(path);
                        this.tcpManager.sendPacket("",packet);
                        break;

                    case "3":
                        this.tcpManager.listRoutes();
                        break;
                }
            }
        } finally {
            try {
                //udpManager.leave();
                //tcpManager.leave();
                return;
            } catch (Exception e) {
                System.out.println("Error closing sockets");
            }
        }

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        JButton setupButton = new JButton("Setup");
        JButton playButton = new JButton("Play");
        JButton pauseButton = new JButton("Pause");
        JButton tearButton = new JButton("Teardown");
        JPanel mainPanel = new JPanel();
        JPanel buttonPanel = new JPanel();
        iconLabel = new JLabel();
        buttonPanel.setLayout(new GridLayout(1, 0));
        buttonPanel.add(setupButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(tearButton);
        iconLabel.setIcon(null);
        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);
        iconLabel.setBounds(0, 0, 380, 280);
        buttonPanel.setBounds(0, 280, 380, 50);
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        setSize(new Dimension(390, 370));
        setVisible(true);
        //////////////////////////// STOP GUI

        playButton.addActionListener(e -> {
            cTimer.start();
        });
        tearButton.addActionListener(e -> {
            cTimer.stop();
            System.exit(0);
        });
        cTimer = new Timer(20, this);
        cTimer.setInitialDelay(0);
        cTimer.setCoalesce(true);
        cBuf = new byte[15000];
        cTimer.start(); //// TOREMOVE
    }

    public String encode(String path){
        byte[] bytes = Base64.getEncoder().encode(path.getBytes());
        return new String(bytes);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DatagramPacket rcvdp = new DatagramPacket(cBuf, cBuf.length);

        try {
            int video_byte_size;

            // utilisar aqui o .receive do datagram socket, depois :
            // meter en cBuf o conteudo da imagem
            // meter em video_byte_size o tamanho de cBuf

            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Image image = toolkit.createImage(cBuf, 0, video_byte_size);

            ImageIcon icon = new ImageIcon(image);
            iconLabel.setIcon(icon);

        } catch (InterruptedIOException iioe) {
            System.out.println("Nothing to read");
        } catch (IOException ioe) {
            System.out.println("Exception caught: " + ioe);
        }
    }
}
