/**
 * ConfigLoader is responsible for loading and managing node configuration data
 * from a configuration file. It provides functionality to retrieve, update, and 
 * display node information.
 * 
 * @author Omar Fofana
 */
import java.io.*;
import java.util.*;

public class ConfigLoader {
    private static final String CONFIG_FILE = "nodes.config";
    private Map<Integer, NodeInfo> nodes = new HashMap<>();

    /**
     * NodeInfo class stores information about a node, including IP address, port,
     * status, and a list of associated files.
     */
    public static class NodeInfo {
        String ip;
        int port;
        String status;
        List<String> files;

        /**
         * Constructs a NodeInfo object.
         * 
         * @param ip     The IP address of the node
         * @param port   The port number the node is using
         * @param status The current status of the node
         * @param files  A list of files associated with the node
         */
        public NodeInfo(String ip, int port, String status, List<String> files) {
            this.ip = ip;
            this.port = port;
            this.status = status;
            this.files = files;
        }

        /**
         * Updates the status of the node.
         * 
         * @param status The new status to set
         */
        public void setStatus(String status) {
            this.status = status;
        }

        /**
         * Updates the list of files associated with the node.
         * 
         * @param files The new list of files to set
         */
        public void setFiles(List<String> files) {
            this.files = files;
        }
    }

    /**
     * Constructs a ConfigLoader instance and loads the configuration file.
     */
    public ConfigLoader() {
        loadConfig();
    }

    /**
     * Loads node configuration data from the configuration file.
     */
    private void loadConfig() {
        try (BufferedReader br = new BufferedReader(new FileReader(CONFIG_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+", 5);
                if (parts.length < 4) {
                    System.err.println("Invalid line format: " + line);
                    continue;
                }

                try {
                    int nodeId = Integer.parseInt(parts[0]);
                    String ip = parts[1];
                    int port = Integer.parseInt(parts[2]);
                    String status = parts[3];
                    List<String> files = (parts.length == 5) ? Arrays.asList(parts[4].split(",")) : new ArrayList<>();

                    nodes.put(nodeId, new NodeInfo(ip, port, status, files));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number format in config: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading config file: " + e.getMessage());
        }
    }

    /**
     * Retrieves the map of nodes loaded from the configuration file.
     * 
     * @return A map of node IDs to NodeInfo objects
     */
    public Map<Integer, NodeInfo> getNodes() {
        return nodes;
    }

    /**
     * Updates the status of a specific node.
     * 
     * @param nodeId    The ID of the node to update
     * @param newStatus The new status to set for the node
     */
    public void setNodeStatus(int nodeId, String newStatus) {
        NodeInfo node = nodes.get(nodeId);
        if (node != null) {
            node.setStatus(newStatus);
        } else {
            System.err.println("Node ID " + nodeId + " not found.");
        }
    }

    /**
     * Updates the list of files associated with a specific node.
     * 
     * @param nodeId   The ID of the node to update
     * @param newFiles The new list of files to associate with the node
     */
    public void setNodeFiles(int nodeId, List<String> newFiles) {
        NodeInfo node = nodes.get(nodeId);
        if (node != null) {
            node.setFiles(newFiles);
            System.out.println("Updated Node " + nodeId + " files to: " + newFiles);
        } else {
            System.err.println("Node ID " + nodeId + " not found.");
        }
    }

    /**
     * Prints all nodes loaded from the configuration file.
     */
    public void printNodes() {
        if (nodes.isEmpty()) {
            System.out.println("No nodes found in the configuration file.");
        } else {
            System.out.println("Loaded nodes from config file:");
            for (Map.Entry<Integer, NodeInfo> entry : nodes.entrySet()) {
                NodeInfo node = entry.getValue();
                System.out.println("Node ID: " + entry.getKey() +
                                   ", IP: " + node.ip +
                                   ", Port: " + node.port +
                                   ", Status: " + node.status +
                                   ", Files: " + (node.files.isEmpty() ? "None" : String.join(", ", node.files)));
            }
        }
    }

    /**
     * Main method to test the ConfigLoader class.
     * 
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        ConfigLoader loader = new ConfigLoader();
        loader.printNodes();

        // Example usage of setters:
        loader.setNodeStatus(1, "Offline");
        loader.setNodeFiles(1, Arrays.asList("newfile1.txt", "newfile2.pdf"));
        
        // Print nodes again to see updates
        loader.printNodes();
    }
}
