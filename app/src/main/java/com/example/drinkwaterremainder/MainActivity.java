package com.example.drinkwaterremainder;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import java.util.Locale;

import me.bastanfar.semicirclearcprogressbar.SemiCircleArcProgressBar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    CardView cardView;
    TextView textComplete,textTarget,textQuantity;
    ImageView addButton,mugButton,switchCup;
    LinearLayout addButtonLayout;
    int quantity,target,complete,percent,ID=0;
    SemiCircleArcProgressBar ProgressBar;
    ArrayList<String> id,time,value;
    CustomAdapter customAdapter;
    RecyclerView recyclerView;
    
    // SharedPreferences for data persistence
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "DrinkWaterPrefs";
    private static final String KEY_COMPLETE = "complete";
    private static final String KEY_TARGET = "target";
    private static final String KEY_QUANTITY = "quantity";
    private static final String KEY_ID_COUNT = "id_count";
    private static final String KEY_RECORDS = "records";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        initializeSharedPreferences();
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

        id= new ArrayList<>();
        time = new ArrayList<>();
        value = new ArrayList<>();
    }
    
    private void initializeSharedPreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }
    
    private void loadSavedData() {
        complete = sharedPreferences.getInt(KEY_COMPLETE, 0);
        target = sharedPreferences.getInt(KEY_TARGET, 2000); // Default 2000ml
        quantity = sharedPreferences.getInt(KEY_QUANTITY, 250); // Default 250ml
        ID = sharedPreferences.getInt(KEY_ID_COUNT, 0);
        
        textComplete.setText(String.valueOf(complete));
        textTarget.setText(String.valueOf(target));
        textQuantity.setText(String.valueOf(quantity));
        
        // Load saved records (simplified - in a real app you'd use a database)
        loadRecordsFromPrefs();
    }
    
    private void loadRecordsFromPrefs() {
        String recordsData = sharedPreferences.getString(KEY_RECORDS, "");
        if (!recordsData.isEmpty()) {
            String[] records = recordsData.split(";");
            for (String record : records) {
                if (!record.isEmpty()) {
                    String[] parts = record.split(",");
                    if (parts.length == 3) {
                        id.add(parts[0]);
                        time.add(parts[1]);
                        value.add(parts[2]);
                    }
                }
            }
        }
    }
    
    private void saveRecordsToPrefs() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < id.size(); i++) {
            if (i > 0) sb.append(";");
            sb.append(id.get(i)).append(",").append(time.get(i)).append(",").append(value.get(i));
        }
        sharedPreferences.edit().putString(KEY_RECORDS, sb.toString()).apply();
    }
    
    private void setupRecyclerView() {
        customAdapter = new CustomAdapter(this, id, time, value);
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
            complete = Integer.parseInt(textComplete.getText().toString());
            target = Integer.parseInt(textTarget.getText().toString());
            
            int update = quantity + complete;
            
            // Update UI
            textComplete.setText(String.valueOf(update));
            updateProgressBar();
            
            // Add record
            ID++;
            id.add(String.valueOf(ID));
            value.add(String.valueOf(quantity) + " ml");
            time.add(getCurrentTime());
            
            customAdapter.notifyDataSetChanged();
            
            // Save data
            saveData();
            
            // Show success message
            if (update >= target) {
                Toast.makeText(this, "🎉 Daily goal achieved! Great job!", Toast.LENGTH_LONG).show();
            } else {
                int remaining = target - update;
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
    
    private void saveData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_COMPLETE, Integer.parseInt(textComplete.getText().toString()));
        editor.putInt(KEY_TARGET, Integer.parseInt(textTarget.getText().toString()));
        editor.putInt(KEY_QUANTITY, Integer.parseInt(textQuantity.getText().toString()));
        editor.putInt(KEY_ID_COUNT, ID);
        editor.apply();
        
        saveRecordsToPrefs();
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
                    saveData();
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
                    saveData();
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
                    updateProgressBar();
                    saveData();
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
    
    private void deleteRecord(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Record");
        builder.setMessage("Are you sure you want to delete this water intake record?");
        
        builder.setPositiveButton("Delete", (dialog, which) -> {
            try {
                // Extract quantity from value and subtract from complete
                String valueStr = value.get(position).replace(" ml", "");
                int recordQuantity = Integer.parseInt(valueStr);
                int currentComplete = Integer.parseInt(textComplete.getText().toString());
                int newComplete = Math.max(0, currentComplete - recordQuantity);
                
                // Remove from lists
                id.remove(position);
                time.remove(position);
                value.remove(position);
                
                // Update UI
                textComplete.setText(String.valueOf(newComplete));
                updateProgressBar();
                customAdapter.notifyItemRemoved(position);
                customAdapter.notifyItemRangeChanged(position, id.size());
                
                // Save data
                saveData();
                
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
        // Clear all data
        complete = 0;
        ID = 0;
        id.clear();
        time.clear();
        value.clear();
        
        // Update UI
        textComplete.setText("0");
        updateProgressBar();
        customAdapter.notifyDataSetChanged();
        
        // Save data
        saveData();
        
        Toast.makeText(this, "Daily progress reset", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        saveData(); // Save data when app is paused
    }
}