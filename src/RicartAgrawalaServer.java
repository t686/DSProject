
public class RicartAgrawalaServer extends Server {
	
	public static volatile Clock clockTS = new Clock();
	
	public void init(){
		super.init();
		setIDs();
		System.out.println("[RA Server] Launching ...");
	}
	
	public boolean receiveRequest(int TS, int nodeID) throws InterruptedException{
		System.out.println("Request received from node: "+nodeID+" with TimeStamp: "+TS);
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
		System.out.println("Sending OK!");
		return true;
	}
	
	public void setIDs(){
		int k = 1;
		for(String nodeID : connectedNodes){
			if(nodeID.equals(Client.nodeIPnPort)){
				RicartAgrawalaClient.nodeID = k;
			}
			k++;
		}
	}
	
	public boolean startRAConcat(){
		System.out.println("[RA Server] Concat process started on node: "+RicartAgrawalaClient.nodeID);
		return true;
	}
	
	public boolean checkState(int TS, int nodeID){
		return (RicartAgrawalaClient.state == Client.State.USING) || ((RicartAgrawalaClient.state == Client.State.REQUESTED)  && (RicartAgrawalaClient.getTimeStampID() < (TS * 10 + nodeID))) ? true : false; 		
	}
}
