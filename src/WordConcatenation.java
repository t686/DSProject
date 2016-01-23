import org.apache.xmlrpc.XmlRpcException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

/*
 * Class specifically for:
 * b) Read the string variable from the master node
 * c) Append some random English word to this string
 * d) Write the updated string to the master node
 */
public class WordConcatenation {

    private ArrayList<String> addedStrings;

    /**
     * Method to request the current host string and add the parameter rndString to it
     * @param rndString
     * @return true if no errors occurred
     */
    public boolean concatString(String rndString) {
        Vector<Object> params = new Vector<>();

        try {
            Client.config.setServerURL(new URL(Client.getFullAddress(Client.urlFormatter(Server.host))));
            Client.xmlRpcClient.setConfig(Client.config);
            params.removeAllElements();

            try {
                System.out.println("[WordConcat] Requesting host string");
                String hostString = (String) Client.xmlRpcClient.execute("Node.rpcRequestString", params);
                hostString.concat(rndString);
                params.removeAllElements();
                params.add(hostString);
                System.out.println("[WordConcat] Sending new string to host");
                boolean response = (boolean) Client.xmlRpcClient.execute("Node.rpcOverrideString", params);
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
     * @return
     */
    public boolean checkAddedWords() throws XmlRpcException {
        Vector<Object> params = new Vector<>();
        params.removeAllElements();
        String hostString = (String) Client.xmlRpcClient.execute("Node.rpcRequestString", params);

        for (String addedWord : addedStrings) {
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
    private void clearList() {
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
        String rndString = "";
        //TODO: get the String from file
        return rndString;
    }

}
