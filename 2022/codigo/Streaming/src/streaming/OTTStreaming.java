package streaming;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.InterruptedIOException;
import javax.swing.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import overlay.TCP.TCPCommunicator;
import streaming.UDP.RTPPacket;

public class OTTStreaming {
    private JFrame frame;
    private JButton runButton;
    private JButton teardownButton;
    private JPanel mainPanel;
    private JPanel buttonPanel;
    private JLabel iconLabel;
    private ImageIcon icon;

    private DatagramPacket rcvdp;
    private DatagramSocket RTPsocket;
    private InetAddress connectorIP;
    public static final int RTP_PORT = 25001;

    private Timer cTimer;
    private byte[] cBuf;
    private int cBufLength = 15000;
    private boolean running;
    private int errorI = 0;

    public OTTStreaming(String ipName){
        try{
            connectorIP = InetAddress.getByName(ipName);
            frame = new JFrame("STREAM CLIENT");
            runButton = new JButton("RUN");
            teardownButton = new JButton("TEARDOWN");
            mainPanel = new JPanel();
            buttonPanel = new JPanel();
            iconLabel = new JLabel();

            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e){
                    System.exit(0);
                }
            });

            buttonPanel.setLayout(new GridLayout(1, 0));
            buttonPanel.add(runButton);
            buttonPanel.add(teardownButton);

            runButton.addActionListener(new RunButtonListener());
            teardownButton.addActionListener(new TeardownButtonListener());

            iconLabel.setIcon(null);
            mainPanel.setLayout(null);
            mainPanel.add(iconLabel);
            mainPanel.add(buttonPanel);
            iconLabel.setBounds(0, 0, 380, 280);
            buttonPanel.setBounds(0, 280, 380, 50);

            frame.getContentPane().add(mainPanel, BorderLayout.CENTER);
            frame.setSize(new Dimension(390, 370));
            frame.setVisible(true);

            cTimer = new Timer(20, new ClientTimerListener());
            cTimer.setInitialDelay(0);
            cTimer.setCoalesce(true);
            cBuf = new byte[cBufLength];

            RTPsocket = new DatagramSocket(RTP_PORT);
            RTPsocket.setSoTimeout(3000);
        }
        catch(Exception e){}
    }

    public static void main(String[] args){
        if (args.length == 1)
            new OTTStreaming(args[0]);
    }

    class RunButtonListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
            System.out.println("PLAY button pressed!");

            TCPCommunicator client;
            client = new TCPCommunicator(null, connectorIP, TCPCommunicator.OPEN_STREAM_CLIENT);
            client.run();
            cTimer.start();
            running = true;
        }
    }

    class TeardownButtonListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
            System.out.println("TEARDOWN button pressed!");

            TCPCommunicator client;
            client = new TCPCommunicator(null, connectorIP, TCPCommunicator.CANCEL_STREAM_CLIENT);
            client.run();
            running = false;
        }
    }

    class ClientTimerListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
            rcvdp = new DatagramPacket(cBuf, cBufLength);

            if (running){
            try{
                RTPsocket.receive(rcvdp);

                RTPPacket rtp_packet = new RTPPacket(rcvdp.getData(), rcvdp.getLength());

                int payload_length = rtp_packet.getpayload_length();
                byte [] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);

                Toolkit toolkit = Toolkit.getDefaultToolkit();
                Image image = toolkit.createImage(payload, 0, payload_length);

                icon = new ImageIcon(image);
                iconLabel.setIcon(icon);
            }
            catch(InterruptedIOException iioe){
                if (errorI == 0){
                    TCPCommunicator client;
                    client = new TCPCommunicator(null, connectorIP, TCPCommunicator.STREAM_BROKEN_CLIENT);
                    client.run();
                    System.out.println("Nothing to read");
                }
                // depois de enviar um aviso que a stream está estragado, só o volta a fazer 15 segundos depois
                errorI ++;
                if (errorI == 5)
                    errorI = 0;
            }
            catch(IOException ioe){
                System.out.println("Exception caught: " + ioe);
            }
            }
        }
    }
}
