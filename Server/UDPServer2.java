
// Author Ethan Kulawiak 3/6/2025

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
     */
    private Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return ois.readObject();
    }

    /**
     * Starts the UDP server and handles incoming/outgoing packets.
     */
    public void createAndListenSocket() {
        // **Packet Listener Task** (Receives data from nodes)
        Runnable listenerTask = () -> {
            try {
                while (true) {
                    byte[] buffer = new byte[4096]; // Buffer for incoming packets
                    DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(incomingPacket); // Wait for a packet from a node

                    byte[] receivedData = Arrays.copyOfRange(incomingPacket.getData(), 0, incomingPacket.getLength());
                    Object receivedObject = deserialize(receivedData);

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

                        // If node hasn't sent data in 30 seconds, mark as Offline
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

                        // Include status and file list in the packet
                        String fileListWithStatus = node.status + "|" + String.join(",", node.files);
                        int dataLength = fileListWithStatus.length();
                        Packet packet = new Packet(version, nodeId, dataLength, fileListWithStatus);
                        packetList.add(packet);
                    }

                    // Serialize node list into a packet
                    byte[] data = serialize(packetList);

                    // Send updated node list to all nodes 1-5
                    for (int nodeId = 1; nodeId <= 5; nodeId++) {
                        ConfigLoader.NodeInfo node = nodes.get(nodeId);
                        if (node == null) continue;

                        try {
                            InetAddress nodeAddress = InetAddress.getByName(node.ip);
                            DatagramPacket sendPacket = new DatagramPacket(data, data.length, nodeAddress, node.port);
                            socket.send(sendPacket);
                            System.out.println("Sent updated node list to Node " + nodeId + " at " + node.ip + ":" + node.port);
                        } catch (IOException e) {
                            System.err.println("Failed to send update to Node " + nodeId);
                        }
                    }

                    // Randomized delay before next broadcast
                    int delay = 1 + random.nextInt(30);
                    System.out.println("Next broadcast in " + delay + " seconds.");
                    Thread.sleep(delay * 1000);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        };

        // Start all tasks
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

