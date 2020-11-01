import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.Scanner;


public class Communication {

    private SerialPort serialPort;
    private StringBuilder message = new StringBuilder();  //received message
    private byte[] accent = new byte[2]; // array to fix accent on strings
    private boolean accentFound = false; // flag if a accent is founded
    private FileReceived fileReceived;
    private static Communication instance = new Communication();

    public Communication() {

        serialPort = chooseSerialPort();
        while (serialPort == null) {
            System.out.println("Choose a existing port");
            serialPort = chooseSerialPort();
        }

        serialPort.setBaudRate(19200);
        serialPort.openPort();
        checkConnection();
        setEventHandler();
        fileReceived = new FileReceived();
    }

    public static Communication getInstance() {
        return instance;
    }


    private SerialPort chooseSerialPort() {
        System.out.println(Arrays.toString(SerialPort.getCommPorts()));
        int i = 0;
        System.out.println("Escolhe a porta que está conectada ao teu Arduino");
        for (SerialPort port : SerialPort.getCommPorts()) {
            System.out.println(i + "- " + port.getSystemPortName());
            i++;
        }
        Scanner option = new Scanner(System.in);
        int serial = Integer.parseInt(option.nextLine());
        if (serial > -1 && serial < i)
            return SerialPort.getCommPorts()[serial];
        else
            return null;
    }

    private void checkConnection() {
        if (serialPort.isOpen())
            System.out.println("Port initialized");
        else
            System.out.println("Port not available");
    }

    /**
     * @param type  - a accent char is a 2 byte value (0 is the first byte, 1 the second)
     * @param toFix - the correspondent byte to fix
     *              <p>
     *              this method fix a accent byte
     */
    private void fixReceivedByte(int type, byte toFix) {
        switch (type) {
            case 0: {
                accentFound = true;
                accent[0] = toFix;
                break;
            }
            case 1: {
                accentFound = false;
                accent[1] = toFix;
                String fixed = new String(accent, StandardCharsets.UTF_8);
                accent = new byte[2];
                message.append(fixed);
            }
        }
    }

    /**
     * @param toFix - the correspondent byte to fix
     *              <p>
     *              this method transform a accent byte in a two byte array to the
     *              other user can receive the correct char
     * @return the two byte array that correspond to the accent byte
     */
    private byte[] fixByteToSend(byte toFix) {

        byte[] arrayByte = new byte[1];
        byte[] fixed;

        arrayByte[0] = toFix;
        String converted = new String(arrayByte, StandardCharsets.ISO_8859_1);
        fixed = converted.getBytes();

        return fixed;

    }

    /**
     * @param b - byte to append on received message
     */
    private void buildMessageString(byte b) {

        boolean filled = fileReceived.putByteData(b);

        if (!filled) {
            printMessage();
        }

    }

    private void printMessage() {

        for (byte letter : fileReceived.getData()) {

            if ((int) letter < 0 && !accentFound) {
                fixReceivedByte(0, letter);
            } else if (accentFound) {
                fixReceivedByte(1, letter);
            } else
                message.append((char) letter);
        }

        System.out.println("Outro: " + message);
        message = new StringBuilder();
        fileReceived = new FileReceived();
    }


