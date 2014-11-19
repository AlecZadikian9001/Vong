import java.io.*;
import java.net.URL;

import javax.sound.sampled.*;
public class Sound implements Runnable
{
	private String fileLocation;
	private Thread t;
	private volatile boolean isPlaying;
	private AudioInputStream audioInputStream;
	private SourceDataLine  line;
	
	private boolean loop;

	public Sound(String path, boolean l){
		fileLocation = path;
		loop = l;
	}
	
	public Sound(String path){ this(path, false); }

	public void play() //called by other classes
	{
		isPlaying = true;
		t = new Thread(this);
		t.start();
	}

	public void stopPlaying(){ //called by other classes
		isPlaying = false;
		line.stop();
		line.flush();
	}

	public void run ()
	{
		playSound(fileLocation);
		while (isPlaying && loop){
			playSound(fileLocation);
		}
	}
	
	private void playSound(String fileName)
	{
		File soundFile = new File("Sounds/"+fileName);
		audioInputStream = null;
		try
		{
			audioInputStream = AudioSystem.getAudioInputStream(soundFile);
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		} 
		AudioFormat     audioFormat = audioInputStream.getFormat();

		DataLine.Info   info = new DataLine.Info(SourceDataLine.class,audioFormat);
		try
		{
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(audioFormat);
		}
		catch (LineUnavailableException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		line.start();
		int     nBytesRead = 0;
		byte[]  abData = new byte[128000];
		while (nBytesRead != -1 && isPlaying)
		{
			try
			{
				nBytesRead = audioInputStream.read(abData, 0, abData.length);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			if (nBytesRead >= 0)
			{
				int     nBytesWritten = line.write(abData, 0, nBytesRead);
			}
		}
		line.drain();
		line.close();
	}
}