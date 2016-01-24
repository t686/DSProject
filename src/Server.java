import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Vector;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

public class Server{
	
	public static final int port = findFreePort();

	public static String host = "none";
	private HashSet<String> rndWordSet = new HashSet<>();
	
	public static HashSet<String> connectedNodes = new HashSet<>(); //List of active Nodes IPs
	public static int stoppedNodes = 0; 							//Counter of nodes successfully stopped
	
	//Using HashSet we eliminate the probability of identical elements on a data structure level
	private String hostString = "";

	//Start the WebServer
	public void init() {
		System.out.println("Server starting...");
		
		try{
			connectedNodes.add(Client.nodeIPnPort);
			Client.serverURLs.add(new URL(Client.getFullAddress(Client.urlFormatter(Client.nodeIPnPort))));
			
		}catch(MalformedURLException e){
			e.printStackTrace();
		}
		
		WebServer webServer = new WebServer(port);
		XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();
		PropertyHandlerMapping propertyHandlMap = new PropertyHandlerMapping();
		
		try {
			propertyHandlMap.addHandler("Node", Server.class);
		} catch(XmlRpcException e){
			e.printStackTrace();
		} 
		
		xmlRpcServer.setHandlerMapping(propertyHandlMap);
		XmlRpcServerConfigImpl xmlRpcServerConfig = (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
		xmlRpcServerConfig.setEnabledForExtensions(true);
		xmlRpcServerConfig.setContentLengthOptional(false);
		
		try{
			DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
			Date date = new Date();
			webServer.start();
			System.out.println("Server succesfuly started at " + dateFormat.format(date));
		} catch(IOException e){
			e.printStackTrace();
		}
		if(!loadFile()) System.out.println("File not found!");
		else System.out.println("File loaded successfully!");
	}
	
	public Object[] join(String newNodeIP){
		try {
			if(connectedNodes.add(newNodeIP)){
				Client.serverURLs.add(new URL(Client.getFullAddress(newNodeIP)));
				System.out.println("[Server] NEW node with address: "+newNodeIP+" was connected!");
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}		
		
		return connectedNodes.toArray();
	}
		
	public boolean signOff(String nodeIP){
		if(connectedNodes.remove(nodeIP)){
			System.out.println("[Server] Node "+nodeIP+" is leaving the network.");

			for(int i=0; i<Client.serverURLs.size(); i++){
				URL url = Client.serverURLs.get(i);
				if(url.toString().compareTo(Client.urlFormatter(nodeIP)) == 0){
					Client.serverURLs.remove(i);
				}
			}
			if (host.equals(nodeIP)) startElection();
			return true;
		}
		return false;

	}

//	public boolean startOperations(String word){
//		System.out.println("[Server] Initial word: " + word+". Starting RA Client...");
//		//new Thread(new RicartAgrawalaClient()).start();
//		return true;
//	}
	
	//Increment the stopped counter until == connectedNodes hash size and print the final string result 
//	public boolean stopOperations(){
//		stoppedNodes++;
//		if(connectedNodes.size() == stoppedNodes){
//			System.out.println("[Server] All operations successfully stoped. Final result: " + hostString);
//		}
//		return true;
//	}

	public boolean startElection() {
		if (!(connectedNodes.size() > 1)) {
			System.out.println("you are not connected to a network");
		}
		else if(Bully.startElection(port, connectedNodes)) {
			host = Client.nodeIPnPort;
			System.out.println("[Server] This application (" + host + ") won the host election");
			broadcastIamHost();
			return true;
		}
		return true;

	}

	private void broadcastIamHost() {
		Vector<Object> params = new Vector<>();

		for(String node : connectedNodes) {
			try {
				Client.config.setServerURL(new URL(Client.getFullAddress(Client.urlFormatter(node))));
				Client.xmlRpcClient.setConfig(Client.config);
				params.removeAllElements();
				params.add(host);

				boolean response = (boolean) Client.xmlRpcClient.execute("Node.hostBroadcast", params);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (XmlRpcException e) {
				System.out.println("XmlRpcException");
				e.printStackTrace();
			}
		}
	}

	//RPC call Method from Node
	public synchronized String rpcElectionRequest(int requester) {

		System.out.println("[Server] received a election message from: " + requester);

		//this should not happen
		if (requester > port) return "Continue";

		//this servers port has a higher value then the requester so it takes over the election process
		startElection();
		return "Stop";
	}

	//RPC call Method from other Server
	public boolean hostBroadcast(String newHost) {
		host = newHost;
		return true;
	}


	//Block for Methods concerning the Concatenation Process

	/**
	 * Method called by concatBroadcaster to initiate the Concatenation process
     */
	public boolean startConcatProcess() {
		if(Client.nodeIPnPort.equals(host)){
			return false;
		}
		boolean keepGoing = true;
		WordConcatenation concatObject = new WordConcatenation();

		try {
			Client.config.setServerURL(new URL(Client.getFullAddress(Client.urlFormatter(host))));
			Client.xmlRpcClient.setConfig(Client.config);

		} catch (MalformedURLException e) {
			System.err.println("MalformedURLException!");
			e.printStackTrace();
		}

		while (keepGoing) {
			try {
				Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 4000 + 1));
			} catch (InterruptedException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
			keepGoing = concatLoop(concatObject);
		}
		try {
			concatObject.checkAddedWords();
		} catch (XmlRpcException e) {
			e.printStackTrace();
		}
		concatObject.clearList();
		return true;
	}

	private boolean concatLoop(WordConcatenation concatObject) {
		Vector<Object> params = new Vector<>();
		params.removeAllElements();
		String response = "";

		while (true) {
			try {
				response = (String) Client.xmlRpcClient.execute("Node.rpcLifeSign", params);
			} catch (XmlRpcException e) {
				e.printStackTrace();
			}
			switch (response) {
				case "goOn":
					concatObject.concatString("DUMMY");
					return true;
				case "wait":
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
						Thread.currentThread().interrupt();
					}
					break;
				case "stop":
					return false;
			}
		}
	}
	/**
	 * Method to let the requester know that this node is alive
	 * Only called via RPC
	 * @return always true to show that its active
	 */
	public boolean rpcLifeSign() {
		//TODO catch every possible scenario
		return true;
	}

	/**
	 * Method to past the current String for the concat prozess to the requester
	 * Only called via RPC
	 * @return current host string
     */
	public String rpcRequestString() {
		return this.hostString;
	}

	/**
	 * Method which pasts the new created string from requester to the host
	 * Only called via RPC
	 * @param newString freshly created string by requester node
	 * @return always true to indicate it went correctly
     */
	public boolean rpcOverrideString(String newString) {
		this.hostString = newString;
		return true;
	}

	/**
	 * This method is called by the host node after the concat process time is over
	 * to evaluate if every concatenation went right
	 * @return true if all strings were appended correctly
     */
	private boolean checkConcatResult() {
		//TODO: implement the broadcast check to every node
		return true;
	}
	

	/*
	 * UTILITY METHODS
	 */
	public static void listOfConnections() {
		System.out.println("[Server] MASTER node is: "+host);
		if(connectedNodes.size() > 0){
			System.out.println("[Server] There are " + connectedNodes.size()
					+ " IPs:");
			for (String str : connectedNodes) {
				System.out.println(str);
			}
		}else{
			System.out.println("The network is empty!");
		}
		
	}

	//Function by github user @vorburger
	private static int findFreePort() {
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(0);
			socket.setReuseAddress(true);
			int port = socket.getLocalPort();
			try {
				socket.close();
			} catch (IOException e) {
				// Ignore IOException on close()
			}
			return port;
		} catch (IOException e) {
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
				}
			}
		}
		throw new IllegalStateException("Could not find a free TCP/IP port to start HTTP Server on");
	}
	private boolean loadFile() {
		File file = new File(String.valueOf(Server.class.getResource("Files/wordList.txt")));
		if(file.canRead()) System.out.println("File is accessible!");
		else System.err.println("File not accessible!");
		try(BufferedReader br = new BufferedReader(new FileReader(file))) {
			for(String line; (line = br.readLine()) != null; ) {
				rndWordSet.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
