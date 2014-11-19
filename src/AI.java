
public class AI {
	private boolean right; //if true, paddle is on right, else on left
	private Model model;
	private double ret,y,x,dy,dx,leftBound,rightBound; //Java performs much better if frequently changed variables are not re-initialized every time
	private int ret2;
	private int lastMove; //the target that the AI has

	public AI(boolean r, Model m){
		right = r;
		model = m;
	}

	public int getMove(){ //returns the location the AI paddle is going to have to move to so it can hit the ball, updates the lastMove
		ret = 0;
		dy = model.getBallDy();
		dx = model.getBallDx();
		
		if (right && dx<0 || !right && dx>0){
			ret2 = (int) model.getBounds().getCenterY();
			lastMove = ret2;
			if (Controller.DEBUG){
				 if (right) System.out.print("Move GUESSED for right paddle: ");
				 else System.out.print("Move GUESSED for left paddle: ");
				 System.out.println(ret2);
			}
			return ret2;
		}
		
		y = model.getBall().getBounds().getCenterY();
		x = model.getBall().getBounds().getCenterX();
		leftBound = (double)model.getBounds().x;
		rightBound = (double)model.getBounds().x + (double)model.getBounds().getWidth();

		if (right){ ret = y + (rightBound - x)/dx * dy; }	
		else{ ret = y + (leftBound - x)/dx * dy; }	
		
		ret2 = (int) Math.round(ret);
		lastMove = ret2;
		if (Controller.DEBUG){
			 if (right) System.out.print("Move calculated for right paddle: ");
			 else System.out.print("Move calculated for left paddle: ");
			 System.out.println(ret2);
		}
		return ret2;
	}

	public int getLastMove(){
		return lastMove;
	}
}
