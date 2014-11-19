import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;

public class MicInput extends Thread
{


	private Controller control;
	private volatile double volume; //updated with each tick by controller
	private volatile double[] frequency; //updated with each tick by controller
	//private volatile boolean update; //if true, update volume and frequency
	private boolean isFreq;
	private double lowFreq;
	private double hiFreq;
	private double hiVol;
	private double lowVol;
	private boolean isVol;

	final static float MAX_8_BITS_SIGNED = Byte.MAX_VALUE;
	final static float MAX_8_BITS_UNSIGNED = 0xff;
	final static float MAX_16_BITS_SIGNED = Short.MAX_VALUE;
	final static float MAX_16_BITS_UNSIGNED = 0xffff;
	private float level, sampleRate;
	private TargetDataLine targetDataLine;
	private AudioFormat format;
	byte[] buffer, volBuff;
	private int bufferSize;

	private static final int BUFFER_SIZE = 8; /*
	The smoothness of the control. Frequency detections are passed through an array and the average taken when calculating moves.
	A larger buffer takes more processing and makes the paddle have a more gradual acceleration and smoother movement in general.
	 */

	public MicInput(Controller c)
	{
		super();
		//lowVol = 0; hiVol = 40; //static

		frequency = new double[BUFFER_SIZE];
		//volume = new double[BUFFER_SIZE];

		control = c;
		if (Controller.DEBUG) System.out.println("MicInput initialized");
		isFreq = false;
		isVol = false;
		sampleRate = 44100;
		int sampleSizeInBits = 16;
		int channels = 1;
		boolean signed = true;
		boolean bigEndian = false;
		format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
		DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);

		buffer = new byte[2*1200];
		volBuff = new byte[2*1200];

		bufferSize = (int) format.getSampleRate()
				* format.getFrameSize();



		try {
			targetDataLine = (TargetDataLine)AudioSystem.getLine(dataLineInfo);
			targetDataLine.open(format, (int)sampleRate);
			targetDataLine.start();
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();



		}
		targetDataLine.read(buffer, 0, buffer.length);

