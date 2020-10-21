HardwareSerial receiver(2);


void setup() {
  // put your setup code here, to run once:
  // Serial_8N1 é o protocolo que quer dizer 8-bit- no parity 2 stop bits
  //17 é o txd
  //16 é o rxd
  Serial.begin(9600);
  receiver.begin(9600, SERIAL_8N1, 17, 16);
}

void loop() {
  // put your main code here, to run repeatedly:
  delay(10000);
  String rxd = receiver.readString();
  //com o uso da serial enviamos para o pc a info!
  //ou seja pode ser usado na app externa
  Serial.print(rxd);
}
