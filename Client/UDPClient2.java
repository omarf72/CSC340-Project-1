/**
 * UDPClient2 is a UDP-based client that communicates with a UDP server.
 * 
 * <p>This client periodically sends its node information to the server and
 * listens for broadcasts from the server containing updated node statuses.</p>
 * 
 * @author Ethan Kulawiak
 * @date 3/6/2025
 */
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.security.SecureRandom;

public class UDPClient2 {
    private DatagramSocket socket; // UDP socket for communication
    private ExecutorService executor; // Thread pool for handling sending/receiving
    private int nodeId; // Unique identifier for this node
    private InetAddress serverAddress; // Server address
    private int serverPort = 9876; // Port used for server communication

    /**
     * Constructs a UDPClient2 instance, initializing the socket and executor.
     * 
     * @param nodeId The ID of this node.
     * @param nodeInfo Configuration details for this node.
     */
    public UDPClient2(int nodeId, ConfigLoader configLoader) {
        try {
            this.nodeId = nodeId;
            ConfigLoader.NodeInfo nodeInfo = configLoader.getNodes().get(nodeId);
            ConfigLoader.NodeInfo serverNode = configLoader.getNodes().get(6); // Get Node 6 (server)
    
            if (nodeInfo == null || serverNode == null) {
                throw new IllegalArgumentException("Node information not found.");
            }

            socket = new DatagramSocket(nodeInfo.port); // Bind to the specified port
            executor = Executors.newFixedThreadPool(2); // Use two threads for handling tasks
            serverAddress = InetAddress.getByName(serverNode.ip); // Server IP address
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * Serializes an object into a byte array for sending over UDP.
     * 
     * @param obj The object to serialize.
     * @return The serialized byte array.
     * @throws IOException If an I/O error occurs during serialization.
     */
    private byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        oos.flush();
        return bos.toByteArray();
    }

    /**
     * Deserializes a byte array back into an object.
     * 
     * @param data The byte array to deserialize.
     * @return The deserialized object.
     * @throws IOException If an I/O error occurs during deserialization.
     * @throws ClassNotFoundException If the class of the serialized object cannot be found.
     */
    private Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return ois.readObject();
    }

    /**
     * Starts the UDP client, handling both sending and receiving of messages.
     * 
     * <p>The client performs two concurrent tasks:</p>
     * <ul>
     * <li>Sender Task - Sends periodic updates to the server.</li>
     * <li>Receiver Task - Listens for broadcast updates from the server.</li>
     * </ul>
     * 
     * @param nodeInfo The configuration details for this node.
     */
    public void createAndListenSocket(ConfigLoader.NodeInfo nodeInfo) {
        if (nodeInfo == null) {
            System.err.println("Node information not found.");
            return;
        }

        // Task for sending packets to the server
        Runnable senderTask = () -> {
            SecureRandom random = new SecureRandom();
            byte version = 1;

            try {
                while (true) {
                    String fileList = String.join(",", nodeInfo.files);
                    Packet packet = new Packet(version, nodeId, fileList.length(), fileList);

                    try {
                        byte[] data = serialize(packet);
                        DatagramPacket sendPacket = new DatagramPacket(data, data.length, serverAddress, serverPort);
                        socket.send(sendPacket);
                        System.out.println("Node " + nodeId + " information sent.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    int delay = 1 + random.nextInt(30); // Wait between 1 and 30 seconds
                    System.out.println("Next send in " + delay + " seconds.");
                    Thread.sleep(delay * 1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        // Task for receiving packets from the server
        Runnable receiverTask = () -> {
            try {
                while (true) {
                    byte[] incomingData = new byte[4096];
                    DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                    socket.receive(incomingPacket);

                    Object receivedObject = deserialize(incomingPacket.getData());

                    if (receivedObject instanceof List<?>) {
                        List<?> rawList = (List<?>) receivedObject;

                        if (!rawList.isEmpty() && rawList.get(0) instanceof Packet) {
                            List<Packet> packetList = (List<Packet>) rawList;
                            System.out.println("Received updated node list from server:");
                            for (Packet pkt : packetList) {
                                int id = pkt.getNodeId();
                                String[] parts = pkt.getData().split("\\|", 2);
                                String status = (parts.length > 1) ? parts[0] : "Unknown";
                                String files = (parts.length > 1) ? parts[1] : "No files";
                                System.out.println("Node " + id + ": Status = " + status + ", Files = " + files);
                            }
                        } else {
                            System.err.println("List received but does not contain Packet objects.");
                        }
                    } else {
                        System.err.println("Invalid packet format received.");
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        };

        executor.execute(senderTask);
        executor.execute(receiverTask);
        executor.shutdown();
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
    ConfigLoader configLoader = new ConfigLoader();
    int nodeId;

    // Prompt user for a Node ID
    while (true) {
        System.out.print("Enter a Node ID (1-5): ");
        try {
            nodeId = Integer.parseInt(scanner.nextLine().trim());
            if (nodeId >= 1 && nodeId <= 5) break;
            else System.err.println("Invalid Node ID! Please enter a number between 1 and 5.");
        } catch (NumberFormatException e) {
            System.err.println("Invalid input! Please enter a number between 1 and 5.");
        }
    }

    UDPClient2 client = new UDPClient2(nodeId, configLoader);
    client.createAndListenSocket(configLoader.getNodes().get(nodeId));
    }
}

