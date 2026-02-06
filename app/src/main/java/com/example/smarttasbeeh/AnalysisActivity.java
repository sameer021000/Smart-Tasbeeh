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
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.content.SharedPreferences;

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
    
    // New
    private androidx.recyclerview.widget.RecyclerView rvCards;
    private LinearLayout layoutDots;
    private AnalysisAdapter adapter;
    private List<AnalysisCard> analysisCards = new ArrayList<>();
    
    // State
    
    // State
    private int currentCount = 0;
    private final int TARGET = 100;
    private boolean isPaused = false;
    private boolean hasStarted = false;
    private long lastTapTime = 0;
    
    // Data: Stores DURATION (ms) of each Zikr (i.e., time between taps)
    // Start -> Tap 1 (Duration 1) -> Tap 2 (Duration 2)...
    private ArrayList<Long> zikrDurations = new ArrayList<>();
    
    // Feedback
    private Vibrator vibrator;
    private ToneGenerator toneGen;
    private static final String PREFS_NAME = "TasbeehPrefs";
    private static final String KEY_VIBRATION = "vibration";
    private static final String KEY_VIBRATION_STRENGTH = "vibration_strength";
    private static final String KEY_SOUND = "sound_enabled";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        initViews();
        setupListeners();
        setupBottomNav();
        
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        
        // reportContainer is the linear layout holding summary + graph + table
        this.reportView = findViewById(R.id.reportContainer);
        rvCards = findViewById(R.id.rvAnalysisCards);
        layoutDots = findViewById(R.id.layoutDots);
        
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
        btnStartOverlay.setOnClickListener(v -> {
            performFeedback();
            startAnalysisSession();
        });
        fabPause.setOnClickListener(v -> {
            performFeedback();
            togglePause();
        });
        btnReset.setOnClickListener(v -> {
            performFeedback();
            resetAnalysis();
        });
        btnShowReport.setOnClickListener(v -> {
            performFeedback();
            generateReport();
        });
        
        setupButtonAnimation(btnStartOverlay);
        setupButtonAnimation(fabPause);
        setupButtonAnimation(btnReset);
        setupButtonAnimation(btnShowReport);
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
        
        // --- Calculate Stats ---
        long totalDur = 0;
        long maxDur = 0;
        long minDur = Long.MAX_VALUE;
        int minIndex = -1;
        int maxIndex = -1;
        
        for (int i=0; i<zikrDurations.size(); i++) {
            long d = zikrDurations.get(i);
            totalDur += d;
            if (d > maxDur) { maxDur = d; maxIndex = i; }
            if (d < minDur) { minDur = d; minIndex = i; }
        }
        double avgMillis = (double) totalDur / zikrDurations.size();
        double avgSec = avgMillis / 1000.0;
        
        // Consistency: Max streak within 10% tolerance of average
        int maxStreak = 0;
        int currentStreak = 0;
        double tolerance = avgMillis * 0.15; // 15% tolerance
        for (long d : zikrDurations) {
            if (Math.abs(d - avgMillis) <= tolerance) {
                currentStreak++;
            } else {
                if (currentStreak > maxStreak) maxStreak = currentStreak;
                currentStreak = 0;
            }
        }
        if (currentStreak > maxStreak) maxStreak = currentStreak; // Check last run
        
        // --- Populate Cards ---
        analysisCards.clear();
        
        // Card 1: Summary
        analysisCards.add(new AnalysisCard(AnalysisCard.TYPE_STAT, "Session Complete", String.format(Locale.US, "%.2fs", avgSec), "Avg Speed | Total: "+(totalDur/1000)+"s", R.drawable.ic_analysis));
        
        // Card 2: Fastest
        analysisCards.add(new AnalysisCard(AnalysisCard.TYPE_STAT, "Fastest Count", String.format(Locale.US, "%.2fs", minDur/1000.0), "Zikr #" + (minIndex + 1), R.drawable.ic_speed)); // need speed icon, fallback analysis
        
        // Card 3: Slowest
        analysisCards.add(new AnalysisCard(AnalysisCard.TYPE_STAT, "Slowest Count", String.format(Locale.US, "%.2fs", maxDur/1000.0), "Zikr #" + (maxIndex + 1), R.drawable.ic_clock));
        
        // Card 4: Streak
        analysisCards.add(new AnalysisCard(AnalysisCard.TYPE_STAT, "Consistency Streak", String.valueOf(maxStreak), "Counts near avg speed", R.drawable.ic_target));
        
        // Card 5: Graph
        analysisCards.add(new AnalysisCard(AnalysisCard.TYPE_GRAPH, "Performance Graph", "", "", 0));
        
        // --- Setup Adapter ---
        if (adapter == null) {
            adapter = new AnalysisAdapter(analysisCards);
            rvCards.setAdapter(adapter);
            rvCards.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
            
            // Snap Helper
            androidx.recyclerview.widget.PagerSnapHelper snapHelper = new androidx.recyclerview.widget.PagerSnapHelper();
            snapHelper.attachToRecyclerView(rvCards);
            
            // Dots
            rvCards.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE) {
                        int pos = ((androidx.recyclerview.widget.LinearLayoutManager) recyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
                        if (pos != -1) updateDots(pos);
                    }
                }
            });
        } else {
            adapter.notifyDataSetChanged();
            rvCards.scrollToPosition(0);
        }
        
        setupDots(analysisCards.size());
    }
    
    private void setupDots(int count) {
        layoutDots.removeAllViews();
        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(this);
            dot.setImageDrawable(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_circle_button)); // Reuse circle shape
            // Or create a simple dot drawable. I'll stick to a simple code-generated shape to avoid resource issues
            // Actually reusing bg_circle works if small.
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(24, 24);
            params.setMargins(8, 0, 8, 0);
            layoutDots.addView(dot, params);
        }
        updateDots(0);
    }
    
    private void updateDots(int activePos) {
        for (int i = 0; i < layoutDots.getChildCount(); i++) {
             ImageView dot = (ImageView) layoutDots.getChildAt(i);
             // Active: Blue, Inactive: Grey
             if (i == activePos) {
                 dot.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.tasbeeh_blue_primary));
                 dot.setAlpha(1.0f);
             } else {
                 dot.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
                 dot.setAlpha(0.5f);
             }
        }
    }

    // --- Inner Classes for Adapter ---

    static class AnalysisCard {
        static final int TYPE_STAT = 0;
        static final int TYPE_GRAPH = 1;
        
        int type;
        String title, value, subtext;
        int iconRes;
        
        AnalysisCard(int type, String t, String v, String s, int i) {
            this.type = type; this.title = t; this.value = v; this.subtext = s; this.iconRes = i;
        }
    }
    
    class AnalysisAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<AnalysisAdapter.CardViewHolder> {
        
        private List<AnalysisCard> items;
        
        AnalysisAdapter(List<AnalysisCard> items) { this.items = items; }
        
        @androidx.annotation.NonNull
        @Override
        public CardViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_analysis_card, parent, false);
            return new CardViewHolder(v);
        }
        
        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull CardViewHolder holder, int position) {
            AnalysisCard item = items.get(position);
            
            if (item.type == AnalysisCard.TYPE_STAT) {
                holder.layoutStat.setVisibility(View.VISIBLE);
                holder.layoutGraph.setVisibility(View.GONE);
                
                holder.tvTitle.setText(item.title);
                holder.tvValue.setText(item.value);
                holder.tvSubtext.setText(item.subtext);
                holder.ivIcon.setImageResource(item.iconRes); // fallback if 0
                
            } else {
                holder.layoutStat.setVisibility(View.GONE);
                holder.layoutGraph.setVisibility(View.VISIBLE);
                
                // Add Graph view if not already there
                if (holder.graphFrame.getChildCount() == 0) {
                     GraphView graph = new GraphView(AnalysisActivity.this, zikrDurations);
                     holder.graphFrame.addView(graph);
                } else {
                    // Update graph data if needed (redrawing)
                    holder.graphFrame.removeAllViews();
                    GraphView graph = new GraphView(AnalysisActivity.this, zikrDurations);
                    holder.graphFrame.addView(graph);
                }
            }
        }
        
        @Override
        public int getItemCount() { return items.size(); }
        
        class CardViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            LinearLayout layoutStat;
            FrameLayout layoutGraph, graphFrame;
            TextView tvTitle, tvValue, tvSubtext;
            ImageView ivIcon;
            
            CardViewHolder(View itemView) {
                super(itemView);
                layoutStat = itemView.findViewById(R.id.layoutStatContent);
                layoutGraph = itemView.findViewById(R.id.layoutGraphContent);
                graphFrame = itemView.findViewById(R.id.graphFrame);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvValue = itemView.findViewById(R.id.tvValue);
                tvSubtext = itemView.findViewById(R.id.tvSubtext);
                ivIcon = itemView.findViewById(R.id.ivIcon);
            }
        }
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
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isVibEnabled = prefs.getBoolean(KEY_VIBRATION, true);
        boolean isSoundEnabled = prefs.getBoolean(KEY_SOUND, true);
        
        // Vibration
        if (isVibEnabled && vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int strength = prefs.getInt(KEY_VIBRATION_STRENGTH, 192);
                long duration = 40 + (long)(85 * (strength / 255.0f));
    
                if (!vibrator.hasAmplitudeControl()) {
                    strength = 255;
                }
                vibrator.vibrate(VibrationEffect.createOneShot(duration, strength));
            } else {
                vibrator.vibrate(50);
            }
        }
        
        // Sound
        if (isSoundEnabled && toneGen != null) {
             toneGen.startTone(ToneGenerator.TONE_CDMA_ANSWER, 150);
        }
    }

    private void setupButtonAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }
    
    private void drawGraph() {
        graphContainer.removeAllViews();
        GraphView graph = new GraphView(this, zikrDurations);
        graphContainer.addView(graph);
    }
    
    private void setupBottomNav() {
        // IDs might change later, but for now apply logic to existing IDs
        // Note: We will reorder XMLs later, but the IDs will remain or be updated.
        // Current: Counter, History, Settings. Analysis is current.
        
        android.view.View.OnClickListener navListener = v -> {
            // Restriction Logic:
            if (hasStarted && !isPaused && currentCount < TARGET) {
               Toast.makeText(AnalysisActivity.this, "Please pause the Zikhr analysis counter to move to another screen.", Toast.LENGTH_SHORT).show();
               return; 
            }
            
            int id = v.getId();
            if (id == R.id.btnNavCounter) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left); // Counter is to the right
                finish();
            } else if (id == R.id.btnNavHistory) { // History will be to the right of Counter
                startActivity(new Intent(this, SavedCountsActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            } else if (id == R.id.btnNavSettings) { // Settings is furthest right
                startActivity(new Intent(this, SettingsActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            }
        };

        findViewById(R.id.btnNavCounter).setOnClickListener(navListener);
        findViewById(R.id.btnNavHistory).setOnClickListener(navListener);
        findViewById(R.id.btnNavSettings).setOnClickListener(navListener);
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
