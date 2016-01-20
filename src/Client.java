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
		
		if(serverURLs.size() > 1){
			System.err.println("You are a part of an existing network!");
		}else if(newNodeIP.equals(nodeIPnPort)){
			System.err.println("You can't connect to yourself!");
		}else{
			//System.err.println("[CLIENT] NEW NODE JOINING: "+newNodeIP);
			try{
				config.setServerURL(new URL(getFullAddress(urlFormatter(newNodeIP))));
				xmlRpcClient.setConfig(config);
				params.removeAllElements();
				params.add(nodeIPnPort);
				try{
					
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
		Vector<Object> params = new Vector<Object>();
		params.add(nodeIPnPort);
		
		if(serverURLs.size() > 1){
			URL[] a = new URL[serverURLs.size()];
			a = serverURLs.toArray(a);
			for (URL url : a) {
				config.setServerURL(url);
				xmlRpcClient.setConfig(config);
				if (!(boolean) xmlRpcClient.execute("Node.signOff", params)) {
					System.out.println("[Client] Failed to signOff from "+ url.getAuthority());
				}
			}
			serverURLs.clear();
			serverURLs.add(new URL(getFullAddress(urlFormatter(nodeIPnPort))));
			System.out.println("[Client] Signed off!");
		}else{
			System.err.println("[Client] You are not connected to a network");
		}
	}
	
	public void listOfNodes() {
		Server.listOfConnections();
		System.out.println("____");
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
}
