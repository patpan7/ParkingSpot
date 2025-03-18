package com.example.parkingspot;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "parking.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_TICKETS = "tickets";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_OPERATOR = "operator";
    private static final String COLUMN_PLATE = "plate";
    private static final String COLUMN_DATETIME = "datetime";

    private static final String CREATE_TABLE_TICKETS =
            "CREATE TABLE " + TABLE_TICKETS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_OPERATOR + " TEXT, " +
                    COLUMN_PLATE + " TEXT, " +
                    COLUMN_DATETIME + " TEXT" +
                    ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_TICKETS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TICKETS);
        onCreate(db);
    }

    public void addTicket(String operator, String plate, String datetime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_OPERATOR, operator);
        values.put(COLUMN_PLATE, plate);
        values.put(COLUMN_DATETIME, convertToDatabaseFormat(datetime));

        db.insert(TABLE_TICKETS, null, values);
        db.close();
    }

    public Cursor getTickets(String dateFrom, String dateTo) {
        SQLiteDatabase db = this.getReadableDatabase();
        String fromFormatted = convertDateFormat(dateFrom) + " 00:00:00";
        String toFormatted = convertDateFormat(dateTo) + " 23:59:59";

        String query = "SELECT operator, COUNT(*) FROM tickets WHERE datetime BETWEEN ? AND ? GROUP BY operator";
        return db.rawQuery(query, new String[]{fromFormatted, toFormatted});
    }

    // ΝΕΑ ΜΕΘΟΔΟΣ: Ανάκτηση όλων των εισιτηρίων με λεπτομέρειες
    public Cursor getTicketsByOperator(String operator, String dateFrom, String dateTo) {
        SQLiteDatabase db = this.getReadableDatabase();
        String fromFormatted = convertDateFormat(dateFrom) + " 00:00:00";
        String toFormatted = convertDateFormat(dateTo) + " 23:59:59";

        Cursor cursor = db.rawQuery("SELECT * FROM tickets WHERE operator = ? AND datetime BETWEEN ? AND ?",
                new String[]{operator, fromFormatted, toFormatted});
        if (cursor == null || cursor.getCount() == 0) {
            Log.e("Database", "No tickets found for operator: " + operator + " from " + dateFrom + " to " + dateTo);
        }
        return cursor;
    }


    private String convertToDatabaseFormat(String date) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return outputFormat.format(inputFormat.parse(date));
        } catch (Exception e) {
            e.printStackTrace();
            return date;
        }
    }

    private String convertDateFormat(String date) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return outputFormat.format(inputFormat.parse(date));
        } catch (Exception e) {
            e.printStackTrace();
            return date;
        }
    }
}
