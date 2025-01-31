/*********************************************************************
 This is an example for our nRF51822 based Bluefruit LE modules

 Pick one up today in the adafruit shop!

 Adafruit invests time and resources providing this open source code,
 please support Adafruit and open-source hardware by purchasing
 products from Adafruit!

 MIT license, check LICENSE for more information
 All text above, and the splash screen below must be included in
 any redistribution
*********************************************************************/

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

/*=========================================================================
    APPLICATION SETTINGS

    FACTORYRESET_ENABLE       Perform a factory reset when running this sketch
   
                              Enabling this will put your Bluefruit LE module
                              in a 'known good' state and clear any config
                              data set in previous sketches or projects, so
                              running this at least once is a good idea.
   
                              When deploying your project, however, you will
                              want to disable factory reset by setting this
                              value to 0.  If you are making changes to your
                              Bluefruit LE device via AT commands, and those
                              changes aren't persisting across resets, this
                              is the reason why.  Factory reset will erase
                              the non-volatile memory where config data is
                              stored, setting it back to factory default
                              values.
       
                              Some sketches that require you to bond to a
                              central device (HID mouse, keyboard, etc.)
                              won't work at all with this feature enabled
                              since the factory reset will clear all of the
                              bonding data stored on the chip, meaning the
                              central device won't be able to reconnect.
    MINIMUM_FIRMWARE_VERSION  Minimum firmware version to have some new features
    MODE_LED_BEHAVIOUR        LED activity, valid options are
                              "DISABLE" or "MODE" or "BLEUART" or
                              "HWUART"  or "SPI"  or "MANUAL"
    -----------------------------------------------------------------------*/
    #define FACTORYRESET_ENABLE         1
    #define MINIMUM_FIRMWARE_VERSION    "0.6.6"
    #define MODE_LED_BEHAVIOUR          "MODE"
    // SOFTWARE UART SETTINGS
    #define BLUEFRUIT_SWUART_RXD_PIN       9    // Required for software serial!
    #define BLUEFRUIT_SWUART_TXD_PIN       10   // Required for software serial!
    #define BLUEFRUIT_UART_CTS_PIN         11   // Required for software serial!
    #define BLUEFRUIT_UART_RTS_PIN         -1   // Optional, set to -1 if unused
/*=========================================================================*/

// Create the bluefruit object, either software serial...uncomment these lines
SoftwareSerial bluefruitSS = SoftwareSerial(BLUEFRUIT_SWUART_TXD_PIN, BLUEFRUIT_SWUART_RXD_PIN);

Adafruit_BluefruitLE_UART ble(bluefruitSS, BLUEFRUIT_UART_MODE_PIN,
                      BLUEFRUIT_UART_CTS_PIN, BLUEFRUIT_UART_RTS_PIN);


// Program Servo Setup
int servopin = 6;
Servo myservo;  // create servo object to control a servo
unsigned long time;
/**************************************************************************/
/*!
    @brief  Sets up the HW an the BLE module (this function is called
            automatically on startup)
*/
/**************************************************************************/
void setup(void)
{

  /* Disable command echo from Bluefruit */
  ble.begin(VERBOSE_MODE);
  ble.factoryReset();
  ble.verbose(false);
  ble.echo(false);

  /* Wait for connection */
  while (! ble.isConnected()) {
      myservo.write(0);
      delay(500);
  }

  // Set module to DATA mode
  ble.setMode(BLUEFRUIT_MODE_DATA);
  myservo.attach(servopin);
}

/**************************************************************************/
/*!
    @brief  Constantly poll for new command or response data
*/
/**************************************************************************/
// Servo Program

void loop(void)
{
  // Echo received data (READ FROM APP)# RECEIVE DATA and Sends feedback to app #FEEDBACK
  while ( ble.available() )
  {
    // Delay range 1550-1650
    myservo.write(0);
    if ('s' == ble.read())
    {
      myservo.write(0);
    }
  }
}
