
public class RicartAgrawalaServer extends Server {
	
	public static volatile Clock clockTS = new Clock();
	
	public void init(){
		super.init();
		System.out.println("RA Server starting ...");
		//new Thread(new RicartAgrawalaClient()).start();
	}
	
	public boolean receiveRequest(int TS, int nodeID){
		System.out.println("Request received from node: "+nodeID+" with TimeStamp: "+TS);
		clockTS.adjustClockVal(TS);
		while(true){
			RicartAgrawalaClient.lock.lock();
			try {
				
				/*if ((RicartAgrawalaClient.state == State.USING) || (RicartAgrawalaClient.state == State.REQUESTED) 
				&& (RicartAgrawalaClient.request.getTimestampAndID() < (timeStamp*10 + ID)));
					//RicartAgrawalaClient.condition.await();
				else*/
					break;
			} finally {
				RicartAgrawalaClient.lock.unlock();
			}
		}
		System.out.println("Sending OK!");
		return true;
	}

}
