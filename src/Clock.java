/*
 * Class required by Ricart-Agrawala algorithm
 * (Implement later) 
 */
public class Clock {
	
	private int currentClock;
	
	public Clock(){
		currentClock = 0;
	}
	
	public int getClockVal(){
		return currentClock;
	}
	
	public void clockTick(){
		currentClock++;
	}
	
	public void resetClockVal(){
		currentClock = 0;
	}
	
	public void adjustClockVal(int otherClockVal){
		if(otherClockVal > currentClock){
			currentClock = otherClockVal + 1;
		}
	}
	
}
