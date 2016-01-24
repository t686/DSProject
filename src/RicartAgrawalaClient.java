import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class RicartAgrawalaClient extends Client{
	
	int minWaitRange = 1000;
	int maxWaitRange = 3000;

	public static State state;
	long startTime;
	
	Clock clockTS = RicartAgrawalaServer.clockTS;
	WordConcatenation wordConcat = new WordConcatenation();

	public static ReentrantLock lock = new ReentrantLock();
	ExecutorService executor = Executors.newCachedThreadPool();


	public RicartAgrawalaClient() {
		System.out.println("RA Client starting...");
		lock.lock();
		try{
			state = State.FREE;
			//request = new Request();
		} finally {
			lock.unlock();
		}
		
	}
	
	public void accessingCriticalSection(){
		System.out.println("Accessing the Critical Section");
		lock.lock();
		try{
			clockTS.clockTick();
			//request.modify(clockTS.getClockVal());
			state = State.REQUESTED;
		} finally {
			lock.unlock();
		}

		//Sending requests and waiting for responses
		//requestSenders.forEach(x -> oKs.add(executor.submit(x)));

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
			//condition.signalAll() ???
		} finally {
			lock.unlock();
		}
	}


	//When and where is this method called?
	public void magicLoop(){
		startTime = System.currentTimeMillis();
		while(EXECUTION_TIME > System.currentTimeMillis() - startTime){

			int randPeriod = (int) (Math.random() * (maxWaitRange - minWaitRange)) + minWaitRange;
			try {
				Thread.sleep(randPeriod);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			accessingCriticalSection();
			//Entering CS
			//params.removeAllElements();
			//this.runOverRpc("Node.", params);
			releaseCriticalSection();
		}
		stopOperations();
	}


}
