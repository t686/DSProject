import java.net.URL;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;


public class Client {
	
	public final long EXECUTION_TIME = 10000; //Loop Execution time 
	
	public static XmlRpcClient xmlRpcClient;
	public static XmlRpcClientConfigImpl config;
	
	public static String nodeIp = null;
	public static String nodeIPnPort = null;
	
	public Vector<Object> params = new Vector<>();
	public static Vector<URL> serverURLs = new Vector<>(); //List of URLs of other machines
	
	public Client(){
		init();
	}

	//Basic setup of client data. (IP, RPC)
	public void init(){
		try{
			System.out.println("Client data initializing...");
			nodeIp = InetAddress.getLocalHost().getHostAddress();
			nodeIPnPort = nodeIp+":"+Server.port;
			config = new XmlRpcClientConfigImpl();
			xmlRpcClient = new XmlRpcClient();
		}catch(UnknownHostException e){
			e.printStackTrace();
		}
		
		System.out.println("Your current IP address is: " + nodeIp +":"+Server.port);
	}

	
	//function for joining the network. The new node IP is propagated through the network to all nodes
	public void join(String newNodeIP){
		
		if(serverURLs.size() > 1){
			System.err.println("You are a part of an existing network!");
		}else if(newNodeIP.equals(nodeIPnPort)){
			System.err.println("You can't connect to yourself!");
		}else{
			try{
				
				config.setServerURL(new URL(formatAddress(newNodeIP)));
				xmlRpcClient.setConfig(config);
				params.removeAllElements();
				params.add(nodeIPnPort);
				try{
					
					Object[] result  = (Object[]) xmlRpcClient.execute("Node.join", params);
					for (Object obj : result){
						String temp = (String) obj;
						if(!temp.equals(formatAddress(nodeIPnPort)) && Server.connectedNodes.add(temp)){
							serverURLs.add(new URL(formatAddress(temp)));
						}
					}
					System.out.println("[Client] Connected !");
					
					//Notify other nodes about a new member of the network
					for(int i=0; i<serverURLs.size(); i++){
						config.setServerURL(serverURLs.get(i));
						xmlRpcClient.setConfig(config);
						xmlRpcClient.execute("Node.join", params);
					}
				startElection();
				}catch(XmlRpcException err){
					System.err.println(err.getMessage());
				}
			
			}catch(MalformedURLException e){
				e.printStackTrace();
				System.err.println("[Client] Wrong machine address!");
			}
		}
	}
	
	//function for leaving the network. Notifying other nodes and updating all lists.
	public void signOff() throws XmlRpcException, MalformedURLException{
		//Notify other nodes about leaving the network
		if(serverURLs.size() > 1){
			
			for (URL url : serverURLs) {
				if(url.toString().compareTo(formatAddress(nodeIPnPort)) != 0){
					config.setServerURL(url);
					xmlRpcClient.setConfig(config);
					params.removeAllElements();
					params.add(nodeIPnPort);
					if (!(boolean) xmlRpcClient.execute("Node.signOff", params)) {
						System.out.println("[Client] Failed to signOff from "+ url.getAuthority());
					}
				}
			}
			serverURLs.clear();
			serverURLs.add(new URL(formatAddress(nodeIPnPort)));
			Server.connectedNodes.clear();
			Server.connectedNodes.add(nodeIPnPort);
			Server.host = "none";
			System.out.println("[Client] Signed off!");
		}else{
			System.err.println("[Client] You are not connected to a network");
		}
	}

	//function start the Bully algorithms Master Node election
	public void startElection() throws XmlRpcException, MalformedURLException {
		if(!(serverURLs.size() > 1)) {
			System.err.println("[Client] You are not connected to a network");
		}
		else {
			config.setServerURL(new URL(formatAddress(nodeIPnPort)));
			xmlRpcClient.setConfig(config);
			params.removeAllElements();
			xmlRpcClient.execute("Node.startElection", params);
		}
	}
	
	//function initiating the Mutual Exclusion "fight" and concatenation processes
	public void startConcatProcess(){
		if(!(serverURLs.size() > 1)){
			System.err.println("[Client] You are not connected to a network");
			return;
		}
		for (URL serverURL : serverURLs) {
			System.out.println("Concat started for node: " + serverURL);
			new Thread(new ConcatBroadcaster(serverURL)).start();
		}
	}
	
	
	/*
	 * Synchronized function that triggers start or stop functions on all nodes simultaneously
	 */
	synchronized public void runOverRpc(String functionName, Vector<Object> params){

		for (URL url : serverURLs){
			config.setServerURL(url);
			xmlRpcClient.setConfig(config);
			try {
				System.err.println("[Client] Running function: "+functionName+" over RPC.");
				xmlRpcClient.execute(functionName, params);
			} catch (XmlRpcException e) {
				e.printStackTrace();
			}
		}
	}
	
	/*
	 * UTILITY METHODS
	 */
	public static void showAllLists(){
		Server.listOfConnections();
		System.out.println("______");
		listOfNodes();
	}
	
	public static void listOfNodes() {
		if(serverURLs.size() > 0){
			System.out.println("[Client] There are " + serverURLs.size()+ " network members:");
			for (URL url : serverURLs) {
				System.out.println(url);
			}
		}else{
			System.out.println("The network is empty!");
		}
	}
	
	public static String formatAddress(String address){
		if (!address.contains("http://")) address = "http://" + address;
		if (!address.contains("/xmlrpc")) address += "/xmlrpc";
		return address;
	}

	
	/**
	 * inner class just used by the client to tell every node to start the concatenation process
	 */
	public class ConcatBroadcaster implements Runnable{

		private URL serverURL;
		private Vector<Object> params = new Vector<>();
		private XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		private XmlRpcClient xmlRpcClient = new XmlRpcClient();

		public ConcatBroadcaster(URL serverURL) {
			this.serverURL = serverURL;
		}
		@Override
		public void run() {
			params.removeAllElements();
			int xmlrpcConnTimeout = 10000; // Connection timeout
			int xmlrpcReplyTimeOut = 60000; // Reply timeout
			this.config.setServerURL(serverURL);
			this.config.setConnectionTimeout(xmlrpcConnTimeout);
			this.config.setReplyTimeout(xmlrpcReplyTimeOut);
			this.xmlRpcClient.setConfig(config);
			try {
				this.xmlRpcClient.execute("Node.startConcatProcess", params);
			} catch (XmlRpcException e) {
				System.err.println("[ConcatBroadcaster] Node " + serverURL + " does not respond");
				e.printStackTrace();
			}
		}
	}
}
