import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.xmlrpc.XmlRpcException;

public class Reader extends Thread{
	
	public Client client;
	
	private BufferedReader buffReader = new BufferedReader(new InputStreamReader(System.in));
	private String inputText = "";
	
	public Reader(Client client){
		this.client = client;
	}

	@Override
	public void run() {
		//Infinite loop for listening to input characters
		System.out.println("== List of options == \n-join \n-signoff \n-start \n-bully  \n-host  \n-list  \n-exit");
		while(true){
			try{
				inputText = buffReader.readLine().trim();
				System.out.println("> " + inputText);
				selectedOption(inputText);
			}catch(IOException | XmlRpcException e){
				e.printStackTrace();
			}	
		}
	}
	
	private void selectedOption(String option) throws IOException, XmlRpcException{
			switch (inputText) {
				case "join":
					System.out.println("Enter IP:Port");
					String newNodeIP = buffReader.readLine().trim();
					client.join(newNodeIP);
					break;
				case "signoff":
					client.signOff();
					break;
				case "start":
					client.startConcatProcess();
					break;
				case "bully":
					client.startElection();
					break;
				case "host":
					System.out.println(Server.host + " is the current host");
					break;
				case "list":
					Client.showAllLists();
					break;
				case "exit":
					System.out.println("Quiting the program...");
					client.signOff();
					System.exit(0);
					break;
				default:
					System.err.println("Pardon, wrong input.");
					break;

			}
	}
}
