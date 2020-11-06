#include <SPI.h>
#include "RF24.h"

byte addresses[][6] = {"G1TST","G2TST"};

/* Hardware configuration: Set up nRF24L01 radio on SPI bus plus pins 4 & 5 */
RF24 radio(4,5);
/**********************************************************/

// Used to control whether this node is sending(1) or receiving(0)
bool role = 0;

/***      Set this radio as radio number 0 or 1         ***/
bool radioNumber = 1;

//To test
struct dataStruct{
  unsigned long _micros;
  float value;
}myData;



void setup() {

  Serial.begin(115200);
  
  radio.begin();

  // Set the PA Level low to prevent power supply related issues since this is a
 // getting_started sketch, and the likelihood of close proximity of the devices. RF24_PA_MAX is default.
  radio.setPALevel(RF24_PA_LOW);
  
  // Open a writing and reading pipe on each radio, with opposite addresses
  if(radioNumber){
    radio.openWritingPipe(addresses[1]);
    radio.openReadingPipe(1,addresses[0]);
  }else{
    radio.openWritingPipe(addresses[0]);
    radio.openReadingPipe(1,addresses[1]);
  }
  
  myData.value = 1.22;
  // Start the radio listening for data
  radio.startListening();
}


void loop(){
  
   changeRole();


  if(role == 1){ 
     sendData();
    }
  else if ( role == 0 )
  {
    receiveData();
 }
  
  }

  
  void receiveData(){

    if( radio.available()){
                                                           // Variable for the received timestamp
      while (radio.available()) {                          // While there is data ready
        radio.read( &myData, sizeof(myData) );             // Get the payload
      }
     
      radio.stopListening();                               // First, stop listening so we can talk  
      myData.value += 0.01;                                // Increment the float value
      radio.write( &myData, sizeof(myData) );              // Send the final one back.      
      radio.startListening();                              // Now, resume listening so we catch the next packets.     
      Serial.print(F("Sent response "));
      Serial.print(myData._micros);  
      Serial.print(F(" : "));
      Serial.println(myData.value);
   }
    
    }

  
  void sendData(){
    
    //to start transmitting stop the listening
     radio.stopListening();
     Serial.println(F("Sending..."));

    myData._micros = micros();

      //if the write is sucessfull, soo all the data was sended
      if(!radio.write(&myData, sizeof(myData))){
        Serial.println("Not working, soz");
        }

      //listening again  
      radio.startListening();
        
     unsigned long started_waiting_at = micros();               // Set up a timeout period, get the current microseconds
     boolean timeout = false;                                   // Set up a variable to indicate if a response was received or not
    
    while ( ! radio.available() ){                             // While nothing is received
      if (micros() - started_waiting_at > 200000 ){            // If waited longer than 200ms, indicate timeout and exit while loop
          timeout = true;
          break;
      }      
     }

     if(timeout){ //not received confirmation from receiver
      Serial.println(F("Failed, response timed out."));
      } else {   //catch the response

        radio.read( &myData, sizeof(myData) );
        unsigned long time = micros();
        
        Serial.print(F("Sent "));
        Serial.print(time);
        Serial.print(F(", Got response "));
        Serial.print(myData._micros);
        Serial.print(F(", Round-trip delay "));
        Serial.print(time-myData._micros);
        Serial.print(F(" microseconds Value "));
        Serial.println(myData.value);
        
        }


        // Try again 10s later
    delay(10000);
    
    }


   void changeRole(){

    
    if ( Serial.available() )
  {
    int c = Serial.read();
    Serial.print("c: " + c );
    Serial.println("r: " + ((int)'R') );
    if ( c == ((int)'T') && role == 0 ){      
      Serial.print(F("*** CHANGING TO TRANSMIT ROLE -- PRESS 'R' TO SWITCH BACK"));
      role = 1;                  // Become the primary transmitter (ping out)
   }else
    if ( c == ((int)'R') && role == 1 ){
      Serial.println(F("*** CHANGING TO RECEIVE ROLE -- PRESS 'T' TO SWITCH BACK"));      
       role = 0;                // Become the primary receiver (pong back)
       radio.startListening();
       
    }
  }
   } 
