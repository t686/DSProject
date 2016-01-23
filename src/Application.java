import java.io.IOException;

/*
 * Distributed systems project. Group #19.
 */

public class Application {

	public static void main(String[] args) throws IOException {
		//RA CME
		System.out.println("Oi, mate!");
		switch (args[0] ) {
			case "RA" :
				RAServer raServer = new RAServer();
				RicartAgrawalaClient raClient = new RicartAgrawalaClient();

				raServer.run();
				raClient.run();


				break;
			case "CME" :
				//init the Server and Client
				Server server = new Server();
				Client client = new Client();

				//Manage the Threads
				Thread clientThread = new Thread();
				Thread serverThread = new Thread();

				//clientThread.start();
				//serverThread.start();
				server.run();

				Reader reader = new Reader(client);
				reader.start();

				//Thread readerThread = new Thread(new Reader(client));
				//readerThread.start();
				break;
		}

		
	}
}