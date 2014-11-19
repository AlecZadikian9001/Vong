import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.Timer;

public class Controller implements ActionListener{	
	public static final boolean DEBUG = false; //for debugging purposes
	public static final boolean ALPHA = false; //for testing purposes

	public static final int HEIGHT = 700; //view height
	public static final int WIDTH = 1000; //view width
	public static final int BALL = 10; //ball dimension (since it's actually a square)
	public static final int PADDLE1X = 0;
	public static final int PADDLE2X = 990;
	public static final int PADDLEWIDTH = 10;
	public static final int PADDLEHEIGHT = 100;
	public static final double MAX_VELOCITY = 10; //max velocity for X or Y of ball
	public static final int SPEED = 15; //the tick speed in milliseconds
	private static final int PLAYER_SPEED = 5; //the speed at which the player can move his paddle

	private int playAs; //neither if 0, left if 1, right if 2, both if 3
	private View view;
	private Model model;
	private int speed; //how many milliseconds the timer waits between each tick
	private boolean isPlaying;
	private Timer timer; //for ticks
	private MicInput mic; private Thread micThread;

	public Controller(){
		// asks for settings
		// initializes view and model and everything, listens to view, initializes model, sets up observers
		String[] options =new String[4];
		options[0] = "Neither"; //both, playAs=0
		options[1] ="Left"; //volume, playAs=1
		options[2] ="Right";//pitch, playAs=2
		options[3] = "Both"; //both, playAs=3
		JOptionPane player = new JOptionPane("", JOptionPane.QUESTION_MESSAGE, 2, null, options, "Left");
		String whichP = null;
		whichP = (String) player.showInputDialog(player,"Do you want to play as the left panel (volume), the right panel (pitch), neither (AI vs AI), or both (left is volume and right is pitch)?", "Play As", JOptionPane.QUESTION_MESSAGE, null, options, "Left");	
		if (whichP == null) return;
		String cont = "volume";
		if (whichP.equalsIgnoreCase("Left")){
			playAs = 1;
			cont = "volume";
		}
		else if (whichP.equalsIgnoreCase("Right")){
			playAs = 2;
			cont = "pitch";
		}
		else if (whichP.equalsIgnoreCase("Neither")){
			playAs = 0;
			cont = "neither";
		}
		else{ 
			playAs=3;
			cont = "volume and pitch";

		}
		int difficulty = 6;
		if (playAs!=3){
			options = new String[16];
			for (int i = 0; i<options.length; i++){
				options[i] = (""+(1+i));
			}
			JOptionPane difficultyPane = new JOptionPane("", JOptionPane.QUESTION_MESSAGE, 2, null, options, "6");
			String diffresult = null;
			diffresult = (String) difficultyPane.showInputDialog(player,"AI strength (speed at which it can move its paddle)?", "AI Strength", JOptionPane.QUESTION_MESSAGE, null, options, "6");
			if (diffresult==null) return;
			difficulty = Integer.parseInt(diffresult);
			if (Controller.DEBUG)System.out.println("Difficulty "+diffresult+" selected."); 
		}
		String[] ok =new String[1];
		if (playAs!=0){
			options[0] ="OK";
			JOptionPane alright = new JOptionPane("", JOptionPane.INFORMATION_MESSAGE, 2);
			alright.showMessageDialog(alright,"You have chosen " + whichP + ".  If you raise the " + cont + " of your voice, the paddle will move up.  As you lower the " + cont + " of your voice, the paddle will move down. " +
					"\n\nIt is best to stay the same distance from your microphone; coming too close will make the sound detection unreliable. " +
					"\nFor pitch detection, making a louder sound will not make the paddle move any faster! All it will do is possibly overload the mic input.", "Directions",JOptionPane.INFORMATION_MESSAGE);
			mic = new MicInput(this);
			micThread = new Thread(mic); micThread.start();
			calibration(playAs);
		}
		
		int musicResult = displayMusicOptions();
		
		speed = Controller.SPEED;
		model = new Model(this,speed,difficulty,!(playAs==1 || playAs==3),!(playAs==2 || playAs==3));
		view = new View(model, this, musicResult);
		model.addObserver(view);
		view.initialize();

		//TODO also initializes mic input and background music

		timer = new Timer(speed, new ActionListener(){
			public void actionPerformed(ActionEvent e){
				try {
					model.tick();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} 
			}
		});
		//initializes starting positions and speed of everything, starts timer
		isPlaying = true;
		timer.start();
	}
	
