#include <WiFi.h>
#include <WebServer.h>
#include <Wire.h>
#include <MPU6050.h>
#include <DHT.h>
#include <TinyGPS++.h>
#include <HardwareSerial.h>

// ===== PIN DEFINITIONS =====
#define DHTPIN 4
#define DHTTYPE DHT22
#define MQ6_DIGITAL 18   // 🔥 Gas sensor D0 pin

// ===== OBJECTS =====
MPU6050 mpu;
DHT dht(DHTPIN, DHTTYPE);
TinyGPSPlus gps;
HardwareSerial gpsSerial(1);
WebServer server(80);

// ===== WIFI =====
const char* ssid = "4AL23EC068";
const char* password = "Alvas@12345";

// ===== FUNCTION =====
void handleData() {

  // 🌡 Temperature
  float temp = dht.readTemperature();
  if (isnan(temp)) temp = 0;

  // 🌫 Gas (Digital)
  int gas = digitalRead(MQ6_DIGITAL);

  // 🧍 Motion
  int16_t ax, ay, az;
  mpu.getAcceleration(&ax, &ay, &az);
  float motion = sqrt(ax*ax + ay*ay + az*az);

  // 📍 GPS
  while (gpsSerial.available()) {
    gps.encode(gpsSerial.read());
  }

  double lat = 0.0, lng = 0.0;
  if (gps.location.isValid()) {
    lat = gps.location.lat();
    lng = gps.location.lng();
  }

  // 📦 JSON RESPONSE
  String json = "{";
  json += "\"temp\":" + String(temp) + ",";
  json += "\"gas\":" + String(gas) + ",";
  json += "\"motion\":" + String(motion) + ",";
  json += "\"lat\":" + String(lat, 6) + ",";
  json += "\"lng\":" + String(lng, 6);
  json += "}";

  server.send(200, "application/json", json);
}

// ===== SETUP =====
void setup() {
  Serial.begin(115200);

  pinMode(MQ6_DIGITAL, INPUT);   // 🔥 Gas sensor pin

  // 📡 WiFi
  WiFi.begin(ssid, password);
  Serial.print("Connecting");

  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.print(".");
  }

  Serial.println("\nConnected!");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());   // 🔥 USE THIS IN ANDROID

  // 🔌 Sensors
  Wire.begin(21, 22);   // SDA, SCL
  mpu.initialize();
  dht.begin();

  gpsSerial.begin(9600, SERIAL_8N1, 16, 17);

  // 🌐 Server
  server.on("/data", handleData);
  server.begin();

  Serial.println("Server Started!");
}

// ===== LOOP =====
void loop() {
  server.handleClient();
}