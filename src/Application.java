import java.io.IOException;

/*
 * Distributed systems project. Group #19.
 */

public class Application {

	public static void main(String[] args) throws IOException {
		
		System.out.println("Oi, mate!");
		
		//init the Server and Client
		Server server = new Server();
		Client client = new Client();
		
		//Manage the Threads
		Thread clientThread = new Thread();
		Thread serverThread = new Thread();
		
		clientThread.start();
		serverThread.start();
		server.run();
		
		Thread readerThread = new Thread(new Reader(client));
		readerThread.start();
		
	}
}