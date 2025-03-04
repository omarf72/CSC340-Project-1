import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UDPPeer{
    private DatagramSocket socket = null;
    private ExecutorService executor;
    public UDPPeer(){
    	try{
    		//create the socket assuming the server is listening on port 9876
			socket = new DatagramSocket(9876);
            executor = Executors.newFixedThreadPool(3);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
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
        Runnable listenerTask = () -> {
            try {
                byte[] incomingData = new byte[1024];
                while (true) {
                    DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                    System.out.println("Waiting...");
                    socket.receive(incomingPacket);

                    String message = (String) deserialize(incomingPacket.getData());
                    if (message.equals("THEEND")) {
                        socket.close();
                        break;
                    }
                    System.out.println("Received message from client: " + message);
                    System.out.println("Client Details: PORT " + incomingPacket.getPort()
                            + ", IP Address: " + incomingPacket.getAddress());

                    InetAddress IPAddress = Inet4Address.getByName(incomingPacket.getAddress().getHostAddress());
                    int port = incomingPacket.getPort();
                    String reply = "Thank you for the message";
                    byte[] data = serialize(reply);
                    DatagramPacket replyPacket = new DatagramPacket(data, data.length, IPAddress, port);
                    socket.send(replyPacket);
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
            //update hashmap
            //print status of each peer
        };
        executor.execute(heartbeatTask);
        executor.execute(listenerTask);
        executor.execute(sendTask);
        executor.shutdown();
        
    }

    public void ping(){
    try 
        {
            DatagramSocket Socket;
            Socket = new DatagramSocket();
            InetAddress IPAddress = InetAddress.getByName("localhost");
            byte[] incomingData = new byte[1024];
            String sentence = "Oh wow";
            byte[] data = sentence.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, 9876);
            Socket.send(sendPacket);
            System.out.println("Message sent from client");
            DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
            Socket.receive(incomingPacket);
            String response = new String(incomingPacket.getData());
            System.out.println("Response from server:" + response);
            Socket.close();
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
