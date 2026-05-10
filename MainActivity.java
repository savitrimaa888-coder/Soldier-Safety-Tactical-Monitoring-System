package com.example.tacticalfieldcommunicationsystemwithiot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    TextView sensorText, locationText;
    Button btnFetch, btnLocation, btnSOS;
    FusedLocationProviderClient fusedLocationClient;

    //  CHANGE THIS TO YOUR ESP32 IP
    String ESP32_URL = "http://10.71.136.251/data";
    // CHANGE THIS TO EMERGENCY CONTACT
    String EMERGENCY_NUMBER = "6363790739";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorText = findViewById(R.id.sensorText);


        locationText = findViewById(R.id.locationText);
        btnFetch = findViewById(R.id.btnFetch);
        btnLocation = findViewById(R.id.btnLocation);
        btnSOS = findViewById(R.id.btnSOS);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnFetch.setOnClickListener(v -> getESP32Data());
        btnLocation.setOnClickListener(v -> getLastLocation());
        btnSOS.setOnClickListener(v -> sendSOS());
    }

    private void getLastLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                String locStr = "📍 Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude();
                locationText.setText(locStr);
            } else {
                Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Sends SOS via Intent to comply with Play Store SMS policies.
     * This avoids the need for the SEND_SMS permission which is restricted to Default Handlers.
     */
    private void sendSOS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            StringBuilder message = new StringBuilder("🚨 EMERGENCY SOS! I need help.");
            if (location != null) {
                message.append("\nMy Location: http://maps.google.com/maps?q=").append(location.getLatitude()).append(",").append(location.getLongitude());
            }

            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + EMERGENCY_NUMBER));
            intent.putExtra("sms_body", message.toString());

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "No SMS app found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 📡 FETCH DATA FROM ESP32
    private void getESP32Data() {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                ESP32_URL,
                null,
                response -> {
                    try {
                        double temp = response.getDouble("temp");
                        int gas = response.getInt("gas");
                        double motion = response.getDouble("motion");

                        String data = "🌡 Temp: " + temp +
                                "\n💨 Gas: " + gas +
                                "\n📡 Motion: " + motion;

                        sensorText.setText(data);



                        if (gas > 300 || motion > 20000) {
                            Toast.makeText(this, "⚠️ Emergency Detected!", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error parsing ESP32 data", e);
                    }
                },
                error -> {
                    Log.e("MainActivity", "ESP32 Connection Error");
                    Toast.makeText(this, "ESP32 Not Connected", Toast.LENGTH_SHORT).show();
                }
        );


        Volley.newRequestQueue(this).add(request);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
