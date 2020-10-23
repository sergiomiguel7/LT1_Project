  HardwareSerial portCommunication(2);
  
  
  const int maxMessageSize=100;

  byte messageReceived[maxMessageSize];
  byte messageSend[maxMessageSize];

  void receiveMessage();
  void sendMessage();
  
  void setup() {
    // Serial_8N1 é o protocolo que quer dizer 8-bit- no parity 2 stop bits
    //17 é o txd
    //16 é o rxd
    Serial.begin(9600);
    portCommunication.begin(9600, SERIAL_8N1, 17, 16);
  }
  
  void loop() {
    
    //if the user is sending a message
    if(Serial.available()){ 
      sendMessage();
      }
  
      //if the user receives a message
      if(portCommunication.available()){
            receiveMessage();
        }
  }
  
  void receiveMessage(){
    String rxd = portCommunication.readString();
    Serial.print(rxd);
  }
  
  void sendMessage(){
      Serial.readBytesUntil('\b', messageSend, 100);
      int sz = 0;
      
      for(int i=0; i<maxMessageSize; i++){
        
         if((char)messageSend[i]== '\n'){
            sz++;
            break;
          }
          sz++;
        }
      
      portCommunication.write(messageSend, sz);
    }
