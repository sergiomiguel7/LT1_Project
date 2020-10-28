import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;


public class Communication {

    private SerialPort serialPort;
    private StringBuilder message = new StringBuilder();
    private byte[] accent = new byte[2];
    private boolean founded = false;
    private static Communication instance = new Communication();

    public Communication() {
        serialPort = chooseSerialPort();
        serialPort.openPort();
        checkConnection();
        setEventHandler();
    }

    public static Communication getInstance() {
        return instance;
    }



    private SerialPort chooseSerialPort(){
        System.out.println(Arrays.toString(SerialPort.getCommPorts()));
        int i = 0;
        System.out.println("Escolhe a porta que está conectada ao teu Arduino");
        for(SerialPort port : SerialPort.getCommPorts()){
            System.out.println(i + "- " + port.getSystemPortName());
            i++;
        }
        Scanner option = new Scanner(System.in);
        int serial = Integer.parseInt(option.nextLine());
        return SerialPort.getCommPorts()[serial];

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
                        message = new StringBuilder();
                    }
                }


            }
        });
    }

    /**
     * @param message - the input message from user
     *
     *  in this method the inputed string from user is converter to a byte array to send via serial port
     *  to the arduino
     */
    public void stringToByte(String message) {

        int messageSize = checkStringSize(message);
        byte[] bArray = new byte[messageSize + 2];  //plus 2 to handle the end chars

        for (int i = 0; i < message.length(); i++) {
            //check if is a character with accent
            if ((byte) message.charAt(i) > 0)
                bArray[i] = (byte) message.charAt(i);
            else {
                byte[] fixed = fixByteToSend((byte) message.charAt(i));
                bArray[i] = fixed[0];
                bArray[i + 1] = fixed[1];
                i++;
            }
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


    public String readUserMessage() {
        Scanner input = new Scanner(System.in);
        return input.nextLine();
    }
}