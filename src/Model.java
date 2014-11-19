import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;

import javax.swing.Timer;

public class Model extends Observable implements ModelInterface {
	public static final int UNDECIDED = 0; //win states
	public static final int PLAYER1 = 1;
	public static final int PLAYER2 = 2;

	private Controller control;
	private AI ai1; //on left
	private AI ai2; //on right
	private volatile double leftPaddleVelocity; //-x for down, 0 for stay, x for up
	private volatile double rightPaddleVelocity; //-x for down, 0 for stay, x for up
	private double ballDy; //dy/dt of ball
	private double ballDx; //dx/dt of ball
	private int winState; //0 if undecided, 1 if player 1 wins, 2 if player 2 wins
	private boolean checkSpeed;
	private int difficulty; //speed at which the AI can move its paddle

	private Polygon ball,paddle1,paddle2; //for checking the dimensions of these panels
	private Rectangle panel;
	private int left, right, bottom, top; //boundaries of game window

	//Frequently accessed temporary variables, initialized just once to save CPU time
	private boolean ballDyPositive; //for collision calculations
	private double speedBoost; //for collision calculations
	private boolean changeCheckSpeed; //if there was a collision

	private boolean first; //if game has just started

	public Model(Controller c, int speed, int difficulty, boolean a1, boolean a2){ //initializes timer
		this.difficulty = difficulty;
		if (Controller.ALPHA) difficulty = 6; 
		control = c;
		panel = new Rectangle(Controller.WIDTH, Controller.HEIGHT-130);
		if (Controller.DEBUG)System.out.println("Model initialized, bounds are "+panel.width+"x"+panel.height); 
		setBall();
		winState = UNDECIDED;

		if (a1) ai1 = new AI(false, this);
		if (a2) ai2 = new AI(true, this);
		checkSpeed = true;
		first = true;

		//if (Controller.DEBUG) System.out.println("Model initialized with boundaries: "+Controller.WIDTH+" x "+Controller.HEIGHT); 
	}

	public void reset(){
		setBall();
		checkSpeed = true;
		first = true;
	}

	private void setBall(){ //sets up the default ball/paddle location and velocity
		int[][]xCoordinates = xCoordinates();
		int[][]yCoordinates = yCoordinates();

		ball = new Polygon(xCoordinates[0], yCoordinates[0], 4);
		paddle1 = new Polygon(xCoordinates[1], yCoordinates[1], 4);
		paddle2 = new Polygon(xCoordinates[2], yCoordinates[2], 4);
		ballDy = (((Math.random()*3))-1)*2;
		ballDx = (Math.random()*2)+3;
	}

	private int[][] xCoordinates(){ //returns an array of x coordinate arrays for Polygon initialization
		//in the outermost array: 0 = ball, 1 = paddle1, 2 = paddle2
		int[] ball = new int[4];
		int[] paddle1 = new int[4];
		int[] paddle2 = new int[4];

		ball [0] = 20+Controller.BALL;
		ball [1] = 20;
		ball [3] = 20+Controller.BALL;
		ball [2] = 20;

		paddle1 [0] = Controller.PADDLE1X;
		paddle1 [1] = Controller.PADDLE1X + Controller.PADDLEWIDTH;
		paddle1 [2] = Controller.PADDLE1X + Controller.PADDLEWIDTH;
		paddle1 [3] = Controller.PADDLE1X;

		paddle2 [0] = Controller.PADDLE2X;
		paddle2 [1] = Controller.PADDLE2X + Controller.PADDLEWIDTH;
		paddle2 [2] = Controller.PADDLE2X + Controller.PADDLEWIDTH;
		paddle2 [3] = Controller.PADDLE2X;

		int[][] ret = new int[3][];
		ret[0] = ball; ret[1] = paddle1; ret[2] = paddle2;
		return ret;
	}

