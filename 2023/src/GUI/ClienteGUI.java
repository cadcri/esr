package GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClienteGUI extends JFrame implements ActionListener {

    JLabel iconLabel;
    Timer cTimer;
    byte[] cBuf;

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
    }

    // chamada muitas vezes
    @Override
    public void actionPerformed(ActionEvent e) {
        int video_byte_size = 10;

        // utilisar aqui o .receive do datagram socket, depois :
        // meter en cBuf o conteudo da imagem
        // meter em video_byte_size o tamanho de cBuf

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image image = toolkit.createImage(cBuf, 0, video_byte_size);

        ImageIcon icon = new ImageIcon(image);
        iconLabel.setIcon(icon);

    }

}