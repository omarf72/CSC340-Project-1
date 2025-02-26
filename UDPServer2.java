import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
//package networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
/**
 * 
 * @author cjaiswal
 *
 *  
 * 
 */
public class UDPServer2 {
    private DatagramSocket socket;
    private ExecutorService executor;

    public UDPServer2() {
        try {
            socket = new DatagramSocket(9876);
            executor = Executors.newFixedThreadPool(4); // Create thread pool with 4 threads
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

        executor.execute(listenerTask);
        executor.shutdown();
    }

    public static void main(String[] args) {
        UDPServer2 server = new UDPServer2();
        server.createAndListenSocket();
    }
}

