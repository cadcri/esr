package src;

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

public class Cliente extends JFrame implements ActionListener {

    static InetAddress ServerIP;

    public static void main(String[] argv) {
        try {
             ServerIP = InetAddress.getByName("10.0.18.10");
            //ServerIP = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        new Cliente(argv[0], 25000);
    }

    JLabel iconLabel;
    DatagramSocket RTPsocket;

    Timer cTimer;
    byte[] cBuf;

    DatagramSocket SendSocket;
    String myIP;

    public Cliente(String myIP, int port) {
        super("Clientes");
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
            sendRequestWantStream(false);
            cTimer.stop();
            System.exit(0);
        });
        cTimer = new Timer(20, this);
        cTimer.setInitialDelay(0);
        cTimer.setCoalesce(true);
        cBuf = new byte[15000];
        try {
            RTPsocket = new DatagramSocket(port);
            RTPsocket.setSoTimeout(5000);
        } catch (SocketException e) {
            System.out.println("src.Cliente: erro no socket: " + e.getMessage());
        }
        cTimer.start(); //// TOREMOVE

        try {
            SendSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        this. myIP = myIP;

        sendRequestWantStream(true);
    }

    private void sendRequestWantStream(boolean want){
        CelsoPacket packet;
        if (want)
            packet = new CelsoPacket((byte) 0x0, null, 0, myIP.getBytes(),myIP.getBytes().length);
        else
            packet = new CelsoPacket((byte) 0x1, null, 0, myIP.getBytes(),myIP.getBytes().length);
        int size = packet.getPacketBytes(cBuf);
        DatagramPacket senddp = new DatagramPacket(cBuf, size, ServerIP, 25000);
        try {
            SendSocket.send(senddp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void actionPerformed(ActionEvent e) {
        DatagramPacket rcvdp = new DatagramPacket(cBuf, cBuf.length);

        try {
            RTPsocket.receive(rcvdp);
            CelsoPacket packet = new CelsoPacket(rcvdp.getData(), rcvdp.getLength());

            int video_byte_size = packet.getVideoBytes(cBuf);

            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Image image = toolkit.createImage(cBuf, 0, video_byte_size);

            ImageIcon icon = new ImageIcon(image);
            iconLabel.setIcon(icon);


            int ip_byte_size = packet.getIPBytes(cBuf);
            byte[] string = new byte[ip_byte_size];
            for (int i = 0; i < ip_byte_size; i++){
                string[i] = cBuf[i];
            }

        } catch (InterruptedIOException iioe) {
            System.out.println("Nothing to read");
        } catch (IOException ioe) {
            System.out.println("Exception caught: " + ioe);
        }
    }

}