import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JPanel;

public class ScorePanel extends JPanel {
	private int score; //this current score
	private String name; //this name
	private int player; //this player
	private ModelInterface model;
	private Font font;

	public ScorePanel(String n, int p, ModelInterface m){ //a JPanel that shows the player's name and score
		super();	
		font = new Font("Courier New", 0, 30);
		
		name = n;
		model = m;
		score = 0;
		player = p;
		setBackground(Color.WHITE);
		setSize(450,90);
		setVisible(true);
	}

	public void processChanges() { //increments the score if necessary
		int win = model.getWinState();
		if (win==player){
			if (Controller.DEBUG) System.out.println("Updating a ScorePanel: "+name); 
			score++;
			model.resetWin();
			repaint();
		}
	}
	
	public void reset(){
		score = 0;
		repaint();
	}
	
	@Override
    public Dimension getPreferredSize() {
        return new Dimension(450, 90);
    };
    
    @Override
    public void paintComponent(Graphics g){
    	super.paintComponent(g);
    	g.setFont(font);
    	g.drawString(name, this.getWidth()/2 - g.getFontMetrics().stringWidth(name)/2,30);
    	g.drawString(""+score, this.getWidth()/2 - g.getFontMetrics().stringWidth(""+score)/2,80);
    }
}
