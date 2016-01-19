import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Reader implements Runnable{
	
	public Client client;
	
	private BufferedReader buffReader = new BufferedReader(new InputStreamReader(System.in));;
	private String inputText = "";
	
	public Reader(Client client){
		this.client = client;
		//this.server = server;
	}

	@Override
	public void run() {
		//Infinite loop for listening to input characters
		System.out.println("List of options: join, signoff, bully, start, list, exit");
		while(true){
			try{
				inputText = buffReader.readLine().trim();
				System.out.println("> " + inputText);
				selectedOption(inputText);
			}catch(IOException e){
				e.printStackTrace();
			}	
		}
	}
	
	private void selectedOption(String option) throws IOException{
			switch (inputText) {
			case "join":
				System.out.println("Operation \"Join\" initiated.");
				System.out.println("Enter IP:Port");
				String newNodeIP = buffReader.readLine().trim();
				client.join(newNodeIP);
				break;
			case "signoff":
				System.out.println("Operation \"Sign Off\" initiated.");
				//client.signoff();
				break;
			case "start":
				System.out.println("Distributed text appending operation initiated.");
				//client.start();
				break;
			case "bully":
				System.out.println("\"Bully\" election algorithm initiated.");
				//client.bully();
				break;
			case "list":
				client.listOfNodes();
				break;
			case "exit":
				System.out.println("Quiting the program...");
				//client.signOff();
				System.exit(0);
				break;
			default:
				System.err.println("Pardon, wrong input.");
				break;
			}
	}
}
