#include <Adafruit_ATParser.h>
#include <Adafruit_BLE.h>
#include <Adafruit_BLEBattery.h>
#include <Adafruit_BLEEddystone.h>
#include <Adafruit_BLEGatt.h>
#include <Adafruit_BLEMIDI.h>
#include <Adafruit_BluefruitLE_SPI.h>
#include <Adafruit_BluefruitLE_UART.h>

#include <Arduino.h>
#include <SPI.h>
#include <Servo.h>


#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_SPI.h"
#include "Adafruit_BluefruitLE_UART.h"

#include "BluefruitConfig.h"

#if SOFTWARE_SERIAL_AVAILABLE
  #include <SoftwareSerial.h>
#endif

#define VERBOSE_MODE    false  // Set to false to disable detailed info
#define FACTORYRESET_ENABLE         0
#define FACTORYRESET_ENABLE         1
#define MINIMUM_FIRMWARE_VERSION    "0.6.6"
#define MODE_LED_BEHAVIOUR          "MODE"
// SOFTWARE UART SETTINGS
#define BLUEFRUIT_SWUART_RXD_PIN       9    // Required for software serial!
#define BLUEFRUIT_SWUART_TXD_PIN       10   // Required for software serial!
#define BLUEFRUIT_UART_CTS_PIN         11   // Required for software serial!
#define BLUEFRUIT_UART_RTS_PIN         -1   // Optional, set to -1 if unused

#define BUFFER_SIZE 128  // Define a buffer size to hold the incoming message

char inputBuffer[BUFFER_SIZE];  // Buffer to hold the incoming data

// Create the bluefruit object, either software serial
SoftwareSerial bluefruitSS = SoftwareSerial(BLUEFRUIT_SWUART_TXD_PIN, BLUEFRUIT_SWUART_RXD_PIN);

Adafruit_BluefruitLE_UART ble(bluefruitSS, BLUEFRUIT_UART_MODE_PIN,
                      BLUEFRUIT_UART_CTS_PIN, BLUEFRUIT_UART_RTS_PIN);

int servopin = 7;
Servo myservo;  // create servo object to control a servo

// Helper function for error handling
void error(const __FlashStringHelper* err) {
  Serial.println(err);
  while (1);
}

void setup(void) {
  Serial.begin(115200);
  clearSerialMonitor();

//  while (!Serial);  // Wait for Serial monitor to be ready (needed for some boards)
//
//  Serial.println(F("Starting Bluefruit LE communication"));
//
  // Initialize Bluefruit
  if (!ble.begin(false)) {
    //error(F("Could not initialize Bluefruit. Check connections!"));
  }
  Serial.println(F("Bluefruit initialized successfully"));

  myservo.attach(servopin);
  delay(500);
 
//  // Disable echo and verbose mode
//  ble.echo(false);
//  ble.verbose(false);

  // Wait for a Bluetooth connection
  Serial.println(F("Waiting for device to connect..."));

  // Set Bluefruit to DATA mode
  ble.setMode(BLUEFRUIT_MODE_DATA);
  Serial.println(F("Set Bluefruit to DATA mode"));
  
  while (!ble.isConnected()) {
    delay(500);
    myservo.write(90);  // Initial position of the servo
    Serial.print(F("."));
  }
  Serial.println(F("\nDevice connected!"));
}

void loop(void) {
  if (ble.isConnected()) {
    // Sending a message to the connected device
    Serial.println(F("Sending message: Hi, Temi"));
    ble.print("Hi, Temi");

    for (int i = 0; i < 20; i++) {
      // Check for received data from the connected device
      if (ble.available()) {
        memset(inputBuffer, 0, BUFFER_SIZE);  // Clear the buffer

        // Read the incoming message byte by byte
        size_t len = 0;
        while (ble.available() && len < BUFFER_SIZE - 1) {
          inputBuffer[len++] = ble.read();
        }

        // Print the received message to the Serial Monitor
        Serial.print(F("Message received: "));
        Serial.println(inputBuffer);
        
        // Move the servo to 180 degrees if the message is "s"
        if (strcmp(inputBuffer, "s") == 0) {
          myservo.write(180);
          Serial.println(F("Servo moved to 180 degrees"));
        }
      }

      delay(100);  // Delay to avoid spamming
    }
  }
}

void clearSerialMonitor() {
  for (int i = 0; i < 50; i++) {
    Serial.println();  // Print blank lines to clear the screen
  }
}
