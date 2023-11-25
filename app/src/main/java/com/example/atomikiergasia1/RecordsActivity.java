package com.example.atomikiergasia1;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ToggleButton;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.example.atomikiergasia1.SpeedRecordDatabaseHelper;

public class RecordsActivity extends AppCompatActivity {
    private ToggleButton toggleButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);

        toggleButton = findViewById(R.id.toggleButton);

        ListView recordslistView = findViewById(R.id.recordsListView);

        // Set the listener for the toggle button
        toggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            boolean isLastWeekRecords = isChecked;

            // Retrieve speed records from the database
            String[] speedRecords = getSpeedRecordsFromDatabase(isLastWeekRecords);

            // Create an ArrayAdapter to display the records
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, speedRecords);

            // Set the adapter to the ListView
            recordslistView.setAdapter(adapter);
        });
    }

    // Implement the getSpeedRecordsFromDatabase method as shown in MainActivity
    private String[] getSpeedRecordsFromDatabase(boolean isLastWeekRecords) {
        SpeedRecordDatabaseHelper databaseHelper = new SpeedRecordDatabaseHelper(this);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        String selection = null;
        String[] selectionArgs = null;

        if (isLastWeekRecords) {
            // Get the timestamp for a week ago
            String oneWeekAgoTimestamp = getOneWeekAgoTimestamp();
            selection = SpeedRecordDatabaseHelper.COLUMN_TIMESTAMP + " >= ?";
            selectionArgs = new String[]{oneWeekAgoTimestamp};
        }

        Cursor cursor = db.query(
                SpeedRecordDatabaseHelper.TABLE_RECORDS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

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

    private String getOneWeekAgoTimestamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7); // Subtract 7 days - 1 Week
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }
}
