/**
 * Represents a network packet containing metadata and data.
 * 
 * @author Omar Fofana
 */
import java.io.Serializable;

public class Packet implements Serializable {

    private byte version; // Version of the packet
    private int nodeId; // Unique identifier for the node
    private int dataLength; // Length of the data in bytes
    private String data; // The actual data payload

    /**
     * Constructs a Packet with specified parameters.
     *
     * @param version The version of the packet
     * @param nodeId The unique identifier of the node
     * @param dataLength The length of the data
     * @param data The actual data payload
     */
    public Packet(byte version, int nodeId, int dataLength, String data) {
        this.version = version;
        this.nodeId = nodeId;
        this.dataLength = dataLength;
        this.data = data;
    }

    /**
     * Gets the version of the packet.
     *
     * @return The version of the packet
     */
    public byte getVersion() {
        return version;
    }

    /**
     * Gets the node ID.
     *
     * @return The node ID
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * Gets the length of the data.
     *
     * @return The data length
     */
    public int getDataLength() {
        return dataLength;
    }

    /**
     * Gets the data payload.
     *
     * @return The data payload as a String
     */
    public String getData() {
        return data;
    }

    /**
     * Sets the version of the packet.
     *
     * @param version The new version to set
     */
    public void setVersion(byte version) {
        this.version = version;
    }

    /**
     * Sets the node ID.
     *
     * @param nodeId The new node ID to set
     */
    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Sets the length of the data.
     *
     * @param dataLength The new data length to set
     */
    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }

    /**
     * Sets the data payload.
     *
     * @param data The new data payload to set
     */
    public void setData(String data) {
        this.data = data;
    }
}
