import java.awt.Color;
import java.awt.*;
import java.util.*;
import javax.swing.*;

public class GamePanel extends JPanel { //the panel that holds the paddles and ball
	private Polygon paddle1, paddle2;
	private ModelInterface model;
	private Polygon ball;
	private int[] ballX, ballY;
	//private static final Color[] colors = {new Color(255,0,200), Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.RED, Color.WHITE}; //colors used for what color the ball is

	private double speed; //the current speed (NOT velocity) of the ball, rounded up/down
	private Color color;
	private boolean checkSpeed; //if the color needs to be checked

	public GamePanel(ModelInterface m){ //creates the graphics		
		super();
		model = m;
		this.setPreferredSize(new Dimension(Controller.WIDTH,Controller.HEIGHT-120)); //?
		this.setBackground(Color.BLACK);
		this.setBorder(BorderFactory.createLineBorder(Color.WHITE));

		ballX = new int[4];
		ballY = new int[4];
		paddle1 = new Polygon();
		paddle2 = new Polygon();

		this.setVisible(true);
		this.validate();
	}

	public void processChanges() {
		ball = model.getBall();
		paddle1 = model.getPaddle1();
		paddle2 = model.getPaddle2();
		checkSpeed = model.checkSpeed();
		//	if (Controller.DEBUG && checkSpeed) System.out.println("GamePanel is checking ball speed."); 
		if (checkSpeed) speed = Math.sqrt((model.getBallDy())*(model.getBallDy()) + (model.getBallDx())*(model.getBallDx()));
		//repaint(); IF NO REFRESH TIMER (???), WILL REPAINT IN THIS METHOD TODO
	}

	@Override
	public void paintComponent(Graphics g){
		super.paintComponent(g);

		if (checkSpeed){
			color = new Color(Math.min(255, (int) (speed*30)), Math.min(255, (int) ((speed)*15)), Math.max(0, 255-((int)(speed*15))));
			g.setColor(color);
		}
		else g.setColor(color);

		try{
			g.fillPolygon(ball);
			g.fillPolygon(paddle1);
			g.fillPolygon(paddle2);
		}
		catch (Exception e){ System.out.println("Handled exception in GamePanel's paintComponent method"); }
	}
}
