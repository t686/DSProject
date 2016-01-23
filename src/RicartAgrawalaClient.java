
public class RicartAgrawalaClient extends Client implements Runnable{
	
	long startTime;
	
	public RicartAgrawalaClient(){
		
	}
	
	@Override
	public void run(){
		startTime = System.currentTimeMillis();
		//while(EXECUTION_TIME > System.currentTimeMillis() - startTime){
			// do the method call for concatenating words
		//}
		//stopOperations();
	}

	@Override
	public void startConcatProcess() {

	}

}