	private int[][] yCoordinates(){//returns an array of y coordinate arrays for Polygon initialization
		//in the outermost array: 0 = ball, 1 = paddle1, 2 = paddle2
		int[] ball = new int[4];
		int[] paddle1 = new int[4];
		int[] paddle2 = new int[4];

		ball [0] = 10+Controller.BALL;
		ball [1] = 10+Controller.BALL;
		ball [2] = 10;
		ball [3] = 10;

		paddle1 [0] = 0;
		paddle1 [1] = 0;
		paddle1 [2] = Controller.PADDLEHEIGHT;
		paddle1 [3] = Controller.PADDLEHEIGHT;

		paddle2 [0] = 0;
		paddle2 [1] = 0;
		paddle2 [2] = Controller.PADDLEHEIGHT;
		paddle2 [3] = Controller.PADDLEHEIGHT;

		int[][] ret = new int[3][];
		ret[0] = ball; ret[1] = paddle1; ret[2] = paddle2;
		return ret;
	}


	public void tick()// throws Exception
	{ //perform actions (including notifyObservers) involved with rules, will be called frequently by the timer
		this.moveBall();
		this.movePaddles();
		checkForCollisions();
		checkForPaddleMoves();
		setChanged();
		notifyObservers();
		clearChanged();
		first = false;
		//try { Thread.sleep(250); }catch( Exception e ){}

	}

	private void checkForPaddleMoves()// throws Exception
	{ //sets the paddle movement velocities based on AI or human input
		if (ai1!=null){
			if (checkSpeed){
				leftPaddleVelocity =  ai1.getMove() - paddle1.getBounds().getCenterY();

			}
			else leftPaddleVelocity = ai1.getLastMove() - paddle1.getBounds().getCenterY();
			if (Math.abs(leftPaddleVelocity)>=difficulty){
				if (leftPaddleVelocity>0) leftPaddleVelocity = Math.min(difficulty, leftPaddleVelocity);
				else leftPaddleVelocity = -Math.min(difficulty, -leftPaddleVelocity);
			}
		}
		else{
			leftPaddleVelocity = control.getVolumeMove();
		}
		if (ai2!=null){
			if (checkSpeed) rightPaddleVelocity = ai2.getMove() - paddle2.getBounds().getCenterY();
			else rightPaddleVelocity = ai2.getLastMove() - paddle2.getBounds().getCenterY();
			if (Math.abs(rightPaddleVelocity)>=difficulty){
				if (rightPaddleVelocity>0) rightPaddleVelocity = Math.min(difficulty, rightPaddleVelocity);
				else rightPaddleVelocity = -Math.min(difficulty, -rightPaddleVelocity);
			}
		}
		else{
			rightPaddleVelocity = control.getPitchMove();
			//try { Thread.sleep(250); }catch( Exception e ){}

		}		
	}

	public void moveBall(){ //moves ball according to dy and dx 
		if (Math.abs(ballDx)>=Controller.MAX_VELOCITY){ //speed limits
			if (ballDx>0) ballDx = Controller.MAX_VELOCITY;
			else ballDx = -Controller.MAX_VELOCITY;
		}
		if (Math.abs(ballDy)>=Controller.MAX_VELOCITY){ //speed limits
			if (ballDy>0) ballDy = Controller.MAX_VELOCITY;
			else ballDy = -Controller.MAX_VELOCITY;
		}
		ball.translate((int)Math.round(ballDx),(int)Math.round(ballDy));
	}

	public void movePaddles(){ //moves paddles according to input values
		paddle1.translate(0,(int) Math.round(leftPaddleVelocity));
		paddle2.translate(0,(int) Math.round(rightPaddleVelocity));
		if (!panel.contains(paddle1.getBounds())){
			boolean above = false;
			if (paddle1.getBounds().y<panel.y) above = true;
			if (above) paddle1.translate(0, panel.y - paddle1.getBounds().y);
			else paddle1.translate(0, panel.height - (paddle1.getBounds().y + paddle1.getBounds().height));
		}
		if (!panel.contains(paddle2.getBounds())){ 
			boolean above = false;
			if (paddle2.getBounds().y<panel.y) above = true;
			if (above) paddle2.translate(0, panel.y - paddle2.getBounds().y);
			else paddle2.translate(0, panel.height - (paddle2.getBounds().y + paddle2.getBounds().height));
		}
	}

	public void setLeftPaddle(int a){ leftPaddleVelocity = a; }
	public void setRightPaddle(int a){ rightPaddleVelocity = a; }

