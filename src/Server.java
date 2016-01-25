import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Server{
	
	public static final int port = findFreePort();

	public static String host = "none";
	public static ArrayList<String> rndWordSet = new ArrayList<>();

	public static HashSet<String> connectedNodes = new HashSet<>(); //List of active Nodes IPs
	public static int stoppedNodes = 0; 							//Counter of nodes successfully stopped

	//!!! FIELDS FOR CONCATENATION PROCESS !!!

	public static WordConcatenation concatObject = new WordConcatenation();
	public static LinkedList<String> requestQueue = new LinkedList<>();
	public static boolean critSectionBusy;
	public static boolean isRunning;
	public static long startTime;
	public static int stoppedRequester;

	//!!! CONCAT BLOCK END !!!

	public XmlRpcConnector xmlRpcConnector = new XmlRpcConnector();


	//Using HashSet we eliminate the probability of identical elements on a data structure level
	public static String hostString = "";

	public Server() {
	}

	//Start the WebServer
	public void init() {
		System.out.println("Server starting...");
		isRunning = false;
		critSectionBusy = false;
		stoppedRequester = 0;
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
		else concatObject.setWordSet(rndWordSet);
		System.out.println(rndWordSet);
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
				this.xmlRpcConnector.setURL(new URL(Client.getFullAddress(Client.urlFormatter(node))));
				params.removeAllElements();
				params.add(host);

				boolean response = xmlRpcConnector.requestBool("Node.hostBroadcast", params);
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
		System.out.println("Starting concatenation process!");
		if(Client.nodeIPnPort.equals(host)){
			return false;
		}
		boolean keepGoing = true;
		concatObject.clearList();

		try {
			this.xmlRpcConnector.setURL(new URL(Client.getFullAddress(Client.urlFormatter(host))));

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
		return true;
	}

	private boolean concatLoop(WordConcatenation concatObject) {
		System.out.println("Starting concatLoop!");
		Vector<Object> params = new Vector<>();
		params.removeAllElements();
		params.add(Client.nodeIPnPort);
		String response = "";

		while (true) {
			System.out.println("Asking for lifesign");
			try {
				response = this.xmlRpcConnector.requestString("Node.rpcLifeSign", params);
			} catch (XmlRpcException e) {
				System.err.println("rpcLifeSign call failed");
				e.printStackTrace();
				return false;
			}
			switch (response) {
				case "goOn":
					System.out.println("Got 'goOn'");
					concatObject.concatString();
					return true;
				case "wait":
					System.out.println("Have to WAIT");
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
						Thread.currentThread().interrupt();
					}
					break;
				case "stop":
					System.out.println("Should STOP");
					return false;
			}
		}
	}
	/**
	 * Method to let the requester know that this node is alive
	 * Only called via RPC
	 * @return always true to show that its active
	 */
	public String rpcLifeSign(String requester) {

		System.out.println("Getting a request for lifeSign");
		if (!isRunning()) {
			System.out.println("Starting timer");
			startTimer();
		}
		if (checkElapsedTime()) {
			System.out.println("Time is over sending a STOP to: " + requester);
			stoppedRequester++;
			System.out.println(stoppedRequester);
			if (stoppedRequester == connectedNodes.size()-1) broadCastCheckConcat();
			return "stop";
		}
		if (critSectionBusy) {
			System.out.println("Sending a WAIT to: " + requester);
			requestQueue.push(requester);
			return "wait";
		}
		else {
			System.out.println("critSection not Busy so check for Requester in Queue for: " + requester);
			System.out.println("Queue peek: " + requestQueue.peek());
			if(requestQueue.isEmpty()) {
				critSectionBusy = true;
				return "goOn";
			}
			else if(requestQueue.peek().equals(requester)) {
				requestQueue.poll();
				critSectionBusy = true;
				return "goOn";
			}
			else return "wait";
		}
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
		critSectionBusy = false;
		return true;
	}

	/**
	 * This method is called by the host node after the concat process time is over
	 * Only called cia RPC
	 * @return true if all strings were appended correctly
     */
	private boolean checkConcatResult() {
		try {
			return concatObject.checkAddedWords();
		} catch (XmlRpcException e) {
			e.printStackTrace();
			return false;
		}
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
		String tempPath = (Server.class.getResource("Files/wordList.txt")).toString();
		String filePath = tempPath.substring(tempPath.indexOf(":")+1);
		File file = new File(filePath);
		try(BufferedReader br = new BufferedReader(new FileReader(file))) {
			for(String line; (line = br.readLine()) != null; ) {
				System.out.println(line);
				rndWordSet.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		System.out.println(rndWordSet.size());
		return true;
	}

	private void startTimer() {
		this.startTime = System.nanoTime();
		this.isRunning = true;
	}
	private boolean isRunning() {
		return this.isRunning;
	}
	private boolean checkElapsedTime() {
		double elapsedTime = (System.nanoTime() - this.startTime) / 1000000000.0;
		return (elapsedTime > 20);

	}
	private void lockCritSection() {
		this.critSectionBusy = true;
	}
	private void unlockCritSection() {
		this.critSectionBusy = false;
	}

	private void broadCastCheckConcat() {

	}

	private class CheckConcatBroadcaster extends Thread {
		private XmlRpcClient xmlRpcClient;
		private XmlRpcClientConfigImpl config;

		public void CheckConcatBroadcaster() {
			this.config = new XmlRpcClientConfigImpl();
			this.xmlRpcClient = new XmlRpcClient();
		}

		public void run() {
			Vector<String> params = new Vector<>();
			params.removeAllElements();
			for (URL serverURL : Client.serverURLs) {
				if (serverURL.toString().contains(host)) continue;
				this.config.setServerURL(serverURL);
				this.xmlRpcClient.setConfig(this.config);
				try {
					boolean response = (boolean) this.xmlRpcClient.execute("Node.checkConcatResult", params);
					if (response) System.out.println("[Server] Concatenation from node " + serverURL + " succeeded");
					else System.err.println("[Server] Concatenation from node " + serverURL + " failed");
				} catch (XmlRpcException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class XmlRpcConnector {

		private XmlRpcClient xmlRpcClient = new XmlRpcClient();
		private XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();

		public String requestString(String methodName, Vector params) throws XmlRpcException {
			return (String) xmlRpcClient.execute(methodName, params);
		}

		public boolean requestBool(String methodName, Vector params) throws XmlRpcException {
			return (boolean) xmlRpcClient.execute(methodName, params);
		}

		public void setURL(URL url) {
			this.config.setServerURL(url);
			this.xmlRpcClient.setConfig(this.config);
		}

	}
}