	private void calibration(int playAs){ //walks the player through calibrating the microphone		
		
		JOptionPane calibrate = new JOptionPane("",JOptionPane.INFORMATION_MESSAGE, 2);

		if (playAs==1 || playAs==3){

			calibrate.showMessageDialog(calibrate, "Make no sound. Press OK.","Calibration",JOptionPane.INFORMATION_MESSAGE); 
			mic.calibrateWeak(); 
			calibrate.showMessageDialog(calibrate, "Emit the loudest sound you can make, and press OK while making that sound.","Calibration",JOptionPane.INFORMATION_MESSAGE);
			mic.calibrateStrong();  
			if (Controller.DEBUG) System.out.println("Controller just finished calibration of volume.");
		} 
		if (playAs==2 || playAs==3){
			try{
			calibrate.showMessageDialog(calibrate, "Emit the lowest sound you can make, and press OK while making that sound.","Calibration",JOptionPane.INFORMATION_MESSAGE);
			if (Controller.DEBUG) System.out.println("Controller calibrating calibrateLow on MicInput..."); 
			mic.calibrateLow();
			calibrate.showMessageDialog(calibrate, "Emit the highest sound you can make, and press OK while making that sound.","Calibration",JOptionPane.INFORMATION_MESSAGE);
			if (Controller.DEBUG) System.out.println("Controller calibrating calibrateHigh on MicInput..."); 
			mic.calibrateHigh();
			}
			catch (Exception e){
				System.out.println("Exception in controller for mic:");
				e.printStackTrace();
			}
			if (Controller.DEBUG) System.out.println("Controller just finished calibration of pitch.");
		}
		mic.finalizeCalibration();
		
	}

	private int displayMusicOptions(){ //asks the user for the music, returns an int ³1
		String[] options = new String[4];
		for (int i = 0; i<options.length; i++) options[i] = ""+(i+1);
		JOptionPane musicPane = new JOptionPane("", JOptionPane.QUESTION_MESSAGE, 2, null, options, "1");
		String musicResult = null;
		while (musicResult==null)
			musicResult = (String) musicPane.showInputDialog(musicPane,"Background music?", "Music", JOptionPane.QUESTION_MESSAGE, null, options, "1");
		return Integer.parseInt(musicResult);
	}

	private void reset(){
		timer.stop();
		model.reset();
		view.reset();
		timer.start();
	}

	public void actionPerformed(ActionEvent e) {
		if (Controller.DEBUG) System.out.println("Selected: " + e.getActionCommand());
		JMenuItem selectedItem = (JMenuItem) e.getSource();
		String text = selectedItem.getText();
		if (text.equals("Pause/Unpause")){
			if (isPlaying){
				isPlaying = false;
				timer.stop();
			}
			else{
				isPlaying = true;
				timer.start();
			}
		}
		else if (text.equals("Toggle music")) view.toggleMusic();
		else if (text.equals("Toggle sound effects")) view.toggleSoundFX();
		else if (text.equals("Change music...")){
			isPlaying = false;
			timer.stop();
			view.stopMusic();
			view.setMusic(displayMusicOptions());
			view.startMusic();
			timer.start();
		}
		else if (text.equals("New game")){

			if (Controller.DEBUG) System.out.println("Game resetting..."); 
			this.reset();

		}
		else if (text.equals("Recalibrate mic...")){
			if (playAs==0) return; //nothing to calibrate
			isPlaying = false;
			timer.stop();
			view.stopMusic();
			this.calibration(playAs);
			view.startMusic();
			timer.start();
		}
		else if (text.equals("Increase mic sensitivity"))
		{
			mic.increaseSens();
		}
		else if (text.equals("Decrease mic sensitivity"))
		{
			mic.decreaseSens();
		}
		else if (text.equals("Credits...")){
			CreditsWindow a = new CreditsWindow();	
		}
	}

	public double getPitchMove(){
		try {
			return mic.getPitchMove()*PLAYER_SPEED; //static
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public double getVolumeMove(){
		return mic.getVolumeMove()*PLAYER_SPEED*2.5; //static
	}
}