    /**
     * this method is listening the port and if it have data available then the port read the bytes
     */
    private void setEventHandler() {
        serialPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
                    return;
                byte[] newData = new byte[serialPort.bytesAvailable()];
                serialPort.readBytes(newData, newData.length);

                //fill the type and the size
                if (fileReceived.getType() == -1 || fileReceived.getSize() == -1) {
                    for (byte byteReceived : newData) {
                        //if type isn't found
                        if (fileReceived.getType() == -1) {
                            if (byteReceived == 1 || byteReceived == 2) {
                                fileReceived.setType(byteReceived);
                            }
                        }
                        // if type is founded but size don't filled yet
                        else if (fileReceived.getType() != -1) {
                            if (fileReceived.getSize() == -1) {
                                fileReceived.setByteSize(byteReceived);
                            } else {
                                fillDataPacket(byteReceived);
                            }
                        }
                    }
                    //fill the data packet
                } else {
                    for (byte b : newData) {
                        fillDataPacket(b);
                    }
                }
            }
        });
    }

    private void fillDataPacket(byte dataByte) {
        if (fileReceived.getType() == 1)
            buildMessageString(dataByte);
        else if (fileReceived.getType() == 2)
            buildFile(dataByte);
    }


    int counter = 0;

    private void buildFile(byte b) {

        boolean filled = fileReceived.putByteData(b);
        System.out.println("Finished: " + !filled + " Received -> " + counter + " of " + fileReceived.getSize());
        counter++;
        if (!filled) {
            saveFile();
        }

    }


    /**
     * @param message - the input message from user
     *                <p>
     *                in this method the inputed string from user is converter to a byte array to send via serial port
     *                to the arduino
     */
    public void stringToByte(String message) {

        int messageSize = checkStringSize(message);
        messageSize = messageSize + 6;   //one byte to type, 3 to size, 2 to stop bits
        byte[] bArray = new byte[messageSize];
        byte[] sizeBytes = ByteBuffer.allocate(4).putInt(checkStringSize(message)).array();

        int i = 1;
        bArray[0] = 1; //type

        //fill the size space on packet
        for (int j = 0; i < sizeBytes.length; j++) {
            if (j != 0) {
                bArray[i] = sizeBytes[j];
                i++;
            }
        }

        int counter = 0;
        for (i = 4; i < messageSize - 2; i++) {
            //check if is a character with accent
            if ((byte) message.charAt(counter) > 0)
                bArray[i] = (byte) message.charAt(counter);
            else {
                byte[] fixed = fixByteToSend((byte) message.charAt(counter));
                bArray[i] = fixed[0];
                bArray[i + 1] = fixed[1];
                i++;
            }
            counter++;
        }

        bArray[messageSize - 2] = '|';
        bArray[messageSize - 1] = '\b';
        serialPort.writeBytes(bArray, bArray.length);
    }


    /**
     * @param userMessage - message that user want to send
     * @return size that the program will need in bytes, to handle the accent bytes
     */
    public int checkStringSize(String userMessage) {
        int size = 0;

        for (int i = 0; i < userMessage.length(); i++) {
            if ((byte) userMessage.charAt(i) > 0)
                size++;
            else {
                size = size + 2;
            }
        }

        return size;

    }

    /**
     * method to build the packet of file
     */
    public void sendFile() {
        byte[] fileBytes = readFile();

        if (fileBytes != null) {
            byte[] fileToSend = new byte[fileBytes.length + 6]; //one byte to type, 3 to size, 2 to stop bits
            byte[] sizeBytes = ByteBuffer.allocate(4).putInt(fileBytes.length).array();

            int i = 1;
            fileToSend[0] = 2; // set type to 2

            //fill the size space on packet
            for (int j = 0; i < sizeBytes.length; j++) {
                if (j != 0) {
                    fileToSend[i] = sizeBytes[j];
                    i++;
                }
            }

            i = 4;
            for (byte b : fileBytes) {
                fileToSend[i] = b;
                i++;
            }

            //stop bits
            fileToSend[fileToSend.length - 2] = '|';
            fileToSend[fileToSend.length - 1] = '\b';

            System.out.println("Início da transmissão " +  Instant.now());
            serialPort.writeBytes(fileToSend, fileToSend.length);
        }
    }

    /**
     * @return array of bytes from file
     */
    private byte[] readFile() {
        //TODO: read path from terminal
        File toRead = new File("envio.txt");
        try {
            return Files.readAllBytes(toRead.toPath());
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    private void saveFile() {
        try {
            System.out.println("Fim da transmissão " + Instant.now());
            FileUtils.writeByteArrayToFile(new File("DadosRecebidos.txt"), fileReceived.getData());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        System.out.println("File saved with sucess!");
        fileReceived = new FileReceived();
    }

    public String readUserMessage() {
        Scanner input = new Scanner(System.in);
        return input.nextLine();
    }
}