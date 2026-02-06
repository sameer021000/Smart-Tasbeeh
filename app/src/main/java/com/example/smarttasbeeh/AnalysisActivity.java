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
    private TextView tvCount, tvTapHint, tvPausedOverlay, tvReportSummary;
    private View btnTap, reportView; 
    private FrameLayout graphContainer;
    private android.widget.TableLayout tableReport;
    private FloatingActionButton fabPause;
    private LinearLayout layoutPauseOptions;
    private Button btnReset, btnShowReport;
    
    private Button btnStartOverlay;
    
    // State
    private int currentCount = 0;
    private final int TARGET = 100;
    private boolean isPaused = false;
    private boolean hasStarted = false;
    private long lastTapTime = 0;
    
    // Data: Stores DURATION (ms) of each Zikr (i.e., time between taps)
    // Start -> Tap 1 (Duration 1) -> Tap 2 (Duration 2)...
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
        progressBar.setMax(TARGET); // Ensure progress scale matches target
        
        tvCount = findViewById(R.id.tvCount);
        tvTapHint = findViewById(R.id.tvTapHint);
        tvPausedOverlay = findViewById(R.id.tvPausedOverlay);
        btnTap = findViewById(R.id.tapActionView); 
        btnStartOverlay = findViewById(R.id.btnStartOverlay);
        
        fabPause = findViewById(R.id.fabPause);
        layoutPauseOptions = findViewById(R.id.layoutPauseOptions);
        btnReset = findViewById(R.id.btnResetAnalysis);
        btnShowReport = findViewById(R.id.btnShowReport);
        
        tableReport = findViewById(R.id.tableReport);
        tvReportSummary = findViewById(R.id.tvReportSummary);
        graphContainer = findViewById(R.id.graphContainer);
        
        // reportContainer is the linear layout holding summary + graph + table
        this.reportView = findViewById(R.id.reportContainer);
        
        // Initial State
        fabPause.setVisibility(View.INVISIBLE);
        reportView.setVisibility(View.GONE);
        tvPausedOverlay.setVisibility(View.GONE);
        layoutPauseOptions.setVisibility(View.GONE);
        
        // Disable tap until started
        btnTap.setEnabled(false);
        btnTap.setClickable(false);
        tvTapHint.setVisibility(View.GONE);
        
        btnStartOverlay.setVisibility(View.VISIBLE);
    }

    private void setupListeners() {
        btnTap.setOnClickListener(v -> handleTap());
        btnStartOverlay.setOnClickListener(v -> startAnalysisSession());
        fabPause.setOnClickListener(v -> togglePause());
        btnReset.setOnClickListener(v -> resetAnalysis());
        btnShowReport.setOnClickListener(v -> generateReport());
    }
    
    private void startAnalysisSession() {
        hasStarted = true;
        currentCount = 0;
        zikrDurations.clear();
        lastTapTime = System.currentTimeMillis();
        
        btnStartOverlay.setVisibility(View.GONE);
        tvTapHint.setVisibility(View.VISIBLE);
        
        btnTap.setEnabled(true);
        btnTap.setClickable(true);
        btnTap.setAlpha(1.0f);
        
        fabPause.setVisibility(View.VISIBLE);
        fabPause.setImageResource(R.drawable.ic_pause);
        
        updateUI();
    }

    private void handleTap() {
        if (!hasStarted || currentCount >= TARGET) return;
        if (isPaused) return; 
        
        long now = System.currentTimeMillis();
        performFeedback(); 

        long duration = now - lastTapTime;
        zikrDurations.add(duration);
        
        lastTapTime = now;
        currentCount++;
        updateUI();
        
        if (currentCount == TARGET) {
            finishAnalysis();
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
            btnTap.setEnabled(false); // Disable tap
            reportView.setVisibility(View.GONE); // Hide previous report if any, wait for user request
            
        } else {
            // Resuming
            fabPause.setImageResource(R.drawable.ic_pause);
            layoutPauseOptions.setVisibility(View.GONE);
            tvPausedOverlay.setVisibility(View.GONE);
            btnTap.setEnabled(true);
            tvPausedOverlay.setVisibility(View.GONE);
            btnTap.setEnabled(true);
            reportView.setVisibility(View.GONE);
            
            // Reset base time so pause duration isn't counted in next interval
            lastTapTime = System.currentTimeMillis(); 
        }
    }
    
    private void resetAnalysis() {
        currentCount = 0;
        zikrDurations.clear();
        isPaused = false;
        hasStarted = false;
        lastTapTime = 0;
        
        // Reset UI to Initial State
        layoutPauseOptions.setVisibility(View.GONE);
        reportView.setVisibility(View.GONE);
        tvPausedOverlay.setVisibility(View.GONE);
        fabPause.setVisibility(View.INVISIBLE);
        
        // Show Start Button again
        btnStartOverlay.setVisibility(View.VISIBLE);
        
        // Disable Tap
        btnTap.setEnabled(false);
        btnTap.setClickable(false);
        tvTapHint.setVisibility(View.GONE);
        
        // Reset progress
        progressBar.setProgress(0);
        updateUI();
    }
    
    private void generateReport() {
        if (zikrDurations.isEmpty()) {
            Toast.makeText(this, "No data yet", Toast.LENGTH_SHORT).show();
            return;
        }
        
        reportView.setVisibility(View.VISIBLE);
        reportView.setAlpha(0f);
        reportView.animate().alpha(1f).setDuration(500).start();
        
        // Stats
        long totalDur = 0;
        long maxDur = 0;
        long minDur = Long.MAX_VALUE;
        for (long d : zikrDurations) {
            totalDur += d;
            if (d > maxDur) maxDur = d;
            if (d < minDur) minDur = d;
        }
        double avgMillis = (double) totalDur / zikrDurations.size();
        double avgSec = avgMillis / 1000.0;
        
        tvReportSummary.setText(String.format(Locale.US, "Count: %d | Avg Speed: %.2f sec\nTotal Time: %.1f sec", 
                currentCount, avgSec, totalDur/1000.0));
        
        // Populate Table
        tableReport.removeAllViews();
        tableReport.setStretchAllColumns(true);
        
        // Table (with border) styling happens in code or xml. 
        // Applying border to the whole table:
        tableReport.setBackgroundResource(R.drawable.border_shape); // Need to create this if want specific borders
        // Or simply set background color.
        
        // Header Row
        android.widget.TableRow headerRow = new android.widget.TableRow(this);
        headerRow.setBackgroundColor(0xFF1565C0); // Darker Blue
        headerRow.setPadding(0, 0, 0, 0);
        
        String[] headers = {"Zikhr", "Time (s)", "Interval (s)"};
        for (String h : headers) {
            TextView tv = new TextView(this);
            tv.setText(h);
            tv.setTextColor(android.graphics.Color.WHITE);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setPadding(16, 24, 16, 24); // More padding
            headerRow.addView(tv);
        }
        tableReport.addView(headerRow);
        
        // Data Rows
        for (int i = 0; i < zikrDurations.size(); i++) {
            long dur = zikrDurations.get(i);
            double sec = dur / 1000.0;
            
            // Create layout params with margins to simulate borders
            android.widget.TableRow.LayoutParams params = new android.widget.TableRow.LayoutParams(
                android.widget.TableRow.LayoutParams.MATCH_PARENT,
                android.widget.TableRow.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(1, 1, 1, 1); // 1px spacing for border effect
            
            android.widget.TableRow row = new android.widget.TableRow(this);
            row.setPadding(0, 12, 0, 12);
            row.setBackgroundColor(0xFFCCCCCC); // Border color behind cells
            
            // Alternating Backgrounds
            int bgColor;
            if (i % 2 == 0) {
                bgColor = 0xFFF5F5F5; // Very Light Grey
            } else {
                bgColor = 0xFFE3F2FD; // Very Light Blue
            }
            // Apply bg to individual cells instead of row to show "grid" via margins if desired
            // Or just keep simple alternating rows. 
            // Re-interpreting "border lines... for the table".
            // Let's stick to row striping but add a bottom divider line for each row.
            
            // Revised approach: Set standard row background
            row.setBackgroundColor(bgColor);
            row.setPadding(0, 16, 0, 16);
            
            // Optional: Draw a thin line at bottom of each row? 
            // Better: Use a background drawable for the row that has a bottom stroke.
            // Or simpler: just keep alternating colors which is standard mobile UI.
            // User specifically asked for "border lines".
            
            // Let's add a ShapeDrawable with bottom border to the row.
            android.graphics.drawable.GradientDrawable border = new android.graphics.drawable.GradientDrawable();
            border.setColor(bgColor);
            border.setStroke(2, 0xFFB0BEC5); // Light Blue-Grey Border
            row.setBackground(border);
            
            // #
            TextView tvNum = new TextView(this);
            tvNum.setText(String.valueOf(i + 1));
            tvNum.setGravity(android.view.Gravity.CENTER);
            tvNum.setTextColor(0xFF000000);
            
            // Time
            TextView tvTime = new TextView(this);
            tvTime.setText(String.format(Locale.US, "%.2f", sec));
            tvTime.setGravity(android.view.Gravity.CENTER);
            // Highlight outliers
            if (dur > avgMillis * 1.5) tvTime.setTextColor(0xFFFF0000); // Slower
            else if (dur < avgMillis * 0.5) tvTime.setTextColor(0xFF008000); // Faster
            else tvTime.setTextColor(0xFF000000);
            
            // Interval
            long cumulative = 0;
            for(int j=0; j<=i; j++) cumulative += zikrDurations.get(j);
            double cumSec = cumulative / 1000.0;
            
            TextView tvCum = new TextView(this);
            tvCum.setText(String.format(Locale.US, "%.1f", cumSec));
            tvCum.setGravity(android.view.Gravity.CENTER);
            tvCum.setTextColor(0xFF555555);
            
            row.addView(tvNum);
            row.addView(tvTime);
            row.addView(tvCum);
            
            tableReport.addView(row);
        }
        
        // Graph
        drawGraph();
    }
    
    private void finishAnalysis() {
        fabPause.setVisibility(View.INVISIBLE); 
        btnTap.setEnabled(false);
        // set opacity to show disabled
        btnTap.setAlpha(0.6f);
        
        // "After reaching 100, circular counter disabled and reset button displayed"
        layoutPauseOptions.setVisibility(View.VISIBLE); 
        // We only want Reset button in this specific case? Or both?
        // "Only after clicking reset... counter goes to 0"
        
        // Generate report automatically? 
        // "give the whole report upto that count"
        generateReport();
        
        // Ensure Reset button is visible (it is inside layoutPauseOptions)
        // btnShowReport is also there.
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
        private Paint paintLine, paintDot, paintText, paintGrid;
        
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

            paintText = new Paint(); // Text Color
            paintText.setColor(0xFF000000);
            paintText.setTextSize(24f);
            paintText.setAntiAlias(true);

            paintGrid = new Paint();
            paintGrid.setColor(0xFFCCCCCC);
            paintGrid.setStrokeWidth(2f);
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (data == null || data.size() < 2) return;
            
            float width = getWidth();
            float height = getHeight();
            float paddingLeft = 60f;
            float paddingBottom = 40f;
            float paddingRight = 20f;
            float paddingTop = 20f;
            
            float maxVal = 0;
            for (long d : data) if (d > maxVal) maxVal = d;
            if (maxVal == 0) maxVal = 1;

            float graphWidth = width - paddingLeft - paddingRight;
            float graphHeight = height - paddingTop - paddingBottom;

            // Draw Axes
            canvas.drawLine(paddingLeft, paddingTop, paddingLeft, height - paddingBottom, paintGrid); // Y
            canvas.drawLine(paddingLeft, height - paddingBottom, width - paddingRight, height - paddingBottom, paintGrid); // X

            // Labels
            canvas.drawText("Count ->", width - 100f, height - 10f, paintText);
            canvas.save();
            canvas.rotate(-90, 20f, height/2);
            canvas.drawText("Time (ms)", 20f, height/2, paintText);
            canvas.restore();

            float xStep = graphWidth / (data.size() - 1);
            
            // Draw lines
            float prevX = paddingLeft;
            float prevY = height - paddingBottom - ((data.get(0) / maxVal) * graphHeight);
            
            for (int i = 1; i < data.size(); i++) {
                float x = paddingLeft + i * xStep;
                float y = height - paddingBottom - ((data.get(i) / maxVal) * graphHeight);
                
                canvas.drawLine(prevX, prevY, x, y, paintLine);
                canvas.drawCircle(prevX, prevY, 6f, paintDot);
                
                prevX = x;
                prevY = y;
            }
            canvas.drawCircle(prevX, prevY, 6f, paintDot);
        }
    }
}
