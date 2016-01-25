import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Vector;
import java.util.concurrent.ThreadLocalRandom;

/*
 * Class specifically for:
 * b) Read the string variable from the master node
 * c) Append some random English word to this string
 * d) Write the updated string to the master node
 */
public class WordConcatenation {

    private static ArrayList<String> rndWordSet = new ArrayList<>();
    private static ArrayList<String> addedStrings;

    private XmlRpcClient xmlRpcClient;
    private XmlRpcClientConfigImpl config;

    public WordConcatenation() {
        System.out.println("OH SHIT!!!");
        addedStrings = new ArrayList<>();
        this.config = new XmlRpcClientConfigImpl();
        this.xmlRpcClient = new XmlRpcClient();
    }

    /**
     * Method to request the current host string and add the parameter rndString to it
     * @return true if no errors occurred
     */
    public boolean concatString() {
        Vector<Object> params = new Vector<>();
        String rndString = getRndString();

        try {
            this.config.setServerURL(new URL(Client.formatAddress(Server.host)));
            this.xmlRpcClient.setConfig(this.config);
            params.removeAllElements();

            try {
                System.out.println("[WordConcat] Requesting host string");
                String hostString = (String) this.xmlRpcClient.execute("Node.rpcRequestString", params);
                hostString = hostString.concat(rndString);
                addedStrings.add(rndString);

                params.removeAllElements();
                params.add(hostString);
                System.out.println("[WordConcat] Sending new string to host");
                boolean response = (boolean) this.xmlRpcClient.execute("Node.rpcOverrideString", params);
                return response;
            } catch (XmlRpcException e) {
                e.printStackTrace();
                return false;
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Method to check if all strings which were added in the process are part of the result string
     */
    public boolean checkAddedWords() throws XmlRpcException {
        Vector<Object> params = new Vector<>();
        params.removeAllElements();
        String hostString = (String) this.xmlRpcClient.execute("Node.rpcRequestString", params);
        System.out.println(addedStrings.size() + " Strings were added!");
        for (String addedWord : addedStrings) {
            System.out.println(addedWord);
            if(hostString.contains(addedWord)) continue;

            System.out.println("[WordConcat] The host strong does not contain " + addedWord);
            return false;
        }
        System.out.println("[WordConcat] concatenation complete");
        return true;
    }

    /**
     *  Method to clear the list of added strings for the next process
     */
    public void clearList() {
        System.out.println("addedStrings cleared");
        addedStrings.clear();
    }

    /**
     * Method returns all Strings which where added by the current instance.
     * Probably not needed
     * @return all strings which where added by current instance
     */
    public ArrayList<String> getAddedStrings() {
        return addedStrings;
    }

    private String getRndString() {

        int rndInt = ThreadLocalRandom.current().nextInt(0, 999);
        String rndString = rndWordSet.get(rndInt);
        return rndString;
    }

    public void setWordSet(ArrayList<String> wordSet) {
        this.rndWordSet = wordSet;
    }

}
