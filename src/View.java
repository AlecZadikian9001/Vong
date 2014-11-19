import java.awt.*;
import java.io.InputStream;
import java.util.*;
import javax.swing.*;

public class View extends JFrame implements Observer{

	private GamePanel gpanel;
	private ModelInterface model;
	private ScorePanel score1;
	private ScorePanel score2;

	private Sound pong;
	private Sound music;
	private boolean sfxEnabled;
	private boolean musicEnabled;
	private int musicNumber;

	public View(ModelInterface m, Controller c, int musicChoice) { //holds menus, panel with game on it, and scoreboards
		super();
		musicNumber = musicChoice;
		model = m;
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		if (System.getProperty("os.name").contains("Windows")) //accounts for GUI differences in Windows
			this.setSize(Controller.WIDTH+10,Controller.HEIGHT+29);
		else this.setSize(Controller.WIDTH,Controller.HEIGHT);
		
		this.setBackground(Color.DARK_GRAY);
		this.setLayout(new BorderLayout(2,2));
		gpanel = new GamePanel(m);

		pong = new Sound("pong.wav");
		music = new Sound("music"+musicNumber+".wav", true);

		JMenuBar menus = new JMenuBar();
		JMenu sound = new JMenu("Sound");
		JMenuItem toggleMusic = new JMenuItem("Toggle music");
		JMenuItem toggleSound = new JMenuItem("Toggle sound effects");
		JMenuItem changeMusic = new JMenuItem("Change music...");
		sound.add(toggleMusic); sound.add(toggleSound); sound.add(changeMusic);
		JMenu game = new JMenu("Game");
		JMenuItem newGame = new JMenuItem("New game");
		JMenuItem pause = new JMenuItem("Pause/Unpause");
		JMenuItem recalibrate = new JMenuItem("Recalibrate mic...");
		JMenuItem increaseSen= new JMenuItem ("Increase mic sensitivity");
		JMenuItem decreaseSen= new JMenuItem ("Decrease mic sensitivity");
		game.add(newGame); game.add(pause); sound.add(recalibrate);
		JMenu about = new JMenu("About");
		JMenuItem credits = new JMenuItem("Credits...");
		about.add(credits);
		sound.add(increaseSen);
		sound.add(decreaseSen);
		toggleMusic.addActionListener(c);
		toggleSound.addActionListener(c);
		changeMusic.addActionListener(c);
		newGame.addActionListener(c);
		pause.addActionListener(c);
		recalibrate.addActionListener(c);
		credits.addActionListener(c);
		menus.add(game); menus.add(sound); menus.add(about);
		menus.validate();
		this.setJMenuBar(menus);

		this.setResizable(false);
		this.validate();
		this.setVisible(true);

		sfxEnabled = true; 
		musicEnabled = true;
		music.play();
	}

	public void initialize(){ //called by controller at the end
		if (Controller.DEBUG) System.out.println("View initialized"); 
		JPanel scores = new JPanel();
		scores.setSize(Controller.WIDTH,100);
		scores.setBackground(Color.BLACK);
		score1 = new ScorePanel("Left", 1, model);
		score2 = new ScorePanel("Right", 2, model);
		scores.add(score1); scores.add(score2);
		scores.setVisible(true);
		scores.validate();
		this.add(scores, BorderLayout.SOUTH);
		this.add(gpanel, BorderLayout.CENTER);
		this.validate();
		this.setVisible(true);
		this.repaint();
	}

	public GamePanel getPanel(){ return gpanel; }

	public void update(Observable o, Object arg) {
		if (sfxEnabled && model.checkSpeed()) pong.play();
		gpanel.processChanges();
		score1.processChanges();
		score2.processChanges();
		repaint();
	}

	public void toggleMusic(){
		if (Controller.DEBUG) System.out.println("Music toggled in View (toggleMusic())"); 
		if (musicEnabled) stopMusic();
		else startMusic();
		musicEnabled = !musicEnabled;
	}

	public void setMusic(int a){
		musicNumber = a;
		music = new Sound("music"+musicNumber+".wav", true);
	}

	public void stopMusic(){
		music.stopPlaying();
	}

	public void startMusic(){
		music.play();
	}

	public void toggleSoundFX(){
		if (Controller.DEBUG) System.out.println("SFX toggled in View (toggleSoundFX())");
		sfxEnabled = !sfxEnabled;
	}

	public void reset(){
		score1.reset();
		score2.reset();
	}
}
