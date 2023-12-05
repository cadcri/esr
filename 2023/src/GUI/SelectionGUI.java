package GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SelectionGUI extends JFrame implements ActionListener {

    public SelectionGUI(){
        JPanel selectionPanel = new JPanel();

        int nbOfStreams = 3; // REPLACE WITH NUMBER OF STREAMS
        for (int i = 0 ; i < nbOfStreams; i++){
            JButton selectionStreamButton = new JButton("Stream "+i); // REPLACE WITH STREAM NAME
            int finalI = i;
            selectionStreamButton.addActionListener(a -> {
                new ClienteGUI(finalI);
            });
            selectionPanel.add(selectionStreamButton);
        }

        getContentPane().add(selectionPanel, BorderLayout.CENTER);

        setSize(new Dimension(390, 370));
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    // FOR TESTING
    public static void main(String[] args){
        new SelectionGUI();
    }
}
