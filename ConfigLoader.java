import java.io.*;
import java.util.*;

public class ConfigLoader {
    // Define the name of the configuration file that contains node details
    private static final String CONFIG_FILE = "nodes.config";

    // A HashMap to store node information, where the key is the node ID, and the value is NodeInfo (IP & port)
    private Map<Integer, NodeInfo> nodes = new HashMap<>();

    // Nested class to store node details (IP address and port number)
    public class NodeInfo {
        String ip;
        int port;

        
        public NodeInfo(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
    }

    // Constructor that loads the configuration file when an instance of ConfigLoader is created
    public ConfigLoader() {
        loadConfig();
    }

    /**
     * Reads the configuration file and populates the 'nodes' map.
     * Expected format in nodes.config:
     *   node_id IP_address Port
     */
    private void loadConfig() {
        try (BufferedReader br = new BufferedReader(new FileReader(CONFIG_FILE))) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim(); // Remove any whitespace

                // Skip empty lines and comments (lines that start with '#')
                if (line.isEmpty() || line.startsWith("#")) continue;

                // Split the line by whitespace into three parts (node_id, IP address, port)
                String[] parts = line.split("\\s+");

                // Ensure the line has exactly 3 parts (valid format)
                if (parts.length != 3) continue; 

                try {
                    int nodeId = Integer.parseInt(parts[0]); // Parse node ID
                    String ip = parts[1]; // Extract IP address
                    int port = Integer.parseInt(parts[2]); // Parse port number

                    // Add the parsed data to the nodes map
                    nodes.put(nodeId, new NodeInfo(ip, port));
                } catch (NumberFormatException e) {
                    // Handle invalid number format 
                    System.err.println("Invalid format in config file: " + line);
                }
            }
        } catch (IOException e) {
            // Print an error message if the file cannot be read
            System.err.println("Error reading config file: " + e.getMessage());
        }
    }

    /**
     * Returns the map containing all loaded nodes.
     */
    public Map<Integer, NodeInfo> getNodes() {
        return nodes;
    }

    /**
     * Prints all loaded nodes to the console for debugging.
     */
    public void printNodes() {
        if (nodes.isEmpty()) {
            System.out.println("No nodes found in the configuration file.");
        } else {
            System.out.println("Loaded nodes from config file:");
            for (Map.Entry<Integer, NodeInfo> entry : nodes.entrySet()) {
                System.out.println("Node ID: " + entry.getKey() + 
                                   ", IP: " + entry.getValue().ip + 
                                   ", Port: " + entry.getValue().port);
            }
        }
    }

    
    public static void main(String[] args) {
        ConfigLoader loader = new ConfigLoader();
        loader.printNodes(); // Print the loaded node details
    }
}