	private void checkForCollisions(){
		//act accordingly if the ball or paddle has collided with something, mutating change
		//if the collision is a win (like if the ball hits the edge of the screen), detects it
		//change the velocity of the ball depending on which part of the paddle it hits
		left = panel.x;
		right = panel.x+panel.width;
		top = panel.y;
		bottom = panel.y+panel.height;
		changeCheckSpeed = false;

		if (ball.getBounds().getCenterX()>=right){
			winState = (Model.PLAYER1);
			if (Controller.DEBUG)System.out.println("Player 1 win detected."); 
			notifyObservers();
			setBall();
			checkSpeed = true;
			changeCheckSpeed = true;
		}
		else if (ball.getBounds().getCenterX()<=left){
			winState = (Model.PLAYER2);
			if (Controller.DEBUG)System.out.println("Player 2 win detected."); 
			notifyObservers();
			setBall();
			checkSpeed = true;
			changeCheckSpeed = true;
		}
		if (paddle2.intersects(ball.getBounds())){
			ball.getBounds().x = (int) Math.round(right-ballDx-60);
			ballDx = (-ballDx);
			speedBoost = Math.min(1, .5*Math.abs(ball.getBounds().getCenterY() - paddle2.getBounds().getCenterY()));
			if (Controller.DEBUG) System.out.println("Speed boosted: "+speedBoost);
			ballDx -= speedBoost;
			ballDyPositive = (ballDy>0);
			if (ballDyPositive) ballDy += speedBoost;
			else ballDy -= speedBoost;
			if (Controller.DEBUG)System.out.println("Collision with right paddle detected."); 
			checkSpeed = true;
			changeCheckSpeed = true;
		}
		else if (paddle1.intersects(ball.getBounds())){
			ball.getBounds().x = (int) Math.round(left+ballDx+60);
			ballDx = (-ballDx);
			speedBoost = Math.min(1, .5*Math.abs(ball.getBounds().getCenterY() - paddle1.getBounds().getCenterY()));
			if (Controller.DEBUG) System.out.println("Speed boosted: "+speedBoost);
			ballDx += speedBoost;
			ballDyPositive = (ballDy>0);
			if (ballDyPositive) ballDy += speedBoost;
			else ballDy -= speedBoost;
			if (Controller.DEBUG)System.out.println("Collision with left paddle detected."); 
			checkSpeed = true;
			changeCheckSpeed = true;
		}
		if (ball.getBounds().getCenterY()<=top){
			if (Controller.DEBUG)System.out.println("Collision with top detected.");
			ball.getBounds().y = (top+1);
			ballDy = (-ballDy);
			if (ballDy<1) ballDy = 1;
			checkSpeed = true;
			changeCheckSpeed = true;
		}
		else if (ball.getBounds().getCenterY()>=bottom){
			if (Controller.DEBUG)System.out.println("Collision with bottom detected.");
			ball.getBounds().y = (bottom-1);
			ballDy = (-ballDy);
			if (ballDy>-1) ballDy = -1;
			checkSpeed = true;
			changeCheckSpeed = true;
		}
		if (!first && changeCheckSpeed != true) checkSpeed = false;
		if (Controller.DEBUG && checkSpeed) System.out.println("CheckSpeed: "+checkSpeed);
		//	if (Controller.ALPHA) checkSpeed = true; 
	}

	public Rectangle getBounds(){
		return panel;
	}

	//methods in order for this to implement interface ModelInterface:

	public Polygon getPaddle1() {
		return paddle1;
	}

	public Polygon getPaddle2() {
		return paddle2;
	}

	public Polygon getBall() {
		return ball;
	}

	public int getBallXCoord() {
		return ball.getBounds().x;
	}

	public int getBallYCoord() {
		return ball.getBounds().y;
	}

	public double getBallDy() {
		return ballDy;
	}

	public double getBallDx() {
		return ballDx;
	}

	public int getWinState(){
		return winState;
	}

	public boolean checkSpeed(){
		return checkSpeed;
	}

	public void resetWin(){
		winState = 0;
	}
}
