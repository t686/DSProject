import java.net.URL;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

public class RicartAgrawalaClient extends Client {
	
	int minWaitRange = 1000;
	int maxWaitRange = 3000;

	public static State state;
	private long startTime;
	public static int timeStamp;
	public static int nodeID;
	private Vector<Object> params = new Vector<Object>();
	
	Clock clockTS = RicartAgrawalaServer.clockTS;
	WordConcatenation concatObject = new WordConcatenation();

	public static ReentrantLock lock = new ReentrantLock(true);
	public static Condition reqBroadcaster = lock.newCondition();

	
	ExecutorService executor = Executors.newCachedThreadPool();
	Vector<RARequest> requestSenders = new Vector<RARequest>();


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
		System.out.println("Accessing the CS");
		lock.lock();
		try{
			clockTS.clockTick();
			state = State.REQUESTED;
			updateTimeStamp(clockTS.getClockVal());
		} finally {
			lock.unlock();
		}

		//Sending requests and waiting for responses
		for(URL url : serverURLs){
			new Thread(new runConcatBroadcast(url)).start();
		}

		lock.lock();
		try {
			state = State.FREE;
		} finally {
			lock.unlock();
		}
	}

	public void releaseCriticalSection(){
		System.out.println("Releasing the CS");
		lock.lock();
		try{
			state = State.FREE;
			reqBroadcaster.signalAll();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void startConcatProcess(){
		if(serverURLs.size() > 1){
			System.out.println("[RA Client] Concatenation process started.");
				
			startTime = System.currentTimeMillis();
			while(System.currentTimeMillis() - startTime < EXECUTION_TIME){

				int randPeriod = (int) (Math.random() * (maxWaitRange - minWaitRange)) + minWaitRange;
				try {
					Thread.sleep(randPeriod);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				accessingCriticalSection();
				concatObject.concatString();
				releaseCriticalSection();
			}
			try {
				concatObject.checkAddedWords();
			} catch (XmlRpcException e) {
				e.printStackTrace();
			}
			System.out.println("[RA Client] Concatenation process finished.");
		} else {
			System.err.println("[RA Client] You are not connected to a network");
		}	
	}
	
	public static int getTimeStampID(){
		return timeStamp*10+nodeID;
	}
	
	public void updateTimeStamp(int newTS){
		RicartAgrawalaClient.timeStamp = newTS;
	}
	
	/**
	 * inner class just used by the client to tell every node to start the concatenation process
	 */
	public class runConcatBroadcast implements Runnable{

		private URL serverURL;
		private Vector<Object> params = new Vector<>();
		private XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		private XmlRpcClient xmlRpcClient = new XmlRpcClient();

		public runConcatBroadcast(URL url) {
			this.serverURL = url;
		}
		@Override
		public void run() {
			params.removeAllElements();
			params.add(timeStamp);
			params.add(nodeID);
			int xmlrpcConnTimeout = 10000; // Connection timeout
			int xmlrpcReplyTimeOut = 60000; // Reply timeout
			config.setServerURL(serverURL);
			config.setConnectionTimeout(xmlrpcConnTimeout);
			config.setReplyTimeout(xmlrpcReplyTimeOut);
			xmlRpcClient.setConfig(config);
			try {
				xmlRpcClient.execute("RANode.receiveRequest", params);
			} catch (XmlRpcException e) {
				System.err.println("[RA Conccat Broadcast] Node " + serverURL + " does not respond");
				e.printStackTrace();
			}
		}
	}
}
