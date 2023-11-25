package com.example.atomikiergasia1;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

public class SpeedRecordDatabaseHelper extends SQLiteOpenHelper{
    private static final String DATABASE_NAME = "SpeedRecords.db";
    private static final int DATABASE_VERSION = 1;

    // Define the table schema
    public static final String TABLE_RECORDS = "speed_records";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_SPEED = "speed";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    // Create the table
    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_RECORDS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_LONGITUDE + " REAL, " +
                    COLUMN_LATITUDE + " REAL, " +
                    COLUMN_SPEED + " REAL, " +
                    COLUMN_TIMESTAMP + " TEXT)";

    public SpeedRecordDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDS);
        onCreate(db);
    }

    public void insertSpeedRecord(Context context, double longitude, double latitude, float speed, String timestamp) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LONGITUDE, longitude);
        values.put(COLUMN_LATITUDE, latitude);
        values.put(COLUMN_SPEED, speed);
        values.put(COLUMN_TIMESTAMP, timestamp);


        try {
            long result = db.insert(TABLE_RECORDS, null, values);

            if (result == -1) {
                // Error inserting data
                Toast.makeText(context, "Failed to insert speed record", Toast.LENGTH_SHORT).show();
                Log.e("SpeedRecordDatabaseHelper", "Error inserting speed record");
            } else {
                // Data inserted successfully
                Toast.makeText(context, "Speed record inserted successfully", Toast.LENGTH_SHORT).show();
                Log.d("SpeedRecordDatabaseHelper", "Speed record inserted successfully");
            }
        } catch (Exception e) {
            // Handle any exceptions
            Toast.makeText(context, "Error inserting speed record", Toast.LENGTH_SHORT).show();
            Log.e("SpeedRecordDatabaseHelper", "Error inserting speed record", e);
        } finally {
            db.close();
        }
    }

    // Method to retrieve all speed records from the database
    public Cursor getAllSpeedRecords() {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(TABLE_RECORDS, null, null, null, null, null, null);
    }

}
