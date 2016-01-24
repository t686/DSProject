import java.io.IOException;
import java.net.URL;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;


public class Client {
	public enum State {
		FREE, REQUESTED, USING
	};
	
	public final long EXECUTION_TIME = 20000;
	
	public static XmlRpcClient xmlRpcClient;
	public static XmlRpcClientConfigImpl config;
	
	public static String nodeIp = null;
	public static String nodeIPnPort = null;
	
	public Vector<Object> params = new Vector<>();
	public static Vector<URL> serverURLs = new Vector<>(); //List of URLs of other machines
	
	public Client(){
		init();
	}

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

	
	public void join(String newNodeIP){
		
		if(serverURLs.size() > 1){
			System.err.println("You are a part of an existing network!");
		}else if(newNodeIP.equals(nodeIPnPort)){
			System.err.println("You can't connect to yourself!");
		}else{
			try{
				
				config.setServerURL(new URL(getFullAddress(urlFormatter(newNodeIP))));
				xmlRpcClient.setConfig(config);
				params.removeAllElements();
				params.add(nodeIPnPort);
				try{
					System.err.println("[Client] NEW NODE JOINING: "+newNodeIP);
					
					Object[] result  = (Object[]) xmlRpcClient.execute("Node.join", params);
					for (Object obj : result){
						String temp = (String) obj;
						if(!temp.equals(urlFormatter(nodeIPnPort)) && Server.connectedNodes.add(temp)){
							serverURLs.add(new URL(getFullAddress(urlFormatter(temp))));
						}
					}
					System.out.println("[Client] Connected !");
					
					//Inform other nodes about a new member of the network
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
	
	public void signOff() throws XmlRpcException, MalformedURLException{
		//Notify other nodes about leaving the network
		if(serverURLs.size() > 1){
			//URL[] urlArr = new URL[serverURLs.size()];
			//urlArr = serverURLs.toArray(urlArr);
			
			for (URL url : serverURLs) {
				if(url.toString().compareTo(getFullAddress(nodeIPnPort)) != 0){
					config.setServerURL(url);
					xmlRpcClient.setConfig(config);
					params.removeAllElements();
					params.add(nodeIPnPort);
					if (!(boolean) xmlRpcClient.execute("Node.signOff", params)) {
						System.out.println("[Client] Failed to signOff from "+ url.getAuthority());
					}
				}
			}
			//Probably optimize those straight forward commands 
			serverURLs.clear();
			serverURLs.add(new URL(getFullAddress(urlFormatter(nodeIPnPort))));
			Server.connectedNodes.clear();
			Server.connectedNodes.add(nodeIPnPort);
			Server.host = "none";
			//TODO cleanup
			System.out.println("[Client] Signed off!");
		}else{
			System.err.println("[Client] You are not connected to a network");
		}
	}

	public void startElection() throws XmlRpcException, MalformedURLException {
		if(!(serverURLs.size() > 1)) {
			System.err.println("[Client] You are not connected to a network");
		}
		else {
			config.setServerURL(new URL(getFullAddress(urlFormatter(nodeIPnPort))));
			xmlRpcClient.setConfig(config);
			params.removeAllElements();
			xmlRpcClient.execute("Node.startElection", params);
		}
	}
	
	//function initiating the Mutual Exclusion "fight" and concatenation processes
	public void startConcatProcess(){
		if(serverURLs.size() > 1){
			params.removeAllElements();
			runOverRpc("Node.startConcatProcess", params);
		} else {
			System.err.println("[Client] You are not connected to a network");
		}
	}
	
	//function called after the EXECUTION_TIME <= 0 from the RA Client class
	public void stopOperations(){
		params.removeAllElements();
		runOverRpc("Node.stopOperations", params);
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
			System.out.println("[Client] There are " + serverURLs.size()
					+ " network members:");
			for (URL url : serverURLs) {
				System.out.println(url);
			}
		}else{
			System.out.println("The network is empty!");
		}
	}
	
	public static String getFullAddress(String address){
		if (!address.contains("http://"))
			address = "http://" + address;
		if (!address.contains("/xmlrpc"))
			address = address + "/xmlrpc";
		return address;
	}
	
	public static String urlFormatter(String ip) {
		return "http://"+ip+"/xmlrpc";
	}
	
	/**
	 * inner class just used by the client to tell every node to start the concatenation process
	 */
	public class ConcatBroadcaster extends Thread{

		public void run() {

		}
	}
}
