import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Scanner;


public class Communication {

    private SerialPort serialPort;
    private StringBuilder message = new StringBuilder();  //received message
    private byte[] accent = new byte[2]; // array to fix accent on strings
    private boolean founded = false; // flag if a accent is founded
    private boolean flagFounded = false;
    private int fileType = 2;
    private int fileBytesSize = 2747; //hard coded file size
    private int counterBytes = 0; //actual readed bytes
    private byte[] receivedFile = new byte[fileBytesSize]; //received file
    private static Communication instance = new Communication();

    public Communication() {

        serialPort = chooseSerialPort();
        while (serialPort == null) {
            System.out.println("Choose a existing port");
            serialPort = chooseSerialPort();
        }
        serialPort.openPort();
        checkConnection();
        setEventHandler();
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
                founded = true;
                accent[0] = toFix;
                break;
            }
            case 1: {
                founded = false;
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

                if (!flagFounded) {
                    for (byte b : newData) {
                        if (b == 0 || b == 1) {
                            fileType=b;
                            flagFounded = true;
                            break;
                        }
                    }
                }

                if (flagFounded) {
                    if (fileType == 0) {
                        // if first byte is 0 is string, 1 is file
                        for (byte b : newData) {
                            if ((char) b != '\n') {
                                if ((int) b < 0 && !founded) {
                                    fixReceivedByte(0, b);
                                    continue;
                                }
                                if (founded) {
                                    fixReceivedByte(1, b);
                                    continue;
                                }
                                char letter = (char) b;
                                message.append(letter);

                            } else {
                                System.out.println("Outro:" + message.toString());
                                flagFounded = false;
                                message = new StringBuilder();
                            }
                        }
                    } else if(fileType==1){
                        for (int j = 0; j < newData.length; j++) {

                            if (j != 0) {
                                byte b = newData[j];

                                if (counterBytes != fileBytesSize) {
                                    receivedFile[counterBytes] = b;
                                    counterBytes++;
                                } else {
                                    flagFounded=false;
                                    saveFile();
                                }
                            }

                        }
                    }

                }
            }
        });
    }

    /**
     * @param message - the input message from user
     *                <p>
     *                in this method the inputed string from user is converter to a byte array to send via serial port
     *                to the arduino
     */
    public void stringToByte(String message) {

        int messageSize = checkStringSize(message);
        byte[] bArray = new byte[messageSize + 3];  //plus 2 to handle the end chars

        int counter = 0;

        bArray[0] = 0;

        for (int i = 1; i < messageSize; i++) {
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

        bArray[messageSize] = '\n';
        bArray[messageSize + 1] = '\b';
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
            byte[] fileToSend = new byte[fileBytes.length + 3];
            int i = 0;
            fileToSend[i] = 1;
            i++;
            for (byte b : fileBytes) {
                fileToSend[i] = b;
                i++;
            }

            fileToSend[fileToSend.length - 2] = '\n';
            fileToSend[fileToSend.length - 1] = '\b';

            serialPort.writeBytes(fileToSend, fileToSend.length);
        }
    }

    /**
     * @return array of bytes from file
     */
    private byte[] readFile() {
        //TODO: read path from terminal
        File toRead = new File("Dados.ppI");
        try {
            return Files.readAllBytes(toRead.toPath());
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    private void saveFile() {
        try {
            FileUtils.writeByteArrayToFile(new File("DadosRecebidos.ppI"), receivedFile);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        System.out.println("File readed with sucess!");
        receivedFile = new byte[fileBytesSize];
        counterBytes = 0;
    }

    public String readUserMessage() {
        Scanner input = new Scanner(System.in);
        return input.nextLine();
    }
}