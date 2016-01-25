import java.net.URL;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
		for (RARequest x : requestSenders) {
		    permits.add(executor.submit(x));
		}
		
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
				requestSenders.add(new RARequest(url));
			}
			
			startTime = System.currentTimeMillis();
			System.out.println("[RA Client] Concatenation process started.");
			while(EXECUTION_TIME > System.currentTimeMillis() - startTime){

				int randPeriod = (int) (Math.random() * (maxWaitRange - minWaitRange)) + minWaitRange;
				try {
					Thread.sleep(randPeriod);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				accessingCriticalSection();
				//wordConcat.concatString();
				releaseCriticalSection();
			}
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

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
