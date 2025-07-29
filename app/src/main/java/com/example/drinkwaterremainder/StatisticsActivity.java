package com.example.drinkwaterremainder;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;

public class StatisticsActivity extends AppCompatActivity {
    private TextView weeklyAverageText, monthlyTotalText, streakText, bestDayText;
    private DatabaseHelper databaseHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        
        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Statistics");
        }
        
        initializeViews();
        loadStatistics();
    }
    
    private void initializeViews() {
        weeklyAverageText = findViewById(R.id.weeklyAverageText);
        monthlyTotalText = findViewById(R.id.monthlyTotalText);
        streakText = findViewById(R.id.streakText);
        bestDayText = findViewById(R.id.bestDayText);
        
        databaseHelper = new DatabaseHelper(this);
    }
    
    private void loadStatistics() {
        // Get statistics from database
        double weeklyAverage = databaseHelper.getWeeklyAverageIntake();
        int monthlyTotal = databaseHelper.getMonthlyTotalIntake();
        int currentStreak = databaseHelper.getCurrentStreak();
        int bestDay = databaseHelper.getBestDayIntake();
        
        // Format and display data
        DecimalFormat df = new DecimalFormat("#.#");
        weeklyAverageText.setText(df.format(weeklyAverage) + " ml");
        monthlyTotalText.setText(monthlyTotal + " ml");
        streakText.setText(currentStreak + " days");
        bestDayText.setText(bestDay + " ml");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh statistics when returning to this activity
        loadStatistics();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
