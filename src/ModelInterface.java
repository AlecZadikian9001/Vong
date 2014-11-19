import java.awt.Polygon;


public interface ModelInterface
{
	Polygon getPaddle1();
	Polygon getPaddle2();
	Polygon getBall();
	double getBallDy();
	double getBallDx();
	int getWinState();
	boolean checkSpeed(); //if there was a collision with something that changes the velocity
	
	void resetWin(); //sets winState to 0
}
