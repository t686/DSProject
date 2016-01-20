import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

public class Server implements Runnable{
	
	public static final int port = 1111;
	
	public static HashSet<String> connectedNodes = new HashSet<String>(); //List of active Nodes IPs
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
		System.out.println("[Server] Node "+nodeIP+" is leaving the network.");
		if(connectedNodes.remove(nodeIP)){//TODO: Issue 
			for(int i=0; i<Client.serverURLs.size(); i++){
				URL url = Client.serverURLs.get(i);
				if(url.toString().compareTo(Client.urlFormatter(nodeIP)) == 0){
					Client.serverURLs.remove(i);
				}
			}
			System.out.println("[Server] ConndectedNodes.size: " + connectedNodes.size() + ", ServerURLs.size: " + Client.serverURLs.size());
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
}
