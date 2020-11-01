
  HardwareSerial portCommunication(2);


  const int maxFileSize=10000;
  const int baudRate=19200;

  byte fileReceived[maxFileSize];
  byte fileSend[maxFileSize];

  void receiveFile();
  void sendFile();

  void setup() {
    // Serial_8N1 é o protocolo que quer dizer 8-bit- no parity 2 stop bits
    //17 é o txd
    //16 é o rxd
    Serial.begin(baudRate);
    portCommunication.begin(baudRate, SERIAL_8N1, 17, 16);
  }

  void loop() {

    //if the user is sending a message
    if(Serial.available()){ 
      sendFile();
      }

      //if the user receives a message
      if(portCommunication.available()){
            receiveFile();
        }
  }

  void receiveFile(){
      portCommunication.readBytesUntil('|',fileReceived,maxFileSize);
      Serial.write(fileReceived,sizeof(fileReceived));
  }

  void sendFile(){
      Serial.readBytesUntil('\b', fileSend, maxFileSize);
      portCommunication.write(fileSend, sizeof(fileSend));
      
  }
