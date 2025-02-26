//package networking;

import java.io.IOException;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * 
 * @author cjaiswal
 *
 *  
 * 
 */
public class UDPClient2 {
    private DatagramSocket socket;
    private Scanner in = new Scanner(System.in);
    private ExecutorService executor;

    public UDPClient2() {
        try {
            socket = new DatagramSocket();
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
        try {
            InetAddress IPAddress = Inet4Address.getByName("localhost");
            int serverPort = 9876;
            byte[] incomingData = new byte[1024];
            
            Runnable senderTask = () -> {
                try {
                    char ch = 'y';
                    do {
                        System.out.println("Enter your message:");
                        String sentence = in.nextLine();
                        byte[] data = serialize(sentence);
                        DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, serverPort);
                        socket.send(sendPacket);
                        System.out.println("Message sent.");
                        
                        if (sentence.equals("THEEND")) {
                            break;
                        }

                        System.out.println("Chat more? Y/N...");
                        ch = in.nextLine().charAt(0);
                    } while (ch == 'y' || ch == 'Y');
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };

            Runnable receiverTask = () -> {
                try {
                    while (true) {
                        DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                        socket.receive(incomingPacket);
                        String response = (String) deserialize(incomingPacket.getData());
                        System.out.println("Response from server: " + response);
                        
                        if (response.equals("THEEND")) {
                            break;
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            };

            executor.execute(senderTask);
            executor.execute(receiverTask);

            executor.shutdown();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        UDPClient2 client = new UDPClient2();
        client.createAndListenSocket();
    }
}

