import java.nio.ByteBuffer;
import java.util.ArrayList;

public class FileReceived {

    private byte type; //type from data
    private int size; //size from data
    private int sizeByteCounter; //number of bytes from size received
    private byte[] byteSize; //array to store the byte size
    private ArrayList<Byte> data; //packet data
    private byte timeStamp;


    public FileReceived() {
        this.type = -1;
        this.size = -1;
        this.timeStamp = -1;
        this.sizeByteCounter=1;
        this.byteSize = new byte[4];
        this.byteSize[0]=0;
    }

    // getters and setters

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
        allocDataSize();
    }

    public byte[] getData() {
        byte[] toSave = new byte[size];
        int i = 0;

        for (Byte b : this.data) {
            toSave[i] = b;
            i++;
        }
        return toSave;
    }



    //methods

    private void allocDataSize() {
        this.data = new ArrayList<>(this.size);
    }

    public boolean putByteData(byte b) {
        if (this.data.size() != this.size) {
            this.data.add(b);
            return true;
        }
        return false;
    }

    public void setByteSize(byte size){
        this.byteSize[sizeByteCounter] = size;
        this.sizeByteCounter++;

        if(this.sizeByteCounter == 4){
            this.setSize(ByteBuffer.wrap(this.byteSize).getInt());
        }
    }


}
