//package networking;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.security.SecureRandom;

public class UDPClient2 {
    private DatagramSocket socket;
    private ExecutorService executor;
    private int nodeId;
    private InetAddress serverAddress;
    private int serverPort = 9876; // Keeping this constant as in the original code

    public UDPClient2(int nodeId, ConfigLoader.NodeInfo nodeInfo) {
        try {
            this.nodeId = nodeId;
            socket = new DatagramSocket(nodeInfo.port); // Bind to the node's port dynamically
            executor = Executors.newFixedThreadPool(2);
            serverAddress = InetAddress.getByName("192.168.56.1"); // Set server IP
        } catch (SocketException | UnknownHostException e) {
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

    public void createAndListenSocket(ConfigLoader.NodeInfo nodeInfo) {
        if (nodeInfo == null) {
            System.err.println("Node information not found.");
            return;
        }

        Runnable senderTask = () -> {
            SecureRandom random = new SecureRandom();
            byte version = 1;

            try {
                while (true) {
                    String fileList = String.join(",", nodeInfo.files);
                    int dataLength = fileList.length();
                    Packet packet = new Packet(version, nodeId, dataLength, fileList);

                    try {
                        byte[] data = serialize(packet);
                        DatagramPacket sendPacket = new DatagramPacket(data, data.length, serverAddress, serverPort);
                        socket.send(sendPacket);
                        System.out.println("Node " + nodeId + " information sent.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    int delay = 1 + random.nextInt(30);
                    System.out.println("Next send in " + delay + " seconds.");

                    try {
                        Thread.sleep(delay * 1000);
                    } catch (InterruptedException e) {
                        System.err.println("Thread sleep interrupted!");
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

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
                                String rawData = pkt.getData();
                    
                                // ✅ Extract status from `data` field (split at "|")
                                String[] parts = rawData.split("\\|", 2);
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

        ConfigLoader.NodeInfo nodeInfo = configLoader.getNodes().get(nodeId);
        if (nodeInfo == null) {
            System.err.println("Error: No configuration found for Node " + nodeId);
            return;
        }

        UDPClient2 client = new UDPClient2(nodeId, nodeInfo);
        client.createAndListenSocket(nodeInfo);
    }
}

