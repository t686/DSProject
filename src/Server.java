import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

public class Server implements Runnable{
	
	//public static final int port = findFreePort();
	public static final int port = findFreePort(); //For easier test results analysis

	public static String host = "none";
	
	public static HashSet<String> connectedNodes = new HashSet<>(); //List of active Nodes IPs
	//Using HashSet we eliminate the probability of identical elements on a data structure level

	@Override
	//Start the WebServer
	public void run() {
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
			return true;
		}
		return false;

	}
	
	public static void listOfConnections() {
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

	public boolean startElection() {
		if(Bully.startElection(port, connectedNodes)) {
			host = Client.nodeIPnPort;
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

		System.out.println("received a election message from: " + requester);

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
}
