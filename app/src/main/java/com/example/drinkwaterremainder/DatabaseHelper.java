package com.example.drinkwaterremainder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "DrinkWater.db";
    private static final int DATABASE_VERSION = 1;
    
    // Tables
    private static final String TABLE_DAILY_INTAKE = "daily_intake";
    private static final String TABLE_WATER_RECORDS = "water_records";
    private static final String TABLE_SETTINGS = "settings";
    
    // Daily intake table columns
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_TOTAL_INTAKE = "total_intake";
    private static final String COLUMN_TARGET = "target";
    private static final String COLUMN_GOAL_ACHIEVED = "goal_achieved";
    
    // Water records table columns
    private static final String COLUMN_RECORD_ID = "record_id";
    private static final String COLUMN_RECORD_DATE = "date";
    private static final String COLUMN_TIME = "time";
    private static final String COLUMN_AMOUNT = "amount";
    
    // Settings table columns
    private static final String COLUMN_SETTING_KEY = "setting_key";
    private static final String COLUMN_SETTING_VALUE = "setting_value";
    
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create daily intake table
        String CREATE_DAILY_INTAKE_TABLE = "CREATE TABLE " + TABLE_DAILY_INTAKE + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_DATE + " TEXT UNIQUE,"
                + COLUMN_TOTAL_INTAKE + " INTEGER DEFAULT 0,"
                + COLUMN_TARGET + " INTEGER DEFAULT 2000,"
                + COLUMN_GOAL_ACHIEVED + " INTEGER DEFAULT 0"
                + ")";
        
        // Create water records table
        String CREATE_WATER_RECORDS_TABLE = "CREATE TABLE " + TABLE_WATER_RECORDS + "("
                + COLUMN_RECORD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_RECORD_DATE + " TEXT,"
                + COLUMN_TIME + " TEXT,"
                + COLUMN_AMOUNT + " INTEGER"
                + ")";
        
        // Create settings table
        String CREATE_SETTINGS_TABLE = "CREATE TABLE " + TABLE_SETTINGS + "("
                + COLUMN_SETTING_KEY + " TEXT PRIMARY KEY,"
                + COLUMN_SETTING_VALUE + " TEXT"
                + ")";
        
        db.execSQL(CREATE_DAILY_INTAKE_TABLE);
        db.execSQL(CREATE_WATER_RECORDS_TABLE);
        db.execSQL(CREATE_SETTINGS_TABLE);
        
        // Insert default settings
        insertDefaultSettings(db);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DAILY_INTAKE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WATER_RECORDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }
    
    private void insertDefaultSettings(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_SETTING_KEY, "default_quantity");
        values.put(COLUMN_SETTING_VALUE, "250");
        db.insert(TABLE_SETTINGS, null, values);
        
        values.clear();
        values.put(COLUMN_SETTING_KEY, "daily_target");
        values.put(COLUMN_SETTING_VALUE, "2000");
        db.insert(TABLE_SETTINGS, null, values);
    }
    
    // Get current date in YYYY-MM-DD format
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    // Initialize today's record if it doesn't exist
    public void initializeTodaysRecord() {
        SQLiteDatabase db = this.getWritableDatabase();
        String today = getCurrentDate();
        
        Cursor cursor = db.query(TABLE_DAILY_INTAKE, null, 
                COLUMN_DATE + " = ?", new String[]{today}, 
                null, null, null);
        
        if (cursor.getCount() == 0) {
            // Create today's record
            ContentValues values = new ContentValues();
            values.put(COLUMN_DATE, today);
            values.put(COLUMN_TOTAL_INTAKE, 0);
            values.put(COLUMN_TARGET, getDailyTarget());
            values.put(COLUMN_GOAL_ACHIEVED, 0);
            db.insert(TABLE_DAILY_INTAKE, null, values);
        }
        
        cursor.close();
        // Don't close db here - let SQLiteOpenHelper manage it
    }
    
    // Add water intake for today
    public void addWaterIntake(int amount) {
        SQLiteDatabase db = this.getWritableDatabase();
        String today = getCurrentDate();
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        
        // Initialize today's record if needed
        initializeTodaysRecord();
        
        // Add water record
        ContentValues recordValues = new ContentValues();
        recordValues.put(COLUMN_RECORD_DATE, today);
        recordValues.put(COLUMN_TIME, currentTime);
        recordValues.put(COLUMN_AMOUNT, amount);
        long recordId = db.insert(TABLE_WATER_RECORDS, null, recordValues);
        
        // Update daily total
        db.execSQL("UPDATE " + TABLE_DAILY_INTAKE + 
                   " SET " + COLUMN_TOTAL_INTAKE + " = " + COLUMN_TOTAL_INTAKE + " + ? " +
                   " WHERE " + COLUMN_DATE + " = ?", 
                   new Object[]{amount, today});
        
        // Check if goal is achieved
        int totalIntake = getTodaysTotalIntake();
        int target = getDailyTarget();
        if (totalIntake >= target) {
            db.execSQL("UPDATE " + TABLE_DAILY_INTAKE + 
                       " SET " + COLUMN_GOAL_ACHIEVED + " = 1 " +
                       " WHERE " + COLUMN_DATE + " = ?", 
                       new Object[]{today});
        }
        
        // Don't close db here - let SQLiteOpenHelper manage it
    }
    
    // Get today's total intake
    public int getTodaysTotalIntake() {
        SQLiteDatabase db = this.getReadableDatabase();
        String today = getCurrentDate();
        int totalIntake = 0;
        
        Cursor cursor = db.query(TABLE_DAILY_INTAKE, 
                new String[]{COLUMN_TOTAL_INTAKE}, 
                COLUMN_DATE + " = ?", new String[]{today}, 
                null, null, null);
        
        if (cursor.moveToFirst()) {
            totalIntake = cursor.getInt(0);
        }
        
        cursor.close();
        // Don't close db here - let SQLiteOpenHelper manage it
        return totalIntake;
    }
    
    // Get today's water records
    public List<WaterRecord> getTodaysRecords() {
        List<WaterRecord> records = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String today = getCurrentDate();
        
        Cursor cursor = db.query(TABLE_WATER_RECORDS, null, 
                COLUMN_RECORD_DATE + " = ?", new String[]{today}, 
                null, null, COLUMN_RECORD_ID + " DESC");
        
        if (cursor.moveToFirst()) {
            do {
                WaterRecord record = new WaterRecord();
                record.id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RECORD_ID));
                record.date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECORD_DATE));
                record.time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME));
                record.amount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT));
                records.add(record);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        // Don't close db here - let SQLiteOpenHelper manage it
        return records;
    }
    
    // Delete a water record
    public void deleteWaterRecord(int recordId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String today = getCurrentDate();
        
        // Get the amount of the record to be deleted
        Cursor cursor = db.query(TABLE_WATER_RECORDS, 
                new String[]{COLUMN_AMOUNT}, 
                COLUMN_RECORD_ID + " = ?", new String[]{String.valueOf(recordId)}, 
                null, null, null);
        
        int amount = 0;
        if (cursor.moveToFirst()) {
            amount = cursor.getInt(0);
        }
        cursor.close();
        
        // Delete the record
        db.delete(TABLE_WATER_RECORDS, COLUMN_RECORD_ID + " = ?", 
                new String[]{String.valueOf(recordId)});
        
        // Update daily total
        if (amount > 0) {
            db.execSQL("UPDATE " + TABLE_DAILY_INTAKE + 
                       " SET " + COLUMN_TOTAL_INTAKE + " = " + COLUMN_TOTAL_INTAKE + " - ? " +
                       " WHERE " + COLUMN_DATE + " = ?", 
                       new Object[]{amount, today});
            
            // Update goal achievement status
            int totalIntake = getTodaysTotalIntake();
            int target = getDailyTarget();
            int goalAchieved = totalIntake >= target ? 1 : 0;
            db.execSQL("UPDATE " + TABLE_DAILY_INTAKE + 
                       " SET " + COLUMN_GOAL_ACHIEVED + " = ? " +
                       " WHERE " + COLUMN_DATE + " = ?", 
                       new Object[]{goalAchieved, today});
        }
        
        // Don't close db here - let SQLiteOpenHelper manage it
    }
    
    // Get daily target
    public int getDailyTarget() {
        return Integer.parseInt(getSetting("daily_target", "2000"));
    }
    
    // Set daily target
    public void setDailyTarget(int target) {
        setSetting("daily_target", String.valueOf(target));
    }
    
    // Get default quantity
    public int getDefaultQuantity() {
        return Integer.parseInt(getSetting("default_quantity", "250"));
    }
    
    // Set default quantity
    public void setDefaultQuantity(int quantity) {
        setSetting("default_quantity", String.valueOf(quantity));
    }
    
    // Get setting value
    private String getSetting(String key, String defaultValue) {
        SQLiteDatabase db = this.getReadableDatabase();
        String value = defaultValue;
        
        Cursor cursor = db.query(TABLE_SETTINGS, 
                new String[]{COLUMN_SETTING_VALUE}, 
                COLUMN_SETTING_KEY + " = ?", new String[]{key}, 
                null, null, null);
        
        if (cursor.moveToFirst()) {
            value = cursor.getString(0);
        }
        
        cursor.close();
        // Don't close db here - let SQLiteOpenHelper manage it
        return value;
    }
    
    // Set setting value
    private void setSetting(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SETTING_VALUE, value);
        
        int rowsAffected = db.update(TABLE_SETTINGS, values, 
                COLUMN_SETTING_KEY + " = ?", new String[]{key});
        
        if (rowsAffected == 0) {
            values.put(COLUMN_SETTING_KEY, key);
            db.insert(TABLE_SETTINGS, null, values);
        }
        
        // Don't close db here - let SQLiteOpenHelper manage it
    }
    
    // Get weekly average intake
    public double getWeeklyAverageIntake() {
        SQLiteDatabase db = this.getReadableDatabase();
        
        // Get date 7 days ago
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -6); // Last 7 days including today
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String weekAgo = sdf.format(calendar.getTime());
        
        Cursor cursor = db.query(TABLE_DAILY_INTAKE, 
                new String[]{"AVG(" + COLUMN_TOTAL_INTAKE + ")"}, 
                COLUMN_DATE + " >= ?", new String[]{weekAgo}, 
                null, null, null);
        
        double average = 0;
        if (cursor.moveToFirst()) {
            average = cursor.getDouble(0);
        }
        
        cursor.close();
        // Don't close db here - let SQLiteOpenHelper manage it
        return average;
    }
    
    // Get monthly total intake
    public int getMonthlyTotalIntake() {
        SQLiteDatabase db = this.getReadableDatabase();
        
        // Get first day of current month
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String monthStart = sdf.format(calendar.getTime());
        
        Cursor cursor = db.query(TABLE_DAILY_INTAKE, 
                new String[]{"SUM(" + COLUMN_TOTAL_INTAKE + ")"}, 
                COLUMN_DATE + " >= ?", new String[]{monthStart}, 
                null, null, null);
        
        int total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getInt(0);
        }
        
        cursor.close();
        // Don't close db here - let SQLiteOpenHelper manage it
        return total;
    }
    
    // Get current streak (consecutive days with goal achieved)
    public int getCurrentStreak() {
        SQLiteDatabase db = this.getReadableDatabase();
        int streak = 0;
        
        // Get data ordered by date descending
        Cursor cursor = db.query(TABLE_DAILY_INTAKE, 
                new String[]{COLUMN_DATE, COLUMN_GOAL_ACHIEVED}, 
                null, null, null, null, COLUMN_DATE + " DESC");
        
        if (cursor.moveToFirst()) {
            do {
                if (cursor.getInt(1) == 1) { // Goal achieved
                    streak++;
                } else {
                    break; // Streak broken
                }
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        // Don't close db here - let SQLiteOpenHelper manage it
        return streak;
    }
    
    // Get best day intake
    public int getBestDayIntake() {
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_DAILY_INTAKE, 
                new String[]{"MAX(" + COLUMN_TOTAL_INTAKE + ")"}, 
                null, null, null, null, null);
        
        int maxIntake = 0;
        if (cursor.moveToFirst()) {
            maxIntake = cursor.getInt(0);
        }
        
        cursor.close();
        // Don't close db here - let SQLiteOpenHelper manage it
        return maxIntake;
    }
    
    // Check if today's goal is achieved
    public boolean isTodaysGoalAchieved() {
        SQLiteDatabase db = this.getReadableDatabase();
        String today = getCurrentDate();
        boolean achieved = false;
        
        Cursor cursor = db.query(TABLE_DAILY_INTAKE, 
                new String[]{COLUMN_GOAL_ACHIEVED}, 
                COLUMN_DATE + " = ?", new String[]{today}, 
                null, null, null);
        
        if (cursor.moveToFirst()) {
            achieved = cursor.getInt(0) == 1;
        }
        
        cursor.close();
        // Don't close db here - let SQLiteOpenHelper manage it
        return achieved;
    }
    
    // Water record model class
    public static class WaterRecord {
        public int id;
        public String date;
        public String time;
        public int amount;
    }
}
