import org.apache.xmlrpc.XmlRpcException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Vector;

class Bully {


    public static boolean startElection(int ownPort, HashSet<String> connectedNodes) {
        String response;

        System.out.println("node " + ownPort + " starts election process!");
        for (String node : connectedNodes) {
            if(extractPortFromIPnPort(node) <= ownPort) continue;
            response = messageNode(ownPort, node);

            switch (response) {
                case "Continue" :
                    //TODO: implement
                    break;
                case "Stop" :
                    System.out.println(node + " is taking over the election process");
                    return false;
                case "Lost" :
                    signOffDisconnectedNode(node);
                    break;
                default :
                    System.err.println("Something went wrong!");
            }
        }
        return true;
    }

    private static String messageNode(int ownPort, String node) {

        Vector<Object> params = new Vector<>();
        System.out.println("sending election message to: " + node);
        try {
            Client.config.setServerURL(new URL(Client.getFullAddress(Client.urlFormatter(node))));
            Client.xmlRpcClient.setConfig(Client.config);
            params.removeAllElements();
            params.add(ownPort);

            return (String) Client.xmlRpcClient.execute("Node.rpcElectionRequest", params);

        } catch (XmlRpcException e) {
            e.printStackTrace();
            return "Lost";
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "Error";
        }
    }

    private static boolean signOffDisconnectedNode(String node) {
        Vector<Object> params = new Vector<>();

        try {
            Client.config.setServerURL(new URL(Client.getFullAddress(Client.urlFormatter(Client.nodeIPnPort))));

            Client.xmlRpcClient.setConfig(Client.config);
            params.removeAllElements();
            params.add(node);

            return (boolean) Client.xmlRpcClient.execute("Node.signOff", params);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (XmlRpcException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static int extractPortFromIPnPort(String nodeIPnPort) {
        int port;
        String portString = nodeIPnPort.substring(nodeIPnPort.indexOf(":")+1);
        port = Integer.parseInt(portString);

        return port;
    }

}
