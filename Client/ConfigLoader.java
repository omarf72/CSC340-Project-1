//Author Omar Fofana
import java.io.*;
import java.util.*;

public class ConfigLoader {
    private static final String CONFIG_FILE = "nodes.config";
    private Map<Integer, NodeInfo> nodes = new HashMap<>();

    // NodeInfo class stores IP, port, status, and file list
    public static class NodeInfo {
        String ip;
        int port;
        String status;
        List<String> files;

        public NodeInfo(String ip, int port, String status, List<String> files) {
            this.ip = ip;
            this.port = port;
            this.status = status;
            this.files = files;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public void setFiles(List<String> files) {
            this.files = files;
        }
    }

    public ConfigLoader() {
        loadConfig();
    }

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

    public Map<Integer, NodeInfo> getNodes() {
        return nodes;
    }

    //  Setter to update the status of a node
    public void setNodeStatus(int nodeId, String newStatus) {
        NodeInfo node = nodes.get(nodeId);
        if (node != null) {
            node.setStatus(newStatus);
           // System.out.println("Updated Node " + nodeId + " status to " + newStatus);
        } else {
            System.err.println("Node ID " + nodeId + " not found.");
        }
    }

    //  Setter to update the file list of a node
    public void setNodeFiles(int nodeId, List<String> newFiles) {
        NodeInfo node = nodes.get(nodeId);
        if (node != null) {
            node.setFiles(newFiles);
            System.out.println("Updated Node " + nodeId + " files to: " + newFiles);
        } else {
            System.err.println("Node ID " + nodeId + " not found.");
        }
    }

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