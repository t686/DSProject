import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

public class Server implements Runnable{
	
	public static final int port = 3333;
	
	public static ArrayList<String> connectedNodes = new ArrayList<String>(); //List of active Nodes IPs

	@Override
	//Start the WebServer
	public void run() {
		System.out.println("Server starting...");
		
		try{
			connectedNodes.add(Client.nodeIPnPort);
			Client.serverURLs.add(new URL(Client.getFullAddress(Client.nodeIPnPort)));
			
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
				System.out.println("[Server] New node with address: "+newNodeIP+" was connected!");
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}		
		
		return connectedNodes.toArray();
	}
	
	public void signOff(String nodeIP){
		if(connectedNodes.remove(nodeIP)){
			System.out.println("Node "+nodeIP+" is leaving the network.");
			
			for(int i=0;i<Client.serverURLs.size();i++){
				URL url = Client.serverURLs.get(i);
				if(url.toString().compareTo(nodeIP) == 0)
					Client.serverURLs.remove(i);
			}
		}
	}
}
