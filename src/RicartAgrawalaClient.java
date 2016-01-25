import java.net.URL;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.xmlrpc.XmlRpcException;

public class RicartAgrawalaClient extends Client implements Runnable{
	
	int minWaitRange = 1000;
	int maxWaitRange = 3000;

	public static State state;
	private long startTime;
	public static int timeStamp;
	public static int nodeID;
	private Vector<Object> params = new Vector<Object>();
	
	Clock clockTS = RicartAgrawalaServer.clockTS;
	WordConcatenation wordConcat = new WordConcatenation();

	public static ReentrantLock lock = new ReentrantLock(true);
	public static Condition reqBroadcaster = lock.newCondition();

	
	ExecutorService executor = Executors.newCachedThreadPool();
	Vector<RARequest> requestSenders = new Vector<RARequest>();
	LinkedList<Future<Boolean>> permits = new LinkedList<Future<Boolean>>();


	public RicartAgrawalaClient() {
		System.out.println("[RA Client] Initializing ...");
		lock.lock();
		try{
			state = State.FREE;
		} finally {
			lock.unlock();
		}
	}
	
	public void accessingCriticalSection(){
		System.out.println("Accessing the Critical Section");
		lock.lock();
		try{
			clockTS.clockTick();
			state = State.REQUESTED;
			updateTimeStamp(clockTS.getClockVal());
		} finally {
			lock.unlock();
		}

		//Sending requests and waiting for responses
		System.out.println("RequestSenders size: "+requestSenders.size());
		//for (RARequest x : requestSenders) {
		//    permits.add(executor.submit(x));
		//}
		
		permits.clear();
		lock.lock();
		try {
			state = State.FREE;
		} finally {
			lock.unlock();
		}
	}

	public void releaseCriticalSection(){
		System.out.println("Releasing the Critical Section");
		lock.lock();
		try{
			state = State.FREE;
			reqBroadcaster.signalAll();
		} finally {
			lock.unlock();
		}
	}
	

	public void startConcatProcess(){
		if(serverURLs.size() > 1){
			
			for(URL url : serverURLs){
				//requestSenders.add(new RARequest(url));
				new Thread(new runConcatBroadcast(url)).start();
			}
			System.out.println("[RA Client] Concatenation process started.");
			
			//In case of calling the startRAConcat from Server probably move this code to RAServer ?
			/*startTime = System.currentTimeMillis();
			
			while(EXECUTION_TIME > System.currentTimeMillis() - startTime){

				int randPeriod = (int) (Math.random() * (maxWaitRange - minWaitRange)) + minWaitRange;
				try {
					Thread.sleep(randPeriod);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				accessingCriticalSection();
				
				releaseCriticalSection();
			}
			*/
			System.out.println("Stopped. Final String: ");
		} else {
			System.err.println("[Client] You are not connected to a network");
		}	
	}

	public Vector<Object> getParams(){
		params.removeAllElements();
		params.add(timeStamp);
		params.add(nodeID);
		return params;
	}
	
	public static int getTimeStampID(){
		return timeStamp*10+nodeID;
	}
	
	public void updateTimeStamp(int newTS){
		this.timeStamp = newTS;
	}
	
	/**
	 * inner class just used by the client to tell every node to start the concatenation process
	 */
	public class runConcatBroadcast implements Runnable{

		private URL serverURL;
		private Vector<Object> params1 = new Vector<>();

		public runConcatBroadcast(URL url) {
			this.serverURL = url;
		}
		@Override
		public void run() {
			params1.removeAllElements();
			int xmlrpcConnTimeout = 10000; // Connection timeout
			int xmlrpcReplyTimeOut = 60000; // Reply timeout
			Client.config.setServerURL(serverURL);
			Client.config.setConnectionTimeout(xmlrpcConnTimeout);
			Client.config.setReplyTimeout(xmlrpcReplyTimeOut);
			Client.xmlRpcClient.setConfig(Client.config);
			try {
				Client.xmlRpcClient.execute("RANode.startRAConcat", params1);
			} catch (XmlRpcException e) {
				System.err.println("[RA Conccat Broadcast] Node " + serverURL + " does not respond");
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
