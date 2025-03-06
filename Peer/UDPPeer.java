import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
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
    private int nodeId;
    public UDPPeer(){
    	try{
    		//create the socket assuming the server is listening on port 9876
			socket = new DatagramSocket(9876);
            executor = Executors.newFixedThreadPool(3);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        System.out.println("Input this node's ID: ");
        Scanner scan = new Scanner(System.in);
        nodeId = scan.nextInt();
        scan.close();
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
        Runnable listenerTask = () -> {
            try {
                byte[] incomingData = new byte[1024];
                while (true) {
                    DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                    System.out.println("Waiting...");
                    socket.receive(incomingPacket);

                    Packet packet = (Packet) deserialize(incomingPacket.getData());
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

        Runnable sendTask = () -> {
            SecureRandom rand = new SecureRandom();
            int seconds = rand.nextInt(1,31);
            while(true){
                try {
                    TimeUnit.SECONDS.sleep(seconds);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ping(); //change to communicate to each peer
                seconds = rand.nextInt(1,31);
            }
        };

        Runnable heartbeatTask = () -> {
            //timer for each other peer
            try {
                while (true) {
                    
                    //set all nodes in hashmap to false
                    for(int i=1; i <= configLoader.getNodes().size(); i++){
                        configLoader.setNodeStatus(i, "Offline");
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
        executor.execute(heartbeatTask);
        executor.execute(listenerTask);
        executor.execute(sendTask);
        executor.shutdown();
        
    }

    public void ping(){
    try 
        {
            for(int i=1; i <= configLoader.getNodes().size(); i++){
                Packet packet;

                byte version = 0;
                packet = new Packet(version, this.nodeId, 0, configLoader.getNodes().get(nodeId).files.toString());
                byte[] data = serialize(packet);
                DatagramSocket Socket = new DatagramSocket();
                InetAddress IPAddress = InetAddress.getByName(configLoader.getNodes().get(i).ip);
                DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, configLoader.getNodes().get(i).port);
                Socket.send(sendPacket);
                System.out.println("Message sent to peer " + i);
                Socket.close();
            }
            
        }
        catch (UnknownHostException e) 
        {
            e.printStackTrace();
        } 
        catch (SocketException e) 
        {
            e.printStackTrace();
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws InterruptedException 
    {
        UDPPeer server = new UDPPeer();
        server.createAndListenSocket();
    }
    public void monitorNodes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'monitorNodes'");
    }
}
