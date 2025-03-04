import java.io.Serializable;

public class Packet implements Serializable{

    private byte version;

    private int nodeId;

    private boolean on_off;

    private String data;



    public Packet(byte version,int nodeId,boolean on_off,String data){

        this.version=version;
        this.nodeId=nodeId;
        this.on_off = on_off;
        this.data=data;

    }


    public byte getVersion() {
        return version;
    }



    public int getNodeId() {
        return nodeId;
    }



    public boolean getOn_off() {
        return on_off;
    }



    public String getData() {
        return data;
    }


    public void setVersion(byte version) {
        this.version = version;
    }


    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }


    public void setOn_off(boolean on_off) {
        this.on_off = on_off;
    }


    public void setData(String data) {
        this.data = data;
    }



    


}