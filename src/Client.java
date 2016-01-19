import java.io.IOException;
import java.net.URL;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;


public class Client implements Runnable{
	
	public final long exec_time = 20000;
	
	public static XmlRpcClient xmlRpcClient;
	public static XmlRpcClientConfigImpl config;
	
	public static String nodeIp = null;
	public static String nodeIPnPort = null;
	public Vector<Object> params = new Vector<Object>();
	
	public static Vector<URL> serverURLs = new Vector<URL>(); //List of URLs of other machines
	
	
	
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
	
	@Override
	public void run() {
		System.out.println("Client running");
		//long startTime = 0;
		
		//Do we even need this function?
	}
	
	public void join(String newNodeIP){
		//TODO: Add a check if we are trying to connect to our own IP (newNode.compate(nodeIPnPort))
		
		//Current Problem:
		//While connecting to another node
		//The current PC add's itself to the list.
		//Find why :(
		
		//After the connection has been established the destination "serverURLs" contains 2 machines
		//The local "serverURLs" has 3. (+ Copy of ourselfs)
		try{
			config.setServerURL(new URL(getFullAddress(newNodeIP)));
			xmlRpcClient.setConfig(config);
			params.removeAllElements();
			params.add(getFullAddress(nodeIPnPort));
			try{
				
				Object[] result  = (Object[]) xmlRpcClient.execute("Node.join", params);
				for (Object obj : result){
					String temp = (String) obj;
					if(Server.connectedNodes.add(temp)){
						serverURLs.add(new URL(getFullAddress(temp)));
					}
				}
				System.out.println("[Client] Connected !");
				
				//Probably create an external joinOverRPC() function
				/*for(int i=0;i<serverURLs.size();i++){
					config.setServerURL(serverURLs.get(i));
					xmlRpcClient.setConfig(config);
					xmlRpcClient.execute("Node.join", params);
				}*/
				
			}catch(XmlRpcException err){
				System.err.println(err.getMessage());
			}
		
		}catch(MalformedURLException e){
			e.printStackTrace();
			System.err.println("Wrong machine address!");
		}
	}
	
	public void listOfNodes() {
		if(serverURLs.size() > 0){
			System.out.println("There are " + serverURLs.size()
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
	
	public static String urlFormatter(String ipAndPort) {
		return "http://".concat(ipAndPort).concat("/xmlrpc");
	}

}
