import org.apache.xmlrpc.XmlRpcException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Vector;

class Bully {

    private int ownPort;
    private String nodeIPnPort;
    private HashSet<String> connectedNodes;

    public Vector<Object> params = new Vector<Object>();

    public Bully(String nodeIPnPort, HashSet<String> connectedNodes) {
        this.nodeIPnPort = nodeIPnPort;
        this.connectedNodes = connectedNodes;
        this.ownPort = extractPortFromIPnPort(nodeIPnPort);

    }

    public boolean startElection() {
        String response;

        System.out.println("node " + ownPort + " starts election process!");
        for (String node : connectedNodes) {
            if(extractPortFromIPnPort(node) <= ownPort) continue;
            response = messageNode(node);

            switch (response) {
                case "Continue" :
                    //TODO: implement
                    break;
                case "Stop" :
                    //TODO: implement
                    break;
                case "Lost" :
                    //TODO: implement
                    break;
                default :
                    System.err.println("Something went wrong!");
            }
        }
        return true;
    }

    private String messageNode(String node) {

        try {
            Client.config.setServerURL(new URL(Client.getFullAddress(Client.urlFormatter(node))));
            Client.xmlRpcClient.setConfig(Client.config);
            params.removeAllElements();
            params.add(ownPort);

            String result  = (String) Client.xmlRpcClient.execute("Node.requestElection", params);
            return result;

        } catch (XmlRpcException e) {
            e.printStackTrace();
            return "Lost";
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "Error";
        }
    }

    private int extractPortFromIPnPort(String nodeIPnPort) {
        int port = 0;
        String portString = nodeIPnPort.substring(nodeIPnPort.indexOf(":"));
        port = Integer.parseInt(portString);

        return port;
    }

}
