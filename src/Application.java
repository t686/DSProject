import java.io.FileNotFoundException;
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
				System.out.println("Ricart Agrawala algorithm selected");
				RicartAgrawalaServer raServer = new RicartAgrawalaServer();
				RicartAgrawalaClient raClient = new RicartAgrawalaClient();
				raServer.init();

				Reader RAreader = new Reader(raClient);

				Thread raReaderThread = new Thread(RAreader);
				raReaderThread.start();


				break;
			case "CME" :
				System.out.println("Central Mutual Exclusion algorithm selected");
				//init the Server and Client
				Server server = new Server();
				Client client = new Client();

				//Manage the Threads
				Thread clientThread = new Thread();
				Thread serverThread = new Thread();

				//clientThread.start();
				//serverThread.start();
				server.init();

				Reader reader = new Reader(client);

				Thread readerThread = new Thread(reader);
				readerThread.start();
				break;
		}

		
	}
}