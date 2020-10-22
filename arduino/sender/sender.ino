HardwareSerial sender(2);

void setup() {
  Serial.begin(9600);
  // put your setup code here, to run once:
  // Serial_8N1 é o protocolo que quer dizer 8-bit- no parity 2 stop bits
  sender.begin(9600, SERIAL_8N1, 17, 16);
}

void loop() {
  // put your main code here, to run repeatedly:
    delay(1000);
    sender.write("Isto é uma mensagem de outro Arduino\n");
    String msg = sender.readString();
    Serial.print(msg);
}
