
public class RicartAgrawalaServer extends Server {
	
	public static volatile Clock clockTS = new Clock();
	
	public void init(){
		super.init();
		System.out.println("[RA Server] Launching ...");
		RicartAgrawalaClient.nodeID = port;	
	}
	
	public boolean receiveRequest(int TS, int nodeID) throws InterruptedException{
		clockTS.adjustClockVal(TS);
		while(true){
			RicartAgrawalaClient.lock.lock();
			try {
				if(checkState(TS, nodeID)){
					RicartAgrawalaClient.reqBroadcaster.await();
				} else {
					break;
				}
			} finally {
				RicartAgrawalaClient.lock.unlock();
			}
		}
		return true;
	}
	
	public boolean checkState(int TS, int nodeID){
		boolean result = RicartAgrawalaClient.isRequestingCS && 
				((TS > RicartAgrawalaClient.timeStamp) || (TS == RicartAgrawalaClient.timeStamp && nodeID > RicartAgrawalaClient.nodeID));
		return result;
	}
}