package com.example.atomikiergasia1;

import static com.example.atomikiergasia1.SpeedRecordDatabaseHelper.COLUMN_LONGITUDE;

import android.content.ContentValues;
import android.content.Context;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    LocationManager locationManager;
    TextView speedTextView;
    SharedPreferences preferences;
    private TextToSpeech textToSpeech;
    private boolean isKmPerHour = true; // Initially, set to true for km/h
    private float speedInMetersPerSecond = 0.0f;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 123;
    private boolean isWarningMessageSpoken = false;
    private boolean isAdditionalWarningMessageSpoken = false;

    TextView addressTextView;


    private String[] getSpeedRecordsFromDatabase() {
        SpeedRecordDatabaseHelper databaseHelper = new SpeedRecordDatabaseHelper(this);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = databaseHelper.getAllSpeedRecords();

        if (cursor == null) {
            return new String[0];
        }

        int recordCount = cursor.getCount();
        String[] speedRecords = new String[recordCount];
        int index = 0;

        // Access the column indices through the cursor
        int longitudeIndex = cursor.getColumnIndex(SpeedRecordDatabaseHelper.COLUMN_LONGITUDE);
        int latitudeIndex = cursor.getColumnIndex(SpeedRecordDatabaseHelper.COLUMN_LATITUDE);
        int speedIndex = cursor.getColumnIndex(SpeedRecordDatabaseHelper.COLUMN_SPEED);
        int timestampIndex = cursor.getColumnIndex(SpeedRecordDatabaseHelper.COLUMN_TIMESTAMP);

        while (cursor.moveToNext()) {
            double longitude = cursor.getDouble(longitudeIndex);
            double latitude = cursor.getDouble(latitudeIndex);
            float speed = cursor.getFloat(speedIndex);
            String timestamp = cursor.getString(timestampIndex);

            String record = String.format("Location: (%.6f, %.6f)\nSpeed: %.2f km/h\nTimestamp: %s",
                    latitude, longitude, speed, timestamp);

            speedRecords[index++] = record;
        }

        cursor.close();
        db.close();

        return speedRecords;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        speedTextView = findViewById(R.id.speedTextView);

        addressTextView = findViewById(R.id.addressTextView);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);


        // Set up TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TextToSpeech", "Language not supported");
                }
            } else {
                Log.e("TextToSpeech", "Initialization failed");
            }
        });

        speedInMetersPerSecond = 0.0f;

        // Check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        }

        setSpeedLimit(50.0f); // Example speed limit - 50.0 km/h

        setupButtonClickListeners();

        Button startButton = findViewById(R.id.startButton);

        // Set an OnClickListener for the "Start" button
        startButton.setOnClickListener(v -> {
            startSpeedCalculation();
        });

        Button stopButton = findViewById(R.id.stopButton);

        // Set an OnClickListener for the "Stop" button
        stopButton.setOnClickListener(v -> {
            stopSpeedCalculation();
        });

        addExampleSpeedRecords();

    }

    // Example method to add some speed records to the database
    private void addExampleSpeedRecords() {
        SpeedRecordDatabaseHelper databaseHelper = new SpeedRecordDatabaseHelper(this);

        //databaseHelper.insertSpeedRecord(this,0.0, 0.0, 10.0f, "2023-11-03 12:10:06");
        //databaseHelper.insertSpeedRecord(this,1.0, 1.0, 15.0f, "2023-11-04 12:16:25");
        //databaseHelper.insertSpeedRecord(this,2.0, 2.0, 20.0f, "2023-11-05 12:17:34");
        databaseHelper.insertSpeedRecord(this,0.0, 0.0, 10.0f, formatTimestamp(System.currentTimeMillis()));
    }

    // Separate method to start location updates
    private void startSpeedCalculation() {
        speedInMetersPerSecond = 0.0f;


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
        } else {
            // Permission is granted, register the location listener to start receiving updates
            try {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000,  // Update every 1 second
                        0,     // Minimum distance 0 meters
                        locationListener
                );
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }


    private void stopSpeedCalculation() {
        locationManager.removeUpdates(locationListener);
        speedTextView.setText("Speed: N/A");
        speedTextView.setTextColor(Color.BLACK);
        addressTextView.setText("Address not available");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start listening for location updates
                startSpeedCalculation();
            } else {
                // Permission denied, handle it (e.g., show a message to the user)
                showPermissionDeniedDialog();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Location Permission Denied")
                .setMessage("This app requires location permission to function properly. Please grant the location permission.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }


    // Method to set the user-defined speed limit
    private void setSpeedLimit(float limit) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat("speed_limit", limit);
        editor.apply();
    }

    // Method to get the user-defined speed limit with a default value
    private float getSpeedLimit() {
        return preferences.getFloat("speed_limit", 0.0f);
    }

    private void setupButtonClickListeners() {
        // Switch Mode Button
        Button switchModeButton = findViewById(R.id.switchModeButton);
        switchModeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Toggle the speed measurement unit when the button is clicked
                isKmPerHour = !isKmPerHour;
                updateSpeedDisplay(speedInMetersPerSecond);
            }
        });

        // Set Speed Limit Button
        Button setSpeedLimitButton = findViewById(R.id.setSpeedLimitButton);
        setSpeedLimitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showSetSpeedLimitDialog();
            }
        });

        // View Records Button
        Button viewRecordsButton = findViewById(R.id.viewRecordsButton);
        viewRecordsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RecordsActivity.class);
                startActivity(intent);
            }
        });
    }

    private void showSetSpeedLimitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.set_speed_limit_dialog, null);
        builder.setView(dialogView);

        final EditText speedLimitEditText = dialogView.findViewById(R.id.speedLimitEditText);
        final TextView currentSpeedLimitTextView = dialogView.findViewById(R.id.currentSpeedLimitTextView);

        // Retrieve the current speed limit and update the text initially
        float currentSpeedLimit = getSpeedLimit();
        currentSpeedLimitTextView.setText("Current Speed Limit: " + currentSpeedLimit + " km/h");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String speedLimitStr = speedLimitEditText.getText().toString();
                if (!speedLimitStr.isEmpty()) {
                    // Convert the input to a float and set the speed limit
                    float speedLimit = Float.parseFloat(speedLimitStr);
                    setSpeedLimit(speedLimit);
                    currentSpeedLimitTextView.setText("Current Speed Limit: " + speedLimit + " km/h");
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            float speedLimit = getSpeedLimit();
            float speedInMetersPerSecond = location.getSpeed(); // Speed in m/s
            float speedInKmPerHour = speedInMetersPerSecond * 3.6f; // Convert to km/h
            updateSpeedDisplay(speedInKmPerHour);
            // Get the address from the latitude and longitude
            String address = getAddressFromLocation(location.getLatitude(), location.getLongitude());
            displayAddress(address);

            // Check if the speed is above the limit
            if (speedInKmPerHour > speedLimit) {
                speedTextView.setTextColor(Color.RED);
                // Condition (a): Check if the warning message has not been spoken
                if (!isWarningMessageSpoken) {
                    String message = "You are exceeding the speed limit!";
                    textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
                    // Insert the violation record into the database (SQLite)
                    insertSpeedViolationRecord(location.getLongitude(), location.getLatitude(), speedInKmPerHour);
                    isWarningMessageSpoken = true;
                }
                // Condition (b): Check if the additional warning message has not been spoken and the speed has increased by 15 km/h
                if (!isAdditionalWarningMessageSpoken && (speedInKmPerHour - speedLimit) >= 15.0f) {
                    // Speak additional warning message
                    String additionalMessage = "Your speed is 15 km/h above the limit!";
                    textToSpeech.speak(additionalMessage, TextToSpeech.QUEUE_FLUSH, null, null);
                    insertSpeedViolationRecord(location.getLongitude(), location.getLatitude(), speedInKmPerHour);
                    isAdditionalWarningMessageSpoken = true;
                }
            } else {
                speedTextView.setTextColor(Color.BLACK);
                isWarningMessageSpoken = false;
                isAdditionalWarningMessageSpoken = false;
            }
        }
    };

    private void updateSpeedDisplay(float speed) {
        if (isKmPerHour) {
            speedTextView.setText(String.format("%.2f km/h", speed));
        } else {
            speedTextView.setText(String.format("%.2f m/s", speed));
        }
    }

    //Method for converting location to address
    private String getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
        String address = "Address not found";

        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                Address returnedAddress = addresses.get(0);

                // You can customize how the address is formatted based on your needs
                address = String.format(
                        "%s, %s, %s, %s",
                        returnedAddress.getAddressLine(0),
                        returnedAddress.getLocality(),
                        returnedAddress.getAdminArea(),
                        returnedAddress.getCountryName()
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return address;
    }

    private void displayAddress(String address) {
        // Split the address by commas
        String[] addressLines = address.split(", ");

        // Ensure that there are at least three parts before accessing them
        if (addressLines.length >= 3) {
            // Concatenate the first three parts of the address
            String formattedAddress = addressLines[0] + ", " + addressLines[1] + ", " + addressLines[2];

            // Assuming you have a TextView for displaying the address
            TextView addressTextView = findViewById(R.id.addressTextView);
            addressTextView.setText(formattedAddress);
        } else {
            // Handle the case where there are not enough parts in the address
            // You can display the entire address or show an error message
            TextView addressTextView = findViewById(R.id.addressTextView);
            addressTextView.setText(address);
        }
    }
    

    // Method to insert the speed violation record into SQLite database
    private void insertSpeedViolationRecord(double longitude, double latitude, float speed) {
        SpeedRecordDatabaseHelper databaseHelper = new SpeedRecordDatabaseHelper(this);
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SpeedRecordDatabaseHelper.COLUMN_LONGITUDE, longitude);
        values.put(SpeedRecordDatabaseHelper.COLUMN_LATITUDE, latitude);
        values.put(SpeedRecordDatabaseHelper.COLUMN_SPEED, speed);

        // Get the current timestamp
        long timestampMillis = System.currentTimeMillis();
        String timestamp = formatTimestamp(timestampMillis);

        values.put(SpeedRecordDatabaseHelper.COLUMN_TIMESTAMP, timestamp);
        db.insert(SpeedRecordDatabaseHelper.TABLE_RECORDS, null, values);
        db.close();
    }

    private String formatTimestamp(long timestampMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date(timestampMillis);
        return sdf.format(date);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Remove location updates to stop listening when the activity is paused
        locationManager.removeUpdates(locationListener);
    }
}