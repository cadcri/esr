package GUI;

import UDP.RTPPacket;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class ClienteGUI extends JFrame implements ActionListener {

    JLabel iconLabel;
    Timer cTimer;
    byte[] cBuf;

    byte[] buff;
    DatagramPacket rcvdp;
    DatagramSocket rtpSocket;

    public ClienteGUI(int streamSelected){
        JButton playButton = new JButton("Play");
        JButton pauseButton = new JButton("Pause");
        JButton juntarButton = new JButton("Juntar Stream");
        JButton initButton = new JButton("Iniciar Stream");
        JPanel mainPanel = new JPanel();
        JPanel buttonPanel = new JPanel();
        iconLabel = new JLabel();
        buttonPanel.setLayout(new GridLayout(1, 0));
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
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
        //////////////////////////// STOP GUI

        playButton.addActionListener(e -> cTimer.start());
        pauseButton.addActionListener(e -> {
            cTimer.stop();
            System.exit(0);
        });
        cTimer = new Timer(20, this);
        cTimer.setInitialDelay(0);
        cTimer.setCoalesce(true);
        cBuf = new byte[15000];
        cTimer.start();

        // NEW

        /// TESTE
// criacao do socket para receber com uma porta de 4555

        try {
            rtpSocket = new DatagramSocket(4555);
        } catch (SocketException ee) {
            throw new RuntimeException(ee);
        }

        // criacao do packet que vai ser povoado
        buff  = new byte[150000];
        rcvdp = new DatagramPacket(buff, buff.length);
    }

    // chamada muitas vezes
    @Override
    public void actionPerformed(ActionEvent e) {
        int video_byte_size = 10;

        // utilisar aqui o .receive do datagram socket, depois :
        // meter en cBuf o conteudo da imagem
        // meter em video_byte_size o tamanho de cBuf

            // rececao do packet e creacao do RTPPacket com os dados recebidos
            try {
                rtpSocket.receive(rcvdp);
            } catch (IOException ee) {
                throw new RuntimeException(ee);
            }
            RTPPacket packet = new RTPPacket(rcvdp.getData(), rcvdp.getLength());
            System.out.println("Receved packet nb : " + packet.getsequencenumber());

            packet.getpayload(buff);
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Image image = toolkit.createImage(buff, 0, packet.getpayload_length());
            ImageIcon icon = new ImageIcon(image);
            iconLabel.setIcon(icon);



        ///




    }

}