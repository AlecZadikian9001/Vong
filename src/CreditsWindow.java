import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class CreditsWindow extends JFrame{
	public CreditsWindow(){
		super("Credits");
		this.setSize(600,300);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(290,290));
		panel.add(new JLabel("Created by Alec and Varun"));
		panel.add(new JLabel("as a computer science school project."));
		panel.add(new JLabel("Copywrite 2013 Duckimation Games, Ant Tantrum Records, and Fatalbert Productions."));
		panel.add(new JLabel("All music by V.G. Varun."));
		panel.setVisible(true);
		this.add(panel);
		this.setVisible(true);
		this.validate();
	}
}
