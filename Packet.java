public class Packet{

    private byte version;

    private int nodeId;

    private int dataLength;

    private byte data;



    public Packet(byte version,int nodeId,int dataLength,byte data){

        this.version=version;
        this.nodeId=nodeId;
        this.dataLength=dataLength;
        this.data=data;

    }


    public byte getVersion() {
        return version;
    }



    public int getNodeId() {
        return nodeId;
    }



    public int getDataLength() {
        return dataLength;
    }



    public byte getData() {
        return data;
    }



    

}