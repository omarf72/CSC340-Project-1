import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.Time;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class UDPPeer extends Thread{
    private DatagramSocket socket = null;

    public UDPPeer() 
    {
    	try 
    	{
    		//create the socket assuming the server is listening on port 9876
			socket = new DatagramSocket(9876);
		} 
    	catch (SocketException e) 
    	{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }
    public void createAndListenSocket() 
    {
        try 
        {
        	//incoming data buffer
            byte[] incomingData = new byte[1024];

            while (true) 
            {
            	//create incoming packet
                DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                System.out.println("Waiting...");
                
                //wait for the packet to arrive and store it in incoming packet
                socket.receive(incomingPacket);
                
                //retrieve the data
                String message = new String(incomingPacket.getData());
                
                //terminate if it is "THEEND" message from the client
                if(message.equals("THEEND"))
                {
                	socket.close();
                	break;
                }
                System.out.println("Received message from client: " + message);
                System.out.println("Client Details:PORT " + incomingPacket.getPort()
                + ", IP Address:" + incomingPacket.getAddress());
                
                //retrieve client socket info and create response packet
                InetAddress IPAddress = incomingPacket.getAddress();
                int port = incomingPacket.getPort();
                String reply = "Thank you for the message";
                byte[] data = reply.getBytes();
                DatagramPacket replyPacket =
                        new DatagramPacket(data, data.length, IPAddress, port);
                socket.send(replyPacket);
            }
        } 
        catch (SocketException e) 
        {
            e.printStackTrace();
        } 
        catch (IOException i) 
        {
            i.printStackTrace();
        } 
    }
    public void run(){
        Random rand = new Random();
        int seconds = rand.nextInt(5,10);
        while(true){
            try {
                TimeUnit.SECONDS.sleep(seconds);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ping();
            seconds = rand.nextInt(5,31);
        }
        
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
        server.start();

        server.createAndListenSocket();
        //server.ping();
    }
    public void monitorNodes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'monitorNodes'");
    }
}
