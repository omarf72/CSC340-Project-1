/**
 * UDPServer2 is a UDP-based server that listens for packets from nodes,
 * updates their statuses, checks for timeouts, and broadcasts node lists
 * to all clients.
 * 
 * <p>It uses multiple threads to handle packet reception, timeout checking,
 * and broadcasting of node data.</p>
 * 
 * @author Ethan Kulawiak
 * @date 3/6/2025
 */
import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPServer2 {
    private DatagramSocket socket; // UDP socket for communication
    private ExecutorService executor; // Thread pool for managing tasks
    private ConfigLoader configLoader; // Manages node configurations
    private SecureRandom random = new SecureRandom(); // For random delays in broadcasting
    private Map<Integer, Long> lastReceivedTime = new HashMap<>(); // Stores last received time for each node

    private static final int TIMEOUT_MS = 30 * 1000; // Timeout period (30 seconds)

    /**
     * Constructs a UDPServer2 instance, initializing the socket, thread pool,
     * and configuration loader.
     */
    public UDPServer2() {
        try {
            socket = new DatagramSocket(9876); // Bind server to port 9876
            executor = Executors.newFixedThreadPool(3); // Use 3 threads: listener, broadcaster, timeout checker
            configLoader = new ConfigLoader(); // Load node configurations
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /**
     * Serializes an object into a byte array for transmission.
     * 
     * @param obj the object to serialize
     * @return the serialized byte array
     * @throws IOException if an I/O error occurs
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
     * @param data the byte array to deserialize
     * @return the deserialized object
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class of the serialized object cannot be found
     */
    private Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return ois.readObject();
    }

    /**
     * Starts the UDP server and handles incoming and outgoing packets.
     * 
     * <p>This method launches three concurrent tasks:</p>
     * <ul>
     * <li>Packet Listener - Receives data from nodes.</li>
     * <li>Node Timeout Checker - Marks nodes as Offline if inactive.</li>
     * <li>Broadcaster - Sends node list updates to clients.</li>
     * </ul>
     */
    public void createAndListenSocket() {
        // **Packet Listener Task** (Receives data from nodes)
        Runnable listenerTask = () -> {
            try {
                while (true) {
                    byte[] buffer = new byte[4096]; // Buffer for incoming packets
                    DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(incomingPacket); // Wait for a packet from a node

                    Object receivedObject = deserialize(incomingPacket.getData());

                    if (receivedObject instanceof Packet) {
                        Packet packet = (Packet) receivedObject;
                        int nodeId = packet.getNodeId();
                        String files = packet.getData();
                        int dataSize = packet.getDataLength();
                        String status = (dataSize > 0) ? "Online" : "Offline";

                        // Update node info in ConfigLoader
                        configLoader.setNodeFiles(nodeId, Arrays.asList(files.split(",")));
                        configLoader.setNodeStatus(nodeId, status);

                        // Store last received timestamp for this node
                        lastReceivedTime.put(nodeId, System.currentTimeMillis());

                        System.out.println("Updated Node " + nodeId + ": Status = " + status + ", Files = " + files);
                    } else {
                        System.err.println("Invalid packet format received.");
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        };

        // **Node Timeout Checker Task** (Marks nodes Offline if inactive)
        Runnable timeoutCheckerTask = () -> {
            try {
                while (true) {
                    long currentTime = System.currentTimeMillis();
                    for (int nodeId = 1; nodeId <= 5; nodeId++) { // Check all nodes 1-5
                        Long lastTime = lastReceivedTime.get(nodeId);

                        // If node never sent a packet, assume it's Offline
                        if (lastTime == null) {
                            configLoader.setNodeStatus(nodeId, "Offline");
                            continue;
                        }

                        // If node hasn't sent data in TIMEOUT_MS, mark as Offline
                        if (currentTime - lastTime > TIMEOUT_MS) {
                            configLoader.setNodeStatus(nodeId, "Offline");
                        }
                    }
                    Thread.sleep(5000); // Check every 5 seconds
                }
            } catch (InterruptedException e) {
                System.err.println("Timeout checker interrupted.");
                Thread.currentThread().interrupt();
            }
        };

        // **Broadcaster Task** (Sends node list to clients)
        Runnable broadcasterTask = () -> {
            try {
                while (true) {
                    Map<Integer, ConfigLoader.NodeInfo> nodes = configLoader.getNodes();
                    List<Packet> packetList = new ArrayList<>();
                    byte version = 1;

                    for (int nodeId = 1; nodeId <= 6; nodeId++) { 
                        ConfigLoader.NodeInfo node = nodes.get(nodeId);
                        if (node == null) continue;

                        String fileListWithStatus = node.status + "|" + String.join(",", node.files);
                        int dataLength = fileListWithStatus.length();
                        Packet packet = new Packet(version, nodeId, dataLength, fileListWithStatus);
                        packetList.add(packet);
                    }

                    byte[] data = serialize(packetList);

                    for (int nodeId = 1; nodeId <= 5; nodeId++) {
                        ConfigLoader.NodeInfo node = nodes.get(nodeId);
                        if (node == null) continue;

                        try {
                            InetAddress nodeAddress = InetAddress.getByName(node.ip);
                            DatagramPacket sendPacket = new DatagramPacket(data, data.length, nodeAddress, node.port);
                            socket.send(sendPacket);
                        } catch (IOException e) {
                            System.err.println("Failed to send update to Node " + nodeId);
                        }
                    }
                    Thread.sleep((1 + random.nextInt(30)) * 1000);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        };

        executor.execute(listenerTask);
        executor.execute(timeoutCheckerTask);
        executor.execute(broadcasterTask);
        executor.shutdown();
    }

    public static void main(String[] args) {
        UDPServer2 server = new UDPServer2();
        server.createAndListenSocket();
    }
}

