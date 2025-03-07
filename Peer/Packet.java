//Author: Omar Fofana

import java.io.Serializable;

public class Packet implements Serializable{

    private byte version;

    private int nodeId;

    private int dataLength;

    private String data;



    public Packet(byte version,int nodeId,int dataLength,String data){

        this.version=version;
        this.nodeId=nodeId;
        this.dataLength = dataLength;
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



    public String getData() {
        return data;
    }


    public void setVersion(byte version) {
        this.version = version;
    }


    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }


    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }


    public void setData(String data) {
        this.data = data;
    }



    


}