		if (System.getProperty("os.name").contains("Windows")){
			if (Controller.DEBUG) System.out.println("Running on Windows, assuming the mic needs 5 notches of increased sensitivity.");
			for (int x=0; x<5; x++)
			{
				this.increaseSens();
			}
		}
		// read about a second at a time
	}
	
	public void increaseSens()
	{
		if (isFreq)
		{
			hiFreq*=1.02;
			lowFreq*=0.98;
		}
		if (isVol)
		{
			hiVol*=1.02;
			lowVol*=0.98;
		}
		
	}

	
	public void decreaseSens()
	{
		if (isFreq)
		{
			hiFreq*=0.98;
			lowFreq*=1.02;
		}
		if (isVol)
		{
			hiVol*=0.98;
			lowVol*=1.02;
		}
		
	}
	
	@Override
	public void run() 
	{ 
		if (Controller.DEBUG)System.out.println("Run called");
		while (true)
		{
			if (Controller.DEBUG){ System.out.println("Running thread."); }

			if (isFreq)
			{

				try {
					for (int i = frequency.length-1; i>0; i--){
						frequency[i] = frequency[i-1];
					}
					frequency[0] = this.getFrequency();
				} catch (Exception e) {

					System.out.println("problem with run");
					e.printStackTrace();
				}

			}

		/*	if (isVol)
			{
				volume = this.getVolume();



				//	if (Controller.DEBUG)	System.out.println("isVol");
				/* try {
					if (Controller.DEBUG) System.out.println("Updating volume buffer...");
					for (int i = volume.length-1; i>0; i--){
					//	System.out.println(".");
						volume[i] = volume[i-1];
					}
					if (Controller.DEBUG) System.out.println("About to call getVolume()");
					volume[0] = this.getVolume();
					System.out.println(volume[0]);
				} catch (Exception e) { e.printStackTrace(); }*/
//			} 


			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


		private double getFrequency() throws Exception 
		{
			isFreq = true;
			double currentFreq;
			currentFreq = 0.0;


			int[] a = new int[buffer.length/2];

			int n = -1;
			while ( (n = targetDataLine.read(buffer, 0, buffer.length)) > 0 ) 
			{

				for ( int i = 0; i < n; i+=2 ) 
				{
					int value = (short)((buffer[i]&0xFF) | ((buffer[i+1]&0xFF) << 8));
					a[i >> 1] = value;
					currentFreq = value;
				}

				double prevDiff = 0;
				double prevDx = 0;
				double maxDiff = 0;

				int sampleLen = 0;



				int len = a.length/2;

				for ( int i = 0; i < len; i++ ) 
				{
					double diff = 0;
					for ( int j = 0; j < len; j++ ) 
					{
						diff += Math.abs(a[j]-a[i+j]);
					}

					double dx = prevDiff-diff;

					// change of sign in dx
					if ( dx < 0 && prevDx > 0 ) 
					{
						// only look for troughs that drop to less than 10% of peak
						if ( diff < (0.1*maxDiff) ) 
						{

							if ( sampleLen == 0 ) 
							{

								sampleLen=i-1;

								//return 0.0; //KILLL THISS
							}
						}
					}

					prevDx = dx;
					prevDiff=diff;
					maxDiff=Math.max(diff,maxDiff);
				}

				if ( sampleLen > 0 ) 
				{
					double frequency = (format.getSampleRate()/sampleLen);
					if (frequency!=0)
					{
						return frequency;
					}

				}
				else 
				{
					if (hiFreq>0.0)
						return (hiFreq+lowFreq)/2;
				}

			}


			return currentFreq;

		} 

		public double getVolumeMove()
		{
			double middle = ((lowVol + hiVol)/2); if (Controller.DEBUG) System.out.println("Middle: "+middle);
			double radius = middle-lowVol;
			//volume = this.getVolume(); //consider making loop & getting frequency across interval
			double thing;
			thing =  ((middle-this.getVolume())/radius);

			if (Controller.DEBUG) System.out.println("Volume move: "+thing);
			return thing;
		}

		public double getPitchMove() //throws Exception
		{
			double middle = ((lowFreq + hiFreq)/2);
			double radius = middle-lowFreq;

			double thing;
			thing =  ((averageFrequency()-middle)/radius);
			return thing;
		}

		private double averageFrequency(){ //returns the average value of the frequency buffer array
			double sum = 0;
			for (double a : frequency){
				sum+=a;
			}
			return sum/(frequency.length);
		}

		//	private double averageVolume(){ //returns the average value of the frequency buffer array
		//		double sum = 0;
		//		for (double a : volume){
		//			sum+=a;
		//		}
		//		return sum/(volume.length);
		//	}


		//Calibration methods called at start of game...
		public void calibrateLow() //throws Exception//pitch
		{
			isFreq = true;
			double lowPitch = 0.0;
			for (int x=0; x<3; x++)
			{
				while (frequency[0] == 0.0)
				{
					lowPitch+=frequency[0];

				} 
				try {
					frequency[0] = this.getFrequency();
				} catch (Exception e) {

					e.printStackTrace();
				}
				lowPitch+=frequency[0];
			}
			lowFreq = lowPitch/3.0;
			//try { Thread.sleep(250); }catch( Exception e ){}
		}

		public void calibrateHigh() throws Exception
		{
			isFreq = true;
			double hiPitch = 0.0;
			for (int x=0; x<3; x++)
			{
				while (frequency[0] == 0.0)
				{
					hiPitch+=frequency[0];

				}
				try {
					frequency[0] = this.getFrequency();
				} catch (Exception e) {

					e.printStackTrace();
				}
				hiPitch+=frequency[0];
			}
			hiFreq = hiPitch/3.0;
			//try { Thread.sleep(250); }catch( Exception e ){}
		}

		public void calibrateWeak() //volume
		{
			isVol = true;
			double loVol = 0.0;
			for (int x=0; x<3; x++)
			{
				//  while (volume[0]>30.0)
				loVol+=volume;
				try {
					volume = this.getVolume();
				} catch (Exception e) {

					e.printStackTrace();
				}
			}


			lowVol = loVol/3.0;
			if (Controller.DEBUG) System.out.println("Lowvol: "+lowVol);
		}

		public void calibrateStrong()
		{
			isVol = true;
			double highVol = 0.0;
			for (int x=0; x<3; x++)
			{
				//while (volume == 0.0 || volume==lowVol)
				//{
					//highVol+=volume;

				//}
				try {
					volume = this.getVolume();
					highVol+=volume;
				} catch (Exception e) {

					e.printStackTrace();
				}
				//highVol+=volume;
			}
			hiVol = highVol/3.0;
			hiVol+=25;
			if (Controller.DEBUG) System.out.println("Hivol: "+hiVol);
		}

		public void finalizeCalibration()
		{
			if (isFreq = true)
			{
				if (hiFreq - lowFreq>=0)
				{
					double temp = hiFreq;
					hiFreq = lowFreq;
					lowFreq = temp;
				}
			}
			else
			{
				if (hiVol - lowVol>=0)
				{
					double temp = hiVol;
					hiVol = lowVol;
					lowVol = temp;
				}
			}
		}




		public double getVolume()
		{
			//	byte[] buffer = new byte[1000];
			
		long lSum = 0;
		for(int i=0; i<buffer.length; i++)
			lSum = lSum + buffer[i];

		double dAvg = lSum / buffer.length;

		double sumMeanSquare = 0d;
		for(int j=0; j<buffer.length; j++)
			sumMeanSquare = sumMeanSquare + Math.pow(buffer[j] - dAvg, 2d);

		double averageMeanSquare = sumMeanSquare / buffer.length;
		double ret =  (Math.pow(averageMeanSquare,0.5d) + 0.5);
			 if (Controller.DEBUG) System.out.println("Volume: "+ret);
			 return ret;
		}

	}