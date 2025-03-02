import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class UDPPeerMod extends Thread {
    private DatagramSocket socket = null;
    private ConfigLoader configLoader;
    private Map<Integer, Boolean> activeNodes = new HashMap<>(); // Track active nodes

    public UDPPeerMod() {
        try {
            // Load node configurations
            configLoader = new ConfigLoader();

            // Start listening on the defined port
            socket = new DatagramSocket(9876);

            // Mark all nodes as inactive initially
            for (Integer nodeId : configLoader.getNodes().keySet()) {
                activeNodes.put(nodeId, false);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void createAndListenSocket() {
        try {
            byte[] incomingData = new byte[1024];

            while (true) {
                DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                System.out.println("Waiting for messages...");

                socket.receive(incomingPacket);

                // Retrieve message and sender details
                String message = new String(incomingPacket.getData()).trim();
                InetAddress senderAddress = incomingPacket.getAddress();
                int senderPort = incomingPacket.getPort();

                System.out.println("Received message: " + message);
                System.out.println("From: " + senderAddress + ":" + senderPort);

                // Update active nodes
                for (Map.Entry<Integer, ConfigLoader.NodeInfo> entry : configLoader.getNodes().entrySet()) {
                    if (entry.getValue().ip.equals(senderAddress.getHostAddress()) && entry.getValue().port == senderPort) {
                        activeNodes.put(entry.getKey(), true);
                        break;
                    }
                }

                // Terminate if message is "THEEND"
                if (message.equals("THEEND")) {
                    socket.close();
                    break;
                }

                // Send acknowledgment
                String reply = "ACK from " + socket.getLocalPort();
                byte[] data = reply.getBytes();
                DatagramPacket replyPacket = new DatagramPacket(data, data.length, senderAddress, senderPort);
                socket.send(replyPacket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        Random rand = new Random();
        int seconds = rand.nextInt(5, 10);

        while (true) {
            try {
                TimeUnit.SECONDS.sleep(seconds);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            pingNodes();
            seconds = rand.nextInt(5, 31);
        }
    }

    // Ping all known nodes from the config file
    public void pingNodes() {
        try {
            DatagramSocket pingSocket = new DatagramSocket();

            for (Map.Entry<Integer, ConfigLoader.NodeInfo> entry : configLoader.getNodes().entrySet()) {
                String ip = entry.getValue().ip;
                int port = entry.getValue().port;

                String message = "PING from " + socket.getLocalPort();
                byte[] data = message.getBytes();

                DatagramPacket sendPacket = new DatagramPacket(data, data.length, InetAddress.getByName(ip), port);
                pingSocket.send(sendPacket);
                System.out.println("Sent PING to " + ip + ":" + port);
            }
            pingSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Periodically check active nodes
    public void monitorNodes() {
        new Thread(() -> {
            while (true) {
                try {
                    System.out.println("Current Active Nodes:");
                    for (Map.Entry<Integer, Boolean> entry : activeNodes.entrySet()) {
                        System.out.println("Node " + entry.getKey() + " -> " + (entry.getValue() ? "ONLINE" : "OFFLINE"));
                    }
                    Thread.sleep(10000); // Print every 10 sec
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        UDPPeer peer = new UDPPeer();
        peer.start(); // Start the heartbeat ping process
        peer.createAndListenSocket(); // Start listening for UDP messages
        peer.monitorNodes(); // Monitor active nodes
    }
}

