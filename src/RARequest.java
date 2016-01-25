import java.net.URL;
import java.util.concurrent.Callable;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

//public class RARequest implements Callable<Boolean>{
public class RARequest implements Runnable{
	
	XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
	XmlRpcClient xmlRpcClient = new XmlRpcClient();
	URL url;
	RicartAgrawalaClient RAClient;
		
	public RARequest(URL url){
		this.url = url;
	}
	
//	@Override
//	public Boolean call() throws Exception {
//		config.setServerURL(url);
//		xmlRpcClient.setConfig(config);
//		try {
//			//System.out.println("Calling the Server.reveiceRequest from client with params: "+ RAClient.getParams());
//			//return (Boolean) xmlRpcClient.execute("Node.receiveRequest", RAClient.getParams());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return false;
//		
//	}
	
	@Override
	public void run(){
		config.setServerURL(url);
		xmlRpcClient.setConfig(config);
		try {
			//System.out.println("Calling the Server.reveiceRequest from client with params: "+ RAClient.getParams());
			//return (Boolean) xmlRpcClient.execute("Node.receiveRequest", RAClient.getParams());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
