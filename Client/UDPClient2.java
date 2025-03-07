//package networking;

// Author :Ethan Kulawiak 3/6/2025

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;
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
     * Constructor to initialize the UDP client.
     * @param nodeId The ID of this node.
     * @param nodeInfo Configuration details for this node.
     */
    public UDPClient2(int nodeId, ConfigLoader.NodeInfo nodeInfo) {
        try {
            this.nodeId = nodeId;
            socket = new DatagramSocket(nodeInfo.port); // Bind to the specified port
            executor = Executors.newFixedThreadPool(2); // Use two threads for handling tasks
            serverAddress = InetAddress.getByName("10.111.163.55"); // Server IP address
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts an object into a byte array for sending over UDP.
     */
    private byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        oos.flush();
        return bos.toByteArray();
    }

    /**
     * Converts a byte array back into an object.
     */
    private Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return ois.readObject();
    }

    /**
     * Starts the client, handling both sending and receiving of messages.
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

                    // Wait between 1 and 30 seconds before sending again
                    int delay = 1 + random.nextInt(30);
                    System.out.println("Next send in " + delay + " seconds.");

                    try {
                        Thread.sleep(delay * 1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
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
                    socket.receive(incomingPacket); // Receive incoming data

                    Object receivedObject = deserialize(incomingPacket.getData());

                    // Process received data if it contains a list of packets
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

        // Start both sender and receiver tasks
        executor.execute(senderTask);
        executor.execute(receiverTask);
        executor.shutdown();
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ConfigLoader configLoader = new ConfigLoader();
        int nodeId;

        // Prompt user for a node ID (must be between 1 and 5)
        while (true) {
            System.out.print("Enter a Node ID (1-5): ");
            try {
                nodeId = Integer.parseInt(scanner.nextLine().trim());
                if (nodeId >= 1 && nodeId <= 5) {
                    break;
                } else {
                    System.err.println("Invalid Node ID! Please enter a number between 1 and 5.");
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid input! Please enter a number between 1 and 5.");
            }
        }

        // Retrieve node configuration based on user input
        ConfigLoader.NodeInfo nodeInfo = configLoader.getNodes().get(nodeId);
        if (nodeInfo == null) {
            System.err.println("Error: No configuration found for Node " + nodeId);
            return;
        }

        // Create and start the UDP client
        UDPClient2 client = new UDPClient2(nodeId, nodeInfo);
        client.createAndListenSocket(nodeInfo);
    }
}

