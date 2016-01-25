import java.io.IOException;

/*
 * Distributed systems project. Group #19.
 */

public class Application {

	public static void main(String[] args) throws IOException {
		//RA or CME
		switch (args[0] ) {
		
			case "RA" :
				System.out.println("-> Ricart Agrawala algorithm selected");
				
				RicartAgrawalaServer raServer = new RicartAgrawalaServer();
				RicartAgrawalaClient raClient = new RicartAgrawalaClient();
				
				raServer.init();

				Reader raReader = new Reader(raClient);
				raReader.start();

				break;
				
			case "CME" :
				System.out.println("-> Central Mutual Exclusion algorithm selected");
				
				Server server = new Server();
				Client client = new Client();

				server.init();

				Reader reader = new Reader(client);
				reader.start();

				break;
		}

		
	}
}