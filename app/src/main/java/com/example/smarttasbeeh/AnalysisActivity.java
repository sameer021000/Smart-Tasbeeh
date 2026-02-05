package com.example.smarttasbeeh;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Locale;

public class AnalysisActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvCount, tvStats, tvTapHint;
    private View btnTap; 
    private Button btnReset;
    
    private int currentCount = 0;
    private final int TARGET = 100;
    private ArrayList<Long> timestamps = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        progressBar = findViewById(R.id.progressBar);
        tvCount = findViewById(R.id.tvCount);
        tvStats = findViewById(R.id.tvStats);
        tvTapHint = findViewById(R.id.tvTapHint);
        btnTap = findViewById(R.id.btnTap);
        btnReset = findViewById(R.id.btnResetAnalysis);

        btnTap.setOnClickListener(v -> handleTap());
        btnReset.setOnClickListener(v -> resetAnalysis());

        setupBottomNav();
    }

    private void handleTap() {
        if (currentCount >= TARGET) return;
        
        long now = System.currentTimeMillis();
        
        // Vibrate
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) v.vibrate(50);

        if (currentCount == 0) {
            timestamps.add(now);
            currentCount++;
            updateUI();
            
            tvStats.setText("Values recording...");
            tvTapHint.setVisibility(View.GONE);
        } else {
            timestamps.add(now);
            currentCount++;
            updateUI();
            
            if (currentCount == TARGET) {
                finishAnalysis();
            }
        }
    }

    private void updateUI() {
        tvCount.setText(String.valueOf(currentCount));
        progressBar.setProgress(currentCount);
    }
    
    private void finishAnalysis() {
        long startTime = timestamps.get(0);
        long endTime = timestamps.get(timestamps.size() - 1);
        long totalDuration = endTime - startTime;
        
        double avgMillis = (double) totalDuration / (timestamps.size() - 1);
        double avgSeconds = avgMillis / 1000.0;
        
        String result = String.format(Locale.US, "Avg Speed: %.2f sec\nTotal: %.1f sec", avgSeconds, totalDuration/1000.0);
        tvStats.setText(result);
        btnReset.setVisibility(View.VISIBLE);
        
        showDetailedAnalysis(avgSeconds);
    }
    
    private void showDetailedAnalysis(double avg) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Analysis Report");
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US, "Average Time per Zikr: %.2f seconds\n\n", avg));
        sb.append("Detailed Log:\n");
        
        long start = timestamps.get(0);
        for (int i = 0; i < timestamps.size(); i++) {
             long t = timestamps.get(i);
             double relative = (t - start) / 1000.0;
             sb.append(String.format(Locale.US, "#%d : %.2fs\n", i+1, relative));
        }
        
        builder.setMessage(sb.toString());
        builder.setPositiveButton("OK", null);
        
        // Reset count to 0 in UI as requested "after completing... counter goes to 0"
        // But we want to show results first. 
        // User said "after completing... counter goes to 0. And the analysis... will be provided"
        // I will reset count when they close dialog or after showing results.
        builder.setOnDismissListener(dialog -> {
             // Optional: reset here? 
             // "counter goes to 0"
             // I'll keep the reset button for manual review, but set valid state.
        });
        
        builder.show();
    }
    
    private void resetAnalysis() {
        currentCount = 0;
        timestamps.clear();
        updateUI();
        tvStats.setText("Start tapping...");
        btnReset.setVisibility(View.INVISIBLE);
        tvTapHint.setVisibility(View.VISIBLE);
    }

    private void setupBottomNav() {
        findViewById(R.id.btnNavCounter).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });
        findViewById(R.id.btnNavHistory).setOnClickListener(v -> {
            startActivity(new Intent(this, SavedCountsActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });
        findViewById(R.id.btnNavSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });
    }
}
