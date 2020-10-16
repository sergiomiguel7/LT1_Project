import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.nio.charset.StandardCharsets;


public class Communication {

    private SerialPort serialPort;
    private StringBuilder message = new StringBuilder();
    private byte[] accent = new byte[2];
    private boolean founded = false;
    private static Communication instance = new Communication();

    public Communication() {

        serialPort = SerialPort.getCommPort("COM3");
        serialPort.openPort();
        checkConnection();
        setEventHandler();

    }


    public static Communication getInstance() {
        return instance;
    }


    private void checkConnection() {
        if (serialPort.isOpen())
            System.out.println("Port initialized");
        else
            System.out.println("Port not available");
    }

    /**
     * @param type - a accent char is a 2 byte value (0 is the first byte, 1 the second)
     * @param toFix - the correspondent byte to fix
     *
     * this method fix a accent byte
     */
    private void fixByte(int type, byte toFix) {
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
                         fixByte(0, b);
                         continue;
                        }
                        if (founded) {
                            fixByte(1,b);
                            continue;
                        }
                        char letter = (char) b;
                        message.append(letter);

                    } else {
                        System.out.println(message.toString());
                        message = new StringBuilder();
                    }
                }
            }
        });
    }
}