
/**
 * This class is an implementation of a UDP Peer to Peer network.
 * 
 *@author Jacob Levin
 *@version 1.0
*/
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UDPPeer{
    private DatagramSocket socket = null;
    private ExecutorService executor;
    private ConfigLoader configLoader = new ConfigLoader();
    private int nodeId; //the node id of the computer this is running on
    /**
     * This function builds an instance of UDPPeer.
     * 
     * @param nodeId This is the node ID the user inputs corresponding to the config file
     * @param nodeInfo This is the node information from the hash map in ConfigLoader
     * @throws SocketException if the socket fails to be made
     */
    public UDPPeer(int nodeId, ConfigLoader.NodeInfo nodeInfo){
    	try{
            this.nodeId = nodeId;
    		//create the socket assuming the server is listening on port 9876
			socket = new DatagramSocket(nodeInfo.port);
            //make a pool of 3 threads
            executor = Executors.newFixedThreadPool(3);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /**
     * This function serializes outgoing data.
     * 
     * @param obj the object that is to be serialized
     * @return The object converted into a byte array
     * @throws IOException when the object fails to serialize
     */
    private byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        oos.flush();
        return bos.toByteArray();
    }

    /**
     * This function deserializes incoming data.
     * 
     * @param data the byte array that is to be turned back into an object
     * @return The byte array converted back into an object
     * @throws IOException for any of the usual input/output exceptions
     * @throws ClassNotFoundException when the class of a serialized object cannot be found
     */
    private Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return ois.readObject();
    }

    /**
     * This function creats and runs the threads of the sending, listening and timer functionalities of the peer
     * 
     * @throws IOException for any of the usual input/output exceptions
     * @throws ClassNotFoundException when the class of a serialized object cannot be found
     * @throws UnknownHostException when the IP address of a host could not be determined
     * @throws SocketException if the socket fails to be made
     * @throws InterruptedException if any thread has interrupted the current thread
     */
    public void createAndListenSocket() {
        //listens for incoming packets
        Runnable listenerTask = () -> {
            try {
                byte[] incomingData = new byte[1024];
                while (true) {
                    //receive any incoming packets
                    DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                    System.out.println("Listening...");
                    socket.receive(incomingPacket);

                    //extract the custom data structure form the incoming packet
                    Packet packet = (Packet) deserialize(incomingPacket.getData());

                    //set the status and files in the hashmap to the corresponding node
                    System.out.println("Received message from client: " + packet);
                    System.out.println("Client Details: PORT " + incomingPacket.getPort()
                            + ", IP Address: " + incomingPacket.getAddress()
                            + ", File Listing: " + packet.getData());
                    configLoader.setNodeStatus(packet.getNodeId(), "Online");
                    configLoader.setNodeFiles(packet.getNodeId(), Arrays.asList(packet.getData().split(",")));
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        };

        //sends to each other peer a packet after every 0-30 secs
        Runnable sendTask = () -> {
            SecureRandom rand = new SecureRandom();
            int seconds = rand.nextInt(0,31);
            while(true){
                //wait 0-30 seconds
                try {
                    TimeUnit.SECONDS.sleep(seconds);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    //send to a packet with corresponding files to each other peer
                    for(int i=1; i <= configLoader.getNodes().size(); i++){
                        if(i != this.nodeId){
                            //define variables
                            Packet packet;
                            byte version = 0;
                            String fileList = String.join(",", configLoader.getNodes().get(nodeId).files);
                            int dataLength = fileList.length();

                            packet = new Packet(version, this.nodeId, dataLength, fileList);
                            byte[] data = serialize(packet); //serialize the packet
                            DatagramSocket Socket = new DatagramSocket();
                            InetAddress IPAddress = InetAddress.getByName(configLoader.getNodes().get(i).ip); //get ip from hashmap
                            DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, configLoader.getNodes().get(i).port); //put the custom data structure in the datagramsocket
                            Socket.send(sendPacket);
                            System.out.println("Message sent to peer " + i);
                            Socket.close();
                        }
                    }
                }
                catch (UnknownHostException e) {
                    e.printStackTrace();
                } 
                catch (SocketException e) {
                    e.printStackTrace();
                } 
                catch (IOException e) {
                    e.printStackTrace();
                }
                seconds = rand.nextInt(0,31);//randomly generate next interval
            }
        };

        Runnable heartbeatTask = () -> {
            //timer for each other peer
            try {
                while (true) {
                    
                    //set all node statuses in hashmap to Offline
                    for(int i=1; i <= configLoader.getNodes().size(); i++){
                        if(i != this.nodeId){
                            configLoader.setNodeStatus(i, "Offline");
                        }else{
                            configLoader.setNodeStatus(i, "Online");
                        }
                    }
                    Thread.sleep(30000);
                    for(int i=1; i <= configLoader.getNodes().size(); i++){
                        System.out.println("Server " + i + ": " + configLoader.getNodes().get(i).status);
                    }
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        };
        //execute runnables
        executor.execute(heartbeatTask);
        executor.execute(listenerTask);
        executor.execute(sendTask);
        executor.shutdown();
        
    }

    /**
     * The function that runs first
     * 
     * @param args
     * @throws NumberFormatException for if the user inputs a non-number
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ConfigLoader configLoader = new ConfigLoader();
        int nodeId;

        //prompts user for a node ID that is between 1 and 6
        while (true) {
            System.out.print("Enter a Node ID (1-6): ");
            try {
                nodeId = Integer.parseInt(scanner.nextLine().trim());
                if (nodeId >= 1 && nodeId <= 6) {
                    break;
                } else {
                    System.err.println("Invalid Node ID! Please enter a number between 1 and 6.");
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid input! Please enter a number between 1 and 6.");
            }
        }

        // Retrieve node configuration based on user input
        ConfigLoader.NodeInfo nodeInfo = configLoader.getNodes().get(nodeId);
        if (nodeInfo == null) {
            System.err.println("Error: No configuration found for Node " + nodeId);
            return;
        }

        // Create and start the UDP client
        UDPPeer server = new UDPPeer(nodeId, nodeInfo);
        server.createAndListenSocket();
    }
}
