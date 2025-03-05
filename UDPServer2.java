import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPServer2 {
    private DatagramSocket socket;
    private ExecutorService executor;
    private ConfigLoader configLoader;
    private SecureRandom random = new SecureRandom();
    private Map<Integer, Long> lastReceivedTime = new HashMap<>(); // Track last packet time

    private static final int TIMEOUT_MS = 30 * 1000; // 30 seconds timeout

    public UDPServer2() {
        try {
            socket = new DatagramSocket(9876);
            executor = Executors.newFixedThreadPool(3); // 3 threads: listener, broadcaster, timeout checker
            configLoader = new ConfigLoader();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        oos.flush();
        return bos.toByteArray();
    }

    private Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return ois.readObject();
    }

    public void createAndListenSocket() {
        // **Packet Listener Task**
        Runnable listenerTask = () -> {
            try {
                while (true) {
                    byte[] buffer = new byte[4096];
                    DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(incomingPacket);

                    // Extract only the valid received bytes
                    byte[] receivedData = Arrays.copyOfRange(incomingPacket.getData(), 0, incomingPacket.getLength());

                    Object receivedObject = deserialize(receivedData);

                    if (receivedObject instanceof Packet) {
                        Packet packet = (Packet) receivedObject;
                        int nodeId = packet.getNodeId();
                        String files = packet.getData();
                        int dataSize = packet.getDataLength();
                        String status = (dataSize > 0) ? "Online" : "Offline";

                        // Update ConfigLoader with new node info
                        configLoader.setNodeFiles(nodeId, Arrays.asList(files.split(",")));
                        configLoader.setNodeStatus(nodeId, status);

                        // **Update last received time for this node**
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

        // **Node Timeout Checker Task**
        Runnable timeoutCheckerTask = () -> {
            try {
                while (true) {
                    long currentTime = System.currentTimeMillis();
                    for (int nodeId = 1; nodeId <= 5; nodeId++) { // Ensure we check ALL nodes 1-5
                        Long lastTime = lastReceivedTime.get(nodeId);
        
                        // If node has NEVER sent a packet, assume it's Offline
                        if (lastTime == null) {
                            configLoader.setNodeStatus(nodeId, "Offline");
                            // **for debug** System.out.println("Node " + nodeId + " has never sent a packet. Marking as Offline.");
                            continue; // Skip further checks
                        }
        
                        // If 30 seconds passed since last packet, mark Offline
                        if (currentTime - lastTime > TIMEOUT_MS) {
                            configLoader.setNodeStatus(nodeId, "Offline");
                           // **for debug** System.out.println("Node " + nodeId + " has timed out and is now Offline.");
                        }
                    }
                    Thread.sleep(5000); // Check every 5 seconds
                }
            } catch (InterruptedException e) {
                System.err.println("Timeout checker interrupted.");
                Thread.currentThread().interrupt();
            }
        };

        // **Broadcaster Task (Sends Node List to Clients)**
        Runnable broadcasterTask = () -> {
            try {
                while (true) {
                    Map<Integer, ConfigLoader.NodeInfo> nodes = configLoader.getNodes();
                    List<Packet> packetList = new ArrayList<>();
                    byte version = 1;
        
                    for (int nodeId = 1; nodeId <= 5; nodeId++) { 
                        ConfigLoader.NodeInfo node = nodes.get(nodeId);
                        if (node == null) continue;
        
                        // Embed status in the `data` field using a delimiter
                        String fileListWithStatus = node.status + "|" + String.join(",", node.files);
                        int dataLength = fileListWithStatus.length();
        
                        Packet packet = new Packet(version, nodeId, dataLength, fileListWithStatus);
                        packetList.add(packet);
                    }
        
                    // Serialize list of packets (now including status inside `data`)
                    byte[] data = serialize(packetList);
        
                    // Send the updated list to all nodes 1-5
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
        
                    // Wait before next broadcast
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

