package com.example.smarttasbeeh;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Vibrator;
import android.os.Build;
import android.os.VibrationEffect;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnalysisActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvCount, tvTapHint, tvPausedOverlay, tvReportSummary, tvReportDetails;
    private View btnTap, scrollReport; 
    private FrameLayout graphContainer;
    private FloatingActionButton fabPause;
    private LinearLayout layoutPauseOptions;
    private Button btnReset, btnShowReport;
    
    // State
    private int currentCount = 0;
    private final int TARGET = 100;
    private boolean isPaused = false;
    private long lastTapTime = 0;
    
    // Data: Stores DURATION (ms) of each Zikr (i.e., time between taps)
    // zikrDurations[0] = duration of 1st zikr (Finish Tap 1 - Start).
    // Actually, user taps AFTER reciting.
    // Tap 0 (Start) -> Recite -> Tap 1 (End of 1st). duration = T1 - T0.
    private ArrayList<Long> zikrDurations = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        initViews();
        setupListeners();
        setupBottomNav();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        tvCount = findViewById(R.id.tvCount);
        tvTapHint = findViewById(R.id.tvTapHint);
        tvPausedOverlay = findViewById(R.id.tvPausedOverlay);
        btnTap = findViewById(R.id.tapActionView); 
        
        fabPause = findViewById(R.id.fabPause);
        layoutPauseOptions = findViewById(R.id.layoutPauseOptions);
        btnReset = findViewById(R.id.btnResetAnalysis);
        btnShowReport = findViewById(R.id.btnShowReport);
        
        scrollReport = findViewById(R.id.scrollReport);
        tvReportSummary = findViewById(R.id.tvReportSummary);
        tvReportDetails = findViewById(R.id.tvReportDetails);
        graphContainer = findViewById(R.id.graphContainer);
        
        // Initial State
        fabPause.setVisibility(View.INVISIBLE);
        scrollReport.setVisibility(View.INVISIBLE);
        tvPausedOverlay.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnTap.setOnClickListener(v -> handleTap());
        
        fabPause.setOnClickListener(v -> togglePause());
        
        btnReset.setOnClickListener(v -> resetAnalysis());
        
        btnShowReport.setOnClickListener(v -> generateReport());
    }

    private void handleTap() {
        if (currentCount >= TARGET) return;
        if (isPaused) {
            // Should be blocked by UI, but safety check
            return; 
        }

        long now = System.currentTimeMillis();
        
        // Feedback
        performFeedback();

        if (currentCount == 0) {
            // First Tap: STARTS the timer for Zikr #1
            lastTapTime = now;
            currentCount++; // Wait, if count=0, this is "Start"?
            // Suggestion: Tap 1 starts it.
            // Requirement: "Tap 100 times".
            // If I tap once, have I completed 1 zikr? 
            // Usually, user recites then taps. So Tap 1 = Done 1.
            // But we need a reference time.
            // Let's assume Tap 1 is "Started". Count remains 0? Or 1?
            // "Timer... time taken for one zikr to another (from 0 to 100)"
            // Logic:
            // Tap (Start) -> Count 0? Or just "Started".
            // Tap (End #1) -> Count 1. Duration = T_end1 - T_start.
            // So we need 101 taps for 100 zikrs if strictly measuring duration?
            // OR we use the first tap as "Count 1 completed" but duration is unknown/approx?
            // "user can count his specific Zikhr for 100 times... analysis... time taken for one zikhr to another"
            // Let's treat Tap 1 as START of Session. Count = 0.
            // Tap 2 = Count 1. Duration = T2-T1.
            // Tap 101 = Count 100.
            
            // Re-reading: "count... for 100 times" commonly means I press the button 100 times.
            // If I press 100 times, I have 99 intervals.
            // If I want 100 intervals, I need a "Start" button or 1st tap is start.
            // Let's assume 1st Tap = Start (Count 0 -> 1?). 
            // If Text says "0", and I tap, it becomes "1". 
            // I cannot measure the speed of the 1st one because I don't know when it started.
            // I will start measuring intervals from Count 2 onwards (Interval 1-2).
            // BUT user wants analysis for 0 to 100.
            // Let's start timestamp on 1st tap (Count 1).
            // Valid duration data starts appearing from Count 2.
            // Count 1: "Started". Duration: N/A (or 0).
            
            lastTapTime = now;
            currentCount = 1; 
            updateUI();
            
            tvTapHint.setVisibility(View.GONE);
            fabPause.setVisibility(View.VISIBLE);
            fabPause.setImageResource(R.drawable.ic_pause);
            
            // Clean slate
            zikrDurations.clear();
            
        } else {
            // Count > 0
            long duration = now - lastTapTime;
            zikrDurations.add(duration);
            
            lastTapTime = now;
            currentCount++;
            updateUI();
            
            if (currentCount == TARGET) {
                finishAnalysis();
            }
        }
    }
    
    private void togglePause() {
        if (currentCount == 0 || currentCount >= TARGET) return;
        
        isPaused = !isPaused;
        
        if (isPaused) {
            // Entering Pause
            fabPause.setImageResource(R.drawable.ic_play_circle); // Play icon to resume
            layoutPauseOptions.setVisibility(View.VISIBLE);
            tvPausedOverlay.setVisibility(View.VISIBLE);
            btnTap.setEnabled(false); // Disable tap
            scrollReport.setVisibility(View.INVISIBLE); // Hide previous report if any, wait for user request
            
        } else {
            // Resuming
            fabPause.setImageResource(R.drawable.ic_pause);
            layoutPauseOptions.setVisibility(View.GONE);
            tvPausedOverlay.setVisibility(View.GONE);
            btnTap.setEnabled(true);
            scrollReport.setVisibility(View.INVISIBLE);
            
            // Reset base time so pause duration isn't counted in next interval
            lastTapTime = System.currentTimeMillis(); 
        }
    }
    
    private void resetAnalysis() {
        currentCount = 0;
        zikrDurations.clear();
        isPaused = false;
        lastTapTime = 0;
        
        layoutPauseOptions.setVisibility(View.GONE);
        scrollReport.setVisibility(View.INVISIBLE);
        tvPausedOverlay.setVisibility(View.GONE);
        fabPause.setVisibility(View.INVISIBLE);
        btnTap.setEnabled(true);
        tvTapHint.setVisibility(View.VISIBLE);
        
        updateUI();
    }
    
    private void generateReport() {
        if (zikrDurations.isEmpty()) {
            Toast.makeText(this, "No data yet", Toast.LENGTH_SHORT).show();
            return;
        }
        
        scrollReport.setVisibility(View.VISIBLE);
        
        // Stats
        long totalDur = 0;
        for (long d : zikrDurations) totalDur += d;
        double avgMillis = (double) totalDur / zikrDurations.size();
        double avgSec = avgMillis / 1000.0;
        
        tvReportSummary.setText(String.format(Locale.US, "Count: %d | Avg Speed: %.2f sec", currentCount, avgSec));
        
        // Detailed Text
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < zikrDurations.size(); i++) {
            // Zikr # refers to interval. 
            // Durations[0] is interval between Tap 1 (Count 1) and Tap 2 (Count 2).
            // So it represents the speed of the 2nd zikr recited? 
            // Or if we assume start time was correct, 1st tap was end of 1st.
            // Let's Label them as "Interval 1-2", "Interval 2-3" etc.
            // Or "Zikr #2" (assuming #1 was setup).
            sb.append(String.format(Locale.US, "Zikr %d : %.2f s\n", (i+2), zikrDurations.get(i)/1000.0));
        }
        tvReportDetails.setText(sb.toString());
        
        // Graph
        drawGraph();
    }
    
    private void finishAnalysis() {
        fabPause.setVisibility(View.INVISIBLE); // Hide pause
        btnTap.setEnabled(false); // Disable
        
        // Auto-show report logic "give the whole report"
        generateReport();
        
        // Show Reset button in the report area? Or main Reset option?
        // User requirements: "After watching report... resume and completes... or reset".
        // But here we finished 100.
        // Let's show Reset button prominent or rely on the Pause/Reset flow?
        // Actually, if finished, we are "Paused" effectively.
        // Let's manually trigger options visibility but without Resume option.
        
        layoutPauseOptions.setVisibility(View.GONE); // Hide pause options
        // Show a final Reset button instead?
        // Re-using Reset button from pause menu but putting it below report?
        // Handled: The flow ends here for 100.
        // User said: "counter goes to 0" after completing.
        // Resetting automatically?
        // "after completing... counter goes to 0. And the analysis... will be provided"
        // This implies: Show Report AND Reset Count.
        
        // Implementation:
        // 1. Show Report.
        // 2. Reset Count variable but keep Report visible?
        // Visually: Count becomes 0.
        
        currentCount = 0;
        updateUI(); 
        
        // Keep Report Visible.
        // Since count is 0, user can start again?
        // Yes.
        lastTapTime = 0;
        zikrDurations.clear(); // Clear internal data?
        // If we clear data, report is empty?
        // We must KEEP data for report until next start.
        // So we decouple data clearing.
        // Clear data on NEXT First Tap.
        
        fabPause.setVisibility(View.INVISIBLE);
        tvTapHint.setVisibility(View.VISIBLE);
        btnTap.setEnabled(true);
    }

    private void updateUI() {
        tvCount.setText(String.valueOf(currentCount));
        progressBar.setProgress(currentCount);
    }

    private void performFeedback() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(50);
            }
        }
    }
    
    private void drawGraph() {
        graphContainer.removeAllViews();
        GraphView graph = new GraphView(this, zikrDurations);
        graphContainer.addView(graph);
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
    
    // Simple custom view for graph
    private class GraphView extends View {
        private List<Long> data;
        private Paint paintLine, paintDot;
        
        public GraphView(Context context, List<Long> data) {
            super(context);
            this.data = new ArrayList<>(data);
            
            paintLine = new Paint();
            paintLine.setColor(0xFF2962FF); // tasbeeh_blue_primary
            paintLine.setStrokeWidth(4f);
            paintLine.setAntiAlias(true);
            
            paintDot = new Paint();
            paintDot.setColor(0xFF2979FF); // lighter blue accent
            paintDot.setStyle(Paint.Style.FILL);
            paintDot.setAntiAlias(true);
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (data == null || data.size() < 2) return;
            
            float width = getWidth();
            float height = getHeight();
            float padding = 20f;
            
            float maxVal = 0;
            for (long d : data) if (d > maxVal) maxVal = d;
            if (maxVal == 0) maxVal = 1;

            float xStep = (width - 2 * padding) / (data.size() - 1);
            
            // Draw lines
            float prevX = padding;
            float prevY = height - padding - ((data.get(0) / maxVal) * (height - 2 * padding));
            
            for (int i = 1; i < data.size(); i++) {
                float x = padding + i * xStep;
                float y = height - padding - ((data.get(i) / maxVal) * (height - 2 * padding));
                
                canvas.drawLine(prevX, prevY, x, y, paintLine);
                canvas.drawCircle(prevX, prevY, 6f, paintDot);
                
                prevX = x;
                prevY = y;
            }
            canvas.drawCircle(prevX, prevY, 6f, paintDot);
        }
    }
}
