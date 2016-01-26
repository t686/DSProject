
public class RicartAgrawalaServer extends Server {
	
	public static volatile Clock clockTS = new Clock();
	
	public void init(){
		super.init();
		System.out.println("[RA Server] Launching ...");
		RicartAgrawalaClient.nodeID = port;
		
	}
	
	public boolean receiveRequest(int TS, int nodeID) throws InterruptedException{
		System.out.println("Request received from node "+nodeID+" with TimeStamp: "+TS);
		clockTS.adjustClockVal(TS);
		while(true){
			RicartAgrawalaClient.lock.lock();
			try {
				if(checkState(TS, nodeID)){
					System.err.println("PLEEEEASE");
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
	
	
	public boolean addString(){
		concatObject.concatString();
		return true;
	}

	
	public boolean checkState(int TS, int nodeID){
		System.out.println("STATE: "+RicartAgrawalaClient.state);
		//System.out.println("TIMESTAMP: "+TS+" and NODE: "+nodeID);
		//System.out.println("CLIENT TIME STAMP: "+RicartAgrawalaClient.getTimeStampID());
		//System.out.println("IS it tRUE?");
		//System.out.println(RicartAgrawalaClient.getTimeStampID() < (TS * 10 + nodeID));
		return (RicartAgrawalaClient.state == Client.State.USING) || ((RicartAgrawalaClient.state == Client.State.REQUESTED)  && (RicartAgrawalaClient.getTimeStampID() > (TS * 10 + nodeID))) ? true : false; 		
	}
}
