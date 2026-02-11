package com.example.smarttasbeeh;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "TasbeehPrefs";
    private static final String KEY_COUNT = "count";
    private static final String KEY_BUTTON_X = "button_x";
    private static final String KEY_BUTTON_Y = "button_y";
    private static final String KEY_VIBRATION = "vibration";
    private static final String KEY_VIBRATION_STRENGTH = "vibration_strength";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_RESUME_ID = "resume_id";
    private static final String KEY_RESUME_TITLE = "resume_title";
    private static final String KEY_TAP_ANYWHERE = "tap_anywhere";
    private static final String KEY_TARGET = "target_count";
    private static final String KEY_AUTO_SPEED = "auto_speed";
    private static final String KEY_AUTO_RUNNING = "is_auto_running";
    private static final String KEY_SOUND = "sound_enabled";

    private int currentCount = 0;
    private int targetCount = 0;
    private final int MAX_COUNT = 9999999;
    private boolean isVibrationEnabled = true;
    private boolean isTapAnywhereEnabled = false;
    private boolean isSoundEnabled = false;
    private ToneGenerator toneGen;

    private TextView tvCountDisplay;
    private CardView counterButton;
    private View gridBackground;
    private FrameLayout bigBoxContainer;
    private View rootLayout;

    // New Controls
    private View btnDecrease, btnAuto, btnTarget, btnReset, btnSave;
    private android.widget.ImageView ivAutoIcon;

    private Handler handler = new Handler();
    private boolean isDragMode = false;
    private float dX, dY;
    private float lastTouchX, lastTouchY;

    // Auto Count Logic
    private Handler autoHandler = new Handler();
    private Runnable autoRunnable;
    private boolean isAutoRunning = false;
    private boolean shouldIgnoreTarget = false; // New flag for Case A/B
    private long autoSpeed = 1000; // Default 1 sec

    private DbHelper dbHelper;
    private Vibrator vibrator;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load Theme Preference before setContentView
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DbHelper(this);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize Views
        tvCountDisplay = findViewById(R.id.tvCountDisplay);
        counterButton = findViewById(R.id.videoButton);
        gridBackground = findViewById(R.id.gridBackground);
        bigBoxContainer = findViewById(R.id.bigBoxContainer);
        btnReset = findViewById(R.id.btnReset); // Promoted
        btnSave = findViewById(R.id.btnSave);   // Promoted
        
        View btnSavedCounts = findViewById(R.id.btnSavedCounts);
        View btnSettings = findViewById(R.id.btnSettings);
        View btnAnalysis = findViewById(R.id.btnAnalysis);

        rootLayout = findViewById(R.id.main);

        btnDecrease = findViewById(R.id.btnDecrease);
        btnAuto = findViewById(R.id.btnAuto);
        btnTarget = findViewById(R.id.btnTarget);
        ivAutoIcon = findViewById(R.id.ivAutoIcon);

        // Restore State
        currentCount = prefs.getInt(KEY_COUNT, 0);
        targetCount = prefs.getInt(KEY_TARGET, 0);
        autoSpeed = prefs.getLong(KEY_AUTO_SPEED, 1000);

        isVibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true);
        isTapAnywhereEnabled = prefs.getBoolean(KEY_TAP_ANYWHERE, true);
        isSoundEnabled = prefs.getBoolean(KEY_SOUND, true);

        // Reset Auto Running state on app launch
        prefs.edit().putBoolean(KEY_AUTO_RUNNING, false).apply();

        updateCountDisplay();

        setupTapAnywhere();

        // ... (gridBackground code) ...
        // I will skipping replacing gridBackground code as I can context match around it if needed or just replace broader block.
        // Actually, to correctly promote variables I need to replace onCreate start.

        // ...


        // Create and set grid background programmatically
        gridBackground.post(() -> {
            int size = 150; // Grid cell size in pixels
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

            android.graphics.Paint paint = new android.graphics.Paint();
            // Get color from resources
            int gridColor = androidx.core.content.ContextCompat.getColor(this, R.color.grid_line_color);
            paint.setColor(gridColor);
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeWidth(2);

            canvas.drawRect(0, 0, size, size, paint);

            android.graphics.drawable.BitmapDrawable bitmapDrawable = new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
            bitmapDrawable.setTileModeXY(android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT);
            gridBackground.setBackground(bitmapDrawable);
        });

        // Restore Button Position (delayed to ensure layout is ready)
        bigBoxContainer.post(() -> {
            float savedX = prefs.getFloat(KEY_BUTTON_X, -1);
            float savedY = prefs.getFloat(KEY_BUTTON_Y, -1);

            if (savedX != -1 && savedY != -1) {
                // simple bounds check
                int parentWidth = bigBoxContainer.getWidth();
                int parentHeight = bigBoxContainer.getHeight();
                int btnWidth = counterButton.getWidth();
                int btnHeight = counterButton.getHeight();

                if (savedX + btnWidth <= parentWidth && savedY + btnHeight <= parentHeight) {
                    counterButton.setX(savedX);
                    counterButton.setY(savedY);
                }
            } else {
                // Center default
                counterButton.setX((bigBoxContainer.getWidth() - counterButton.getWidth()) / 2f);
                counterButton.setY((bigBoxContainer.getHeight() - counterButton.getHeight()) / 2f);
            }
        });

        // Counter Button Logic
        Runnable longPressRunnable = new Runnable() {
            @Override
            public void run() {
                isDragMode = true;
                gridBackground.setVisibility(View.VISIBLE);
                vibrate(100); // Feedback for drag mode

                // Highlight button logic (optional visual cue)
                counterButton.setAlpha(0.8f);

                // Change border color to grid line color
                try {
                    android.graphics.drawable.GradientDrawable bg = (android.graphics.drawable.GradientDrawable) bigBoxContainer.getBackground();
                    int gridColor = androidx.core.content.ContextCompat.getColor(MainActivity.this, R.color.grid_line_color);
                    float density = getResources().getDisplayMetrics().density;
                    int strokeWidth = (int)(2 * density);
                    bg.setStroke(strokeWidth, gridColor);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        counterButton.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = event.getRawX();
                    lastTouchY = event.getRawY();
                    dX = view.getX() - event.getRawX();
                    dY = view.getY() - event.getRawY();

                    // Click Animation: Scale Down
                    view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start();

                    isDragMode = false;
                    handler.postDelayed(longPressRunnable, 3000); // 3 seconds hold
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (isDragMode) {
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        // Bounds Check
                        newX = Math.max(0, Math.min(newX, bigBoxContainer.getWidth() - view.getWidth()));
                        newY = Math.max(0, Math.min(newY, bigBoxContainer.getHeight() - view.getHeight()));

                        view.animate()
                                .x(newX)
                                .y(newY)
                                .setDuration(0)
                                .start();
                    } else {
                        // If moved significantly before long press triggers, cancel long press
                        if (Math.hypot(event.getRawX() - lastTouchX, event.getRawY() - lastTouchY) > 50) {
                            handler.removeCallbacks(longPressRunnable);
                        }
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    // Click Animation: Scale Up
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();

                    handler.removeCallbacks(longPressRunnable);
                    if (isDragMode) {
                        isDragMode = false;
                        gridBackground.setVisibility(View.INVISIBLE);
                        counterButton.setAlpha(1.0f);
                        saveButtonPosition(view.getX(), view.getY());

                        // Revert border color
                        try {
                            android.graphics.drawable.GradientDrawable bg = (android.graphics.drawable.GradientDrawable) bigBoxContainer.getBackground();
                            int normalColor = androidx.core.content.ContextCompat.getColor(MainActivity.this, R.color.card_stroke_color);
                            float density = getResources().getDisplayMetrics().density;
                            int strokeWidth = (int)(2 * density);
                            bg.setStroke(strokeWidth, normalColor);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        // Click action
                        if (Math.hypot(event.getRawX() - lastTouchX, event.getRawY() - lastTouchY) < 50) {
                            // If auto count is running, tap stops it (or pauses ideally), but also counts?
                            // User requirement: "Tap anywhere option when the auto count is running."
                            // Strategy: Interaction stops Auto if running, otherwise Increments.
                            // Wait, if I'm holding a physical counter and I tap, I expect it to count.
                            // But if auto is running, maybe I want to intervene.
                            // Let's say: Tap -> Stop Auto. Do NOT increment (to avoid double count if they just wanted to stop).
                            if (isAutoRunning) {
                                stopAutoCount();
                            } else {
                                incrementCount();
                            }
                        }
                    }
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    // Click Animation: Scale Up
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();

                    handler.removeCallbacks(longPressRunnable);
                    isDragMode = false;
                    gridBackground.setVisibility(View.INVISIBLE);
                    counterButton.setAlpha(1.0f);

                    // Revert border color
                    try {
                        android.graphics.drawable.GradientDrawable bg = (android.graphics.drawable.GradientDrawable) bigBoxContainer.getBackground();
                        int normalColor = androidx.core.content.ContextCompat.getColor(MainActivity.this, R.color.card_stroke_color);
                        float density = getResources().getDisplayMetrics().density;
                        int strokeWidth = (int)(2 * density);
                        bg.setStroke(strokeWidth, normalColor);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
            }
            return false;
        });

        // Other Interactions
        btnReset.setOnClickListener(v -> {
            performButtonFeedback();
            showResetDialog();
        });
        btnSave.setOnClickListener(v -> {
            performButtonFeedback();
            showSaveDialog();
        });
        btnSavedCounts.setOnClickListener(v -> {
            if (isAutoRunning) {
                Toast.makeText(MainActivity.this, "Please pause auto-count", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(MainActivity.this, SavedCountsActivity.class));
            overridePendingTransition(0, 0);
        });
        btnSettings.setOnClickListener(v -> {
            if (isAutoRunning) {
                Toast.makeText(MainActivity.this, "Please pause auto-count", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            overridePendingTransition(0, 0);
        });
        btnAnalysis.setOnClickListener(v -> {
            if (isAutoRunning) {
                Toast.makeText(MainActivity.this, "Please pause auto-count", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(MainActivity.this, AnalysisActivity.class));
            overridePendingTransition(0, 0);
        });

        // New Feature Listeners
        btnDecrease.setOnClickListener(v -> {
            performButtonFeedback();
            decreaseCount();
        });
        btnTarget.setOnClickListener(v -> {
            performButtonFeedback();
            showTargetDialog();
        });
        btnAuto.setOnClickListener(v -> {
            performButtonFeedback();
            if (isAutoRunning) {
                stopAutoCount();
            } else {
                if (isTapAnywhereEnabled) {
                    Toast.makeText(this, "Please disable Tap Anywhere in Settings to use Auto-Count", Toast.LENGTH_SHORT).show();
                    return;
                }
                showAutoConfigDialog();
            }
        });

        setupButtonAnimation(btnDecrease);
        setupButtonAnimation(btnTarget);
        setupButtonAnimation(btnAuto);
        setupButtonAnimation(btnSave);
        setupButtonAnimation(btnReset);

        // Auto Runnable
        autoRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAutoRunning) return;

                incrementCount();

                // Check locally after increment if we should continue
                // Note: incrementCount handles target check and will open dialog if reached.
                // If reached, we should stop.
                // Fix for Case A: Only stop if we are NOT ignoring target.
                if (!shouldIgnoreTarget && targetCount > 0 && currentCount >= targetCount) {
                    stopAutoCount();
                    showTargetReachedDialog();
                    return; // Stop loop
                }

                autoHandler.postDelayed(this, autoSpeed);
            }
        };
    }

    private void setupButtonAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            return false; // propagate to OnClickListener
        });
    }

    private void decreaseCount() {
        if (isAutoRunning) return; // Not allowed

        if (currentCount > 0) {
            currentCount--;
            updateCountDisplay();
            // vibration handled in performButtonFeedback
            saveCountPref();
        }
    }

    private void performButtonFeedback() {
        vibrate(90); // Match incrementCount vibration
        if (isSoundEnabled && toneGen != null) {
            toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, 50);
        }
    }

    private void showTargetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_set_target, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etTarget = view.findViewById(R.id.etTarget);
        if (targetCount > 0) {
            etTarget.setText(String.valueOf(targetCount));
        }

        view.findViewById(R.id.btnStartTarget).setOnClickListener(v -> {
            String val = etTarget.getText().toString();
            if (val.isEmpty()) return;

            int newTarget = Integer.parseInt(val);
            if (newTarget <= 0) {
                etTarget.setError("Invalid Target");
                return;
            }

            // Logic: If already some count is there (not saved)...
            if (currentCount > 0) {
                if (newTarget > currentCount) {
                    // Direct update without dialog
                    targetCount = newTarget;
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putInt(KEY_TARGET, targetCount).apply();
                    dialog.dismiss();
                    Toast.makeText(this, "Target Set: " + targetCount, Toast.LENGTH_SHORT).show();
                } else {
                    // <= Current Count -> Show specific warning using the custom dialog
                    dialog.dismiss();
                    showTargetLessThanCurrentDialog(newTarget);
                }
            } else {
                targetCount = newTarget;
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putInt(KEY_TARGET, targetCount).apply();
                dialog.dismiss();
                Toast.makeText(this, "Target Set: " + targetCount, Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }


    private void showSaveBeforeResetDialog(int newTarget) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_save_before_reset, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        view.findViewById(R.id.btnSave).setOnClickListener(v -> {
            dialog.dismiss();
            showSaveDialog();
            Toast.makeText(this, "Please save, then set target.", Toast.LENGTH_LONG).show();
        });

        view.findViewById(R.id.btnDontSave).setOnClickListener(v -> {
            dialog.dismiss();
            currentCount = 0;
            updateCountDisplay();
            saveCountPref();
            targetCount = newTarget;
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putInt(KEY_TARGET, targetCount).apply();
            Toast.makeText(this, "Count Reset. Target Set: " + targetCount, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showTargetLessThanCurrentDialog(int newTarget) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_target_less_warning, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        view.findViewById(R.id.btnStartFromZero).setOnClickListener(v -> {
            dialog.dismiss();
            showSaveBeforeResetDialog(newTarget);
        });

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showAutoConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_auto_config, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvSpeedValue = view.findViewById(R.id.tvSpeedValue);
        com.google.android.material.slider.Slider sliderSpeed = view.findViewById(R.id.sliderSpeed);
        TextView tvMoreTime = view.findViewById(R.id.tvMoreTime);

        // Convert internal ms to seconds for display
        float currentSec = autoSpeed / 1000f;

        // Determine initial mode
        final boolean[] isExtendedMode = {currentSec > 5.0f};

        // Configure Slider based on mode
        if (isExtendedMode[0]) {
            sliderSpeed.setValueFrom(5.0f);
            sliderSpeed.setValueTo(60.0f);
            sliderSpeed.setStepSize(1.0f); // 1 sec steps for extended range
            tvMoreTime.setText("Less time ?");
            
            // Allow up to 60.0
            if (currentSec > 60.0f) currentSec = 60.0f;
            if (currentSec < 5.0f) currentSec = 5.0f; 
        } else {
            sliderSpeed.setValueFrom(0.1f);
            sliderSpeed.setValueTo(5.0f);
            sliderSpeed.setStepSize(0.1f);
            tvMoreTime.setText("More time ?");
            
            if (currentSec < 0.1f) currentSec = 0.1f;
            if (currentSec > 5.0f) currentSec = 5.0f;
        }

        sliderSpeed.setValue(currentSec);
        tvSpeedValue.setText(String.format(Locale.US, "%.1f sec", currentSec));

        // Toggle Logic
        tvMoreTime.setOnClickListener(v -> {
            if (isExtendedMode[0]) {
                // Switch to Less Time (Normal Mode)
                isExtendedMode[0] = false;
                sliderSpeed.setValueFrom(0.1f);
                sliderSpeed.setValueTo(5.0f);
                sliderSpeed.setStepSize(0.1f);
                sliderSpeed.setValue(5.0f); // Reset to max of normal
                tvMoreTime.setText("More time ?");
            } else {
                // Switch to More Time (Extended Mode)
                isExtendedMode[0] = true;
                sliderSpeed.setValueFrom(5.0f);
                sliderSpeed.setValueTo(60.0f);
                sliderSpeed.setStepSize(1.0f);
                sliderSpeed.setValue(5.0f); // Reset to min of extended
                tvMoreTime.setText("Less time ?");
            }
            // Update Text
             tvSpeedValue.setText(String.format(Locale.US, "%.1f sec", sliderSpeed.getValue()));
        });

        sliderSpeed.addOnChangeListener((slider, value, fromUser) -> {
            tvSpeedValue.setText(String.format(Locale.US, "%.1f sec", value));
        });

        view.findViewById(R.id.btnStartAuto).setOnClickListener(v -> {
            float val = sliderSpeed.getValue();
            autoSpeed = (long)(val * 1000);
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putLong(KEY_AUTO_SPEED, autoSpeed).apply();

            startAutoCount();
            dialog.dismiss();
        });

        view.findViewById(R.id.btnCancelAuto).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void startAutoCount() {
        if (isAutoRunning) return;

        isAutoRunning = true;

        // Case A/B Logic:
        // If Target < Current (already passed), we ignore target until we pause/reset.
        // If Target > Current, we respect it.
        shouldIgnoreTarget = (targetCount > 0 && targetCount <= currentCount);

        ivAutoIcon.setImageResource(R.drawable.ic_pause); // Change to Pause

        // Disable Interactions
        setInteractionsEnabled(false);

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(KEY_AUTO_RUNNING, true).apply();

        autoHandler.postDelayed(autoRunnable, autoSpeed);
    }

    private void stopAutoCount() {
        if (!isAutoRunning) return;

        isAutoRunning = false;
        ivAutoIcon.setImageResource(R.drawable.ic_auto); // Change back to Auto

        // Enable Interactions
        setInteractionsEnabled(true);

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(KEY_AUTO_RUNNING, false).apply();

        autoHandler.removeCallbacks(autoRunnable);
    }

    private void setInteractionsEnabled(boolean enabled) {
        float alpha = enabled ? 1.0f : 0.5f;

        btnDecrease.setEnabled(enabled);
        btnDecrease.setAlpha(alpha);

        btnTarget.setEnabled(enabled);
        btnTarget.setAlpha(alpha);

        btnSave.setEnabled(enabled);
        btnSave.setAlpha(alpha);

        btnReset.setEnabled(enabled);
        btnReset.setAlpha(alpha);

        // Sidebar Navigation Buttons - kept enabled but with Toast check
        // Logic handled in OnClickListener

        // Main Button & Tap Anywhere
        counterButton.setEnabled(enabled); // Disables touch listener

        if (isTapAnywhereEnabled) {
            if (enabled) {
                rootLayout.setOnClickListener(v -> incrementCount());
                bigBoxContainer.setOnClickListener(v -> incrementCount());
            } else {
                rootLayout.setOnClickListener(null);
                bigBoxContainer.setOnClickListener(null);
            }
        }
    }

    private void incrementCount() {
        if (currentCount < MAX_COUNT) {
            currentCount++;
            updateCountDisplay();
            vibrate(90); // Strong vibration for click
            vibrate(90); // Strong vibration for click
            if (isSoundEnabled && toneGen != null) {
                // TONE_PROP_NACK produces a dull click/thud sound
                toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 50);
            }
            saveCountPref();

            // Target Check
            // Controlled by shouldIgnoreTarget for Auto Loop, but what about manual taps?
            // "When Auto-count is running... Taps disabled". So manual taps impossible during auto.
            // So we only check target here if MANUALLY tapping (which only happens if !isAutoRunning)
            // OR if called by autoRunnable.
            // BUT wait, autoRunnable calls startAutoCount -> calls incrementCount.
            // We should strip target logic from here or handle it.
            // If I move target check to autoRunnable, manual taps won't check target?
            // Target check should be consistent.

            // Let's rely on the check inside autoRunnable for AUTO.
            // For Manual, we always respecting target?
            // "Counter == Target --> Show popup" implies general rule.

            // However, the "Ignore Target" rule is specific to Auto Count "Continue" scenario.
            // If I manually tap past target, should it stop me? Usually yes.
            // But if I am in Auto Mode, I might be "ignoring target".

            // Refined Check:
            if (targetCount > 0 && currentCount == targetCount) {
                if (isAutoRunning) {
                    if (!shouldIgnoreTarget) {
                        stopAutoCount();
                        showTargetReachedDialog();
                    }
                    // Else: Continue (Case A)
                } else {
                    // Manual Tap: Always show?
                    // If I am manually tapping, I likely want that feedback.
                    showTargetReachedDialog();
                }
            }
        }
    }

    private void showTargetReachedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_target_reached, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvMsg = view.findViewById(R.id.tvTargetMessage);
        tvMsg.setText("Target of " + targetCount + " reached.");

        view.findViewById(R.id.btnContinue).setOnClickListener(v -> {
            // "Show ... whether to continue"
            dialog.dismiss();
            // Continue means we ignore target? or what?
            // "Counter == Target --> Show popup... whether to continue or increase target."
            // If continue, we just close dialog. Next count 101 != 100. So it won't trigger again.
        });

        view.findViewById(R.id.btnIncreaseTarget).setOnClickListener(v -> {
            dialog.dismiss();
            showTargetDialog(); // Set new target
        });

        dialog.show();
        vibrate(500); // Long vibrate for goal
    }

    private void updateCountDisplay() {
        tvCountDisplay.setText(String.valueOf(currentCount));
    }

    private void saveCountPref() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putInt(KEY_COUNT, currentCount).apply();
    }

    private void saveButtonPosition(float x, float y) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putFloat(KEY_BUTTON_X, x)
                .putFloat(KEY_BUTTON_Y, y)
                .apply();
    }

    private void showResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_reset_confirm, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        // Transparent background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        view.findViewById(R.id.btnResetConfirm).setOnClickListener(v -> {
            stopAutoCount(); // Ensure auto stops
            currentCount = 0;
            updateCountDisplay();
            saveCountPref();
            clearResumeState();
            dialog.dismiss();
        });

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_save_count, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etTitle = view.findViewById(R.id.etTitle);
        Button btnSaveDialog = view.findViewById(R.id.btnSave);
        TextView tvInstructions = view.findViewById(R.id.tvInstructions);
        TextView tvExistingTitle = view.findViewById(R.id.tvExistingTitle);

        // Check for Resume State
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int resumeId = prefs.getInt(KEY_RESUME_ID, -1);
        String resumeTitle = prefs.getString(KEY_RESUME_TITLE, null);

        if (resumeId != -1 && resumeTitle != null) {
            // Update Mode
            tvInstructions.setVisibility(View.GONE);
            etTitle.setVisibility(View.GONE);
            tvExistingTitle.setVisibility(View.VISIBLE);
            tvExistingTitle.setText(resumeTitle);

            btnSaveDialog.setText("Update");

            // Enable button by default since title is present
            btnSaveDialog.setAlpha(1.0f);
            btnSaveDialog.setEnabled(true);
            btnSaveDialog.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2962FF));

            btnSaveDialog.setOnClickListener(v -> {
                String timestamp = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date());
                dbHelper.updateCount(resumeId, currentCount, timestamp);

                Toast.makeText(this, "Count Updated!", Toast.LENGTH_SHORT).show();

                // Reset after update
                currentCount = 0;
                updateCountDisplay();
                saveCountPref();
                clearResumeState();
                stopAutoCount(); // Ensure stopped

                dialog.dismiss();
            });

        } else {
            // Save Mode (Existing Logic)
            // Initial state
            btnSaveDialog.setAlpha(0.5f);
            btnSaveDialog.setEnabled(false);

            etTitle.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.toString().trim().length() > 0) {
                        btnSaveDialog.setAlpha(1.0f);
                        btnSaveDialog.setEnabled(true);
                        btnSaveDialog.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2962FF));
                    } else {
                        btnSaveDialog.setAlpha(0.5f);
                        btnSaveDialog.setEnabled(false);
                        btnSaveDialog.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2962FF));
                    }
                }
                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });

            btnSaveDialog.setOnClickListener(v -> {
                // Trim and replace multiple spaces with single space
                String title = etTitle.getText().toString().trim().replaceAll("\\s+", " ");

                if (title.isEmpty()) return;

                if (dbHelper.isTitleExists(title)) {
                    etTitle.setError("Name already exists");
                    return;
                }

                String timestamp = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date());
                dbHelper.saveCount(title, currentCount, timestamp);

                Toast.makeText(this, "Count Saved!", Toast.LENGTH_SHORT).show();

                // Reset after save
                currentCount = 0;
                updateCountDisplay();
                saveCountPref();
                stopAutoCount(); // Ensure stopped

                dialog.dismiss();
            });
        }

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void clearResumeState() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .remove(KEY_RESUME_ID)
                .remove(KEY_RESUME_TITLE)
                .apply();
    }



    private void vibrate(long ms) {
        if (isVibrationEnabled && vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int strength = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(KEY_VIBRATION_STRENGTH, 192);
                // Scale duration: 40ms to 125ms based on strength
                long duration = 40 + (long)(85 * (strength / 255.0f));

                if (!vibrator.hasAmplitudeControl()) {
                    strength = 255; // Use max if control not supported
                }
                vibrator.vibrate(VibrationEffect.createOneShot(duration, strength));
            } else {
                vibrator.vibrate(ms);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if we need to resume a count from SavedCountsActivity
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        // If external activity modified it, refresh
        currentCount = prefs.getInt(KEY_COUNT, currentCount);
        // Refresh settings that might have changed
        isVibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true);
        isTapAnywhereEnabled = prefs.getBoolean(KEY_TAP_ANYWHERE, true);
        isSoundEnabled = prefs.getBoolean(KEY_SOUND, true);

        targetCount = prefs.getInt(KEY_TARGET, 0); // Reload target
        autoSpeed = prefs.getLong(KEY_AUTO_SPEED, 1000);

        updateCountDisplay();
        setupTapAnywhere();
    }

    private void setupTapAnywhere() {
        if (isTapAnywhereEnabled) {
            // Note: If auto running, tap -> stop. Else increment.
            rootLayout.setOnClickListener(v -> {
                if (isAutoRunning) stopAutoCount();
                else incrementCount();
            });
            bigBoxContainer.setOnClickListener(v -> {
                if (isAutoRunning) stopAutoCount();
                else incrementCount();
            });
        } else {
            rootLayout.setOnClickListener(null);
            rootLayout.setClickable(false);
            bigBoxContainer.setOnClickListener(null);
            bigBoxContainer.setClickable(false);
        }
    }

}