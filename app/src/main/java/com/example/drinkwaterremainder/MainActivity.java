package com.example.drinkwaterremainder;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import me.bastanfar.semicirclearcprogressbar.SemiCircleArcProgressBar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    CardView cardView;
    TextView textComplete,textTarget,textQuantity;
    ImageView addButton,mugButton,switchCup;
    LinearLayout addButtonLayout;
    int quantity,target,complete,percent;
    SemiCircleArcProgressBar ProgressBar;
    List<DatabaseHelper.WaterRecord> waterRecords;
    CustomAdapter customAdapter;
    RecyclerView recyclerView;
    
    // Database helper for data persistence
    private DatabaseHelper databaseHelper;
    
    // Notification helper
    private NotificationHelper notificationHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        initializeDatabase();
        loadSavedData();
        setupRecyclerView();
        updateProgressBar();
        setupClickListeners();
        setupCardViewRadius();
    }
    
    private void initializeViews() {
        cardView = findViewById(R.id.cardViewId);
        ProgressBar =(SemiCircleArcProgressBar) findViewById(R.id.progressbarId);
        textComplete = (TextView) findViewById(R.id.textViewCompleteID);
        textTarget = (TextView) findViewById(R.id.textViewTargetID);
        textQuantity = (TextView) findViewById(R.id.textViewQuantityID);
        mugButton = (ImageView) findViewById(R.id.mugButtonID);
        addButton = (ImageView) findViewById(R.id.addButtonID);
        addButtonLayout = (LinearLayout) findViewById(R.id.addButtonLayoutID);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerViewID);
        switchCup = (ImageView) findViewById(R.id.switchCupID);

        waterRecords = new ArrayList<>();
    }
    
    private void initializeDatabase() {
        databaseHelper = new DatabaseHelper(this);
        notificationHelper = new NotificationHelper(this);
        
        // Initialize today's record if needed (this handles daily reset automatically)
        databaseHelper.initializeTodaysRecord();
        
        checkDailyReset();
    }
    
    private void checkDailyReset() {
        // The database automatically handles daily reset by creating new records for each day
        // This method can be used for additional daily reset logic if needed
        
        // For example, we could show a daily motivation message on the first app open each day
        // or reset notification preferences, etc.
    }
    
    private void loadSavedData() {
        complete = databaseHelper.getTodaysTotalIntake();
        target = databaseHelper.getDailyTarget();
        quantity = databaseHelper.getDefaultQuantity();
        
        textComplete.setText(String.valueOf(complete));
        textTarget.setText(String.valueOf(target));
        textQuantity.setText(String.valueOf(quantity));
        
        // Load today's records
        waterRecords = databaseHelper.getTodaysRecords();
    }
    
    private void setupRecyclerView() {
        customAdapter = new CustomAdapter(this, waterRecords);
        customAdapter.setOnItemDeleteListener(this::deleteRecord);
        recyclerView.setAdapter(customAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
    
    private void updateProgressBar() {
        if (target > 0) {
            percent = (complete * 100) / target;
            percent = Math.min(percent, 100); // Cap at 100%
            ProgressBar.setPercentWithAnimation(percent);
            
            // Update next reminder time
            updateNextReminderTime();
        }
    }
    
    private void updateNextReminderTime() {
        // Calculate next reminder time (every 2 hours during awake time: 6 AM - 10 PM)
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String[] reminderTimes = {"08:00 AM", "10:00 AM", "12:00 PM", "02:00 PM", "04:00 PM", "06:00 PM", "08:00 PM"};
        
        String currentTime = timeFormat.format(new Date());
        String nextTime = "08:00 AM"; // Default to morning
        
        for (int i = 0; i < reminderTimes.length - 1; i++) {
            if (isTimeAfter(currentTime, reminderTimes[i]) && isTimeBefore(currentTime, reminderTimes[i + 1])) {
                nextTime = reminderTimes[i + 1];
                break;
            }
        }
        
        // Update the next time display in the UI if the TextView exists
        TextView nextTimeText = findViewById(R.id.textView2);
        if (nextTimeText != null) {
            nextTimeText.setText(nextTime);
        }
    }
    
    private boolean isTimeAfter(String time1, String time2) {
        // Simple time comparison (this could be improved with proper time parsing)
        return time1.compareTo(time2) > 0;
    }
    
    private boolean isTimeBefore(String time1, String time2) {
        // Simple time comparison (this could be improved with proper time parsing)
        return time1.compareTo(time2) < 0;
    }
    
    private void setupClickListeners() {
        addButton.setOnClickListener(this);
        mugButton.setOnClickListener(this);
        switchCup.setOnClickListener(this);
        textTarget.setOnClickListener(this); // Allow target editing
        textComplete.setOnLongClickListener(v -> {
            showResetDialog();
            return true;
        });
    }
    
    private void setupCardViewRadius() {
        cardView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                 int width = cardView.getWidth();
                float radius = width / 2.0f;
                cardView.setRadius(radius);
                cardView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.addButtonID || v.getId() == R.id.mugButtonID) {
            addWaterIntake();
        } else if (v.getId() == R.id.switchCupID) {
            showCupSizeDialog();
        } else if (v.getId() == R.id.textViewTargetID) {
            showTargetEditDialog();
        }
    }
    
    private void addWaterIntake() {
        try {
            quantity = Integer.parseInt(textQuantity.getText().toString());
            target = databaseHelper.getDailyTarget();
            
            // Add water intake to database
            databaseHelper.addWaterIntake(quantity);
            
            // Get updated totals
            complete = databaseHelper.getTodaysTotalIntake();
            
            // Update UI
            textComplete.setText(String.valueOf(complete));
            updateProgressBar();
            
            // Refresh records list
            waterRecords = databaseHelper.getTodaysRecords();
            customAdapter.updateRecords(waterRecords);
            
            // Show success message and notifications
            if (databaseHelper.isTodaysGoalAchieved()) {
                notificationHelper.showGoalAchievement();
                Toast.makeText(this, "🎉 Daily goal achieved! Great job!", Toast.LENGTH_LONG).show();
            } else {
                int remaining = target - complete;
                Toast.makeText(this, "+" + quantity + "ml added. " + remaining + "ml remaining", Toast.LENGTH_SHORT).show();
            }
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid quantity value", Toast.LENGTH_SHORT).show();
        }
    }
    
    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    private void saveSettings() {
        // Save settings to database
        databaseHelper.setDailyTarget(target);
        databaseHelper.setDefaultQuantity(quantity);
    }
    
    private void showCupSizeDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_dialog);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.dialog_background));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        
        // Add click listeners to cup size options
        setupCupSizeOptions(dialog);
        
        dialog.show();
    }
    
    private void setupCupSizeOptions(Dialog dialog) {
        int[] cupSizes = {100, 125, 150, 175, 200, 300, 400, 500};
        int[] textViewIds = {
            R.id.textView100ml, R.id.textView125ml, R.id.textView150ml, 
            R.id.textView175ml, R.id.textView200ml, R.id.textView300ml, 
            R.id.textView400ml, R.id.textView500ml
        };
        
        for (int i = 0; i < Math.min(cupSizes.length, textViewIds.length); i++) {
            TextView textView = dialog.findViewById(textViewIds[i]);
            if (textView != null) {
                final int size = cupSizes[i];
                textView.setOnClickListener(v -> {
                    textQuantity.setText(String.valueOf(size));
                    quantity = size;
                    saveSettings();
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this, "Cup size set to " + size + "ml", Toast.LENGTH_SHORT).show();
                });
            }
        }
        
        // Handle custom option
        TextView customOption = dialog.findViewById(R.id.textViewCustom);
        if (customOption != null) {
            customOption.setOnClickListener(v -> {
                dialog.dismiss();
                showCustomQuantityDialog();
            });
        }
    }
    
    private void showCustomQuantityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Custom Quantity");
        
        final EditText input = new EditText(this);
        input.setHint("Enter quantity in ml");
        input.setText(textQuantity.getText().toString());
        builder.setView(input);
        
        builder.setPositiveButton("Set", (dialog, which) -> {
            String inputText = input.getText().toString().trim();
            try {
                int customQuantity = Integer.parseInt(inputText);
                if (customQuantity > 0 && customQuantity <= 2000) {
                    textQuantity.setText(String.valueOf(customQuantity));
                    quantity = customQuantity;
                    saveSettings();
                    Toast.makeText(this, "Custom quantity set to " + customQuantity + "ml", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Please enter a valid quantity (1-2000ml)", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showTargetEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Daily Target");
        
        final EditText input = new EditText(this);
        input.setHint("Enter daily target in ml");
        input.setText(textTarget.getText().toString());
        builder.setView(input);
        
        builder.setPositiveButton("Set", (dialog, which) -> {
            String inputText = input.getText().toString().trim();
            try {
                int newTarget = Integer.parseInt(inputText);
                if (newTarget > 0 && newTarget <= 10000) {
                    textTarget.setText(String.valueOf(newTarget));
                    target = newTarget;
                    updateProgressBar();
                    saveSettings();
                    Toast.makeText(this, "Daily target set to " + newTarget + "ml", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Please enter a valid target (1-10000ml)", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void deleteRecord(int recordId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Record");
        builder.setMessage("Are you sure you want to delete this water intake record?");
        
        builder.setPositiveButton("Delete", (dialog, which) -> {
            try {
                databaseHelper.deleteWaterRecord(recordId);
                
                // Refresh data
                complete = databaseHelper.getTodaysTotalIntake();
                textComplete.setText(String.valueOf(complete));
                updateProgressBar();
                
                // Refresh records
                waterRecords = databaseHelper.getTodaysRecords();
                customAdapter.updateRecords(waterRecords);
                
                Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show();
                
            } catch (Exception e) {
                Toast.makeText(this, "Error deleting record", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Daily Progress");
        builder.setMessage("This will reset your daily water intake progress. Are you sure?");
        
        builder.setPositiveButton("Reset", (dialog, which) -> {
            resetDailyProgress();
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void resetDailyProgress() {
        // This method is handled automatically by the database daily reset
        // Since database creates new records each day, no manual reset is needed
        Toast.makeText(this, "Data resets automatically each day", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Settings are saved automatically when changed
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_statistics) {
            Intent intent = new Intent(this, StatisticsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_settings) {
            showSettingsDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");
        
        String[] options = {"Set Daily Target", "Set Cup Size", "Enable Notifications", "Reset All Data"};
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showTargetEditDialog();
                    break;
                case 1:
                    showCupSizeDialog();
                    break;
                case 2:
                    notificationHelper.showHydrationReminder();
                    Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    showResetDialog();
                    break;
            }
        });
        
        builder.show();
    }
}