package com.example.smarttasbeeh;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "TasbeehPrefs";
    private static final String KEY_VIBRATION = "vibration";
    private static final String KEY_VIBRATION_STRENGTH = "vibration_strength";
    private static final String KEY_TAP_ANYWHERE = "tap_anywhere";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_AUTO_RUNNING = "is_auto_running";
    private static final String KEY_SOUND = "sound_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before view creation
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        SwitchMaterial switchDarkMode = findViewById(R.id.switchDarkMode);
        SwitchMaterial switchHaptic = findViewById(R.id.switchHaptic);
        SwitchMaterial switchSound = findViewById(R.id.switchSound);
        SwitchMaterial switchTapAnywhere = findViewById(R.id.switchTapAnywhere);
        SeekBar seekBarVibration = findViewById(R.id.seekBarVibration);
        View layoutVibrationLevel = findViewById(R.id.layoutVibrationLevel);
        TextView tvSeekValue = findViewById(R.id.tvSeekValue);
        View btnNavCounter = findViewById(R.id.btnNavCounter);
        View btnSavedCounts = findViewById(R.id.btnSavedCounts);

        // Set initial state
        switchDarkMode.setChecked(isDarkMode);
        boolean isVibEnabled = prefs.getBoolean(KEY_VIBRATION, true);
        switchHaptic.setChecked(isVibEnabled);
        boolean isSoundEnabled = prefs.getBoolean(KEY_SOUND, true);
        switchSound.setChecked(isSoundEnabled);
        boolean isTapAnywhere = prefs.getBoolean(KEY_TAP_ANYWHERE, true);
        switchTapAnywhere.setChecked(isTapAnywhere);

        // Setup SeekBar
        // Setup SeekBar
        // Default 75% -> 191
        int savedStrength = prefs.getInt(KEY_VIBRATION_STRENGTH, 192);
        // Map 1-255 to 0-100
        int progress = (int) ((savedStrength - 1) * 100 / 254.0f);
        seekBarVibration.setProgress(progress);
        tvSeekValue.setText(progress + "%");

        // Post to wait for layout to determine width for positioning
        seekBarVibration.post(() -> updateLabelPosition(seekBarVibration, tvSeekValue, progress));

        // Initial visibility
        layoutVibrationLevel.setAlpha(isVibEnabled ? 1.0f : 0.5f);
        seekBarVibration.setEnabled(isVibEnabled);

        // Logic
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putBoolean(KEY_DARK_MODE, isChecked);
            editor.apply();

            // Revert checks to avoid loop if listener triggers during recreate (though standard practice)
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        switchHaptic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putBoolean(KEY_VIBRATION, isChecked);
            editor.apply();

            layoutVibrationLevel.setAlpha(isChecked ? 1.0f : 0.5f);
            seekBarVibration.setEnabled(isChecked);

            if (isChecked) Toast.makeText(this, "Vibration On", Toast.LENGTH_SHORT).show();
        });

        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putBoolean(KEY_SOUND, isChecked);
            editor.apply();
            if (isChecked) Toast.makeText(this, "Sound On", Toast.LENGTH_SHORT).show();
        });

        switchTapAnywhere.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                SharedPreferences checkPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                boolean isAutoRunning = checkPrefs.getBoolean(KEY_AUTO_RUNNING, false);
                if (isAutoRunning) {
                    Toast.makeText(this, "Auto-Count is currently running", Toast.LENGTH_SHORT).show();
                    buttonView.setChecked(false);
                    return;
                }
            }

            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putBoolean(KEY_TAP_ANYWHERE, isChecked);
            editor.apply();
            if (isChecked) Toast.makeText(this, "Tap Anywhere Enabled", Toast.LENGTH_SHORT).show();
        });

        seekBarVibration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // Map 0-100 to 1-255
                    int strength = 1 + (int)(progress * 254 / 100.0f);
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                            .putInt(KEY_VIBRATION_STRENGTH, strength)
                            .apply();

                    tvSeekValue.setText(progress + "%");
                    updateLabelPosition(seekBar, tvSeekValue, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Vibrate to demonstrate
                int strength = 1 + (int)(seekBar.getProgress() * 254 / 100.0f);
                // Scale duration: 40ms to 125ms
                // 75% gives approx 104ms
                long duration = 40 + (long)(85 * (strength / 255.0f));
                vibrate(duration, strength);
            }
        });

        // Navigation
        btnNavCounter.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        btnSavedCounts.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, SavedCountsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        findViewById(R.id.btnNavAnalysis).setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, AnalysisActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }


    private void updateLabelPosition(SeekBar seekBar, TextView textView, int progress) {
        // Measure text view to get current width after text change
        textView.measure(0, 0);
        int textWidth = textView.getMeasuredWidth();

        float width = seekBar.getWidth() - seekBar.getPaddingLeft() - seekBar.getPaddingRight();
        float thumbPos = (progress / 100.0f) * width; // 0 to width

        // Adjust for padding and centering text
        float x = seekBar.getPaddingLeft() + thumbPos - (textWidth / 2.0f);

        // Clamp to prevent clipping
        float maxAvailableX = seekBar.getWidth() - textWidth;
        if (x < 0) x = 0;
        if (x > maxAvailableX) x = maxAvailableX;

        // Since both are in a RelativeLayout and SeekBar has padding, 
        // we might simply set X relative to parent.
        textView.setX(seekBar.getX() + x);
    }

    private void vibrate(long ms, int amplitude) {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Determine if amplitude control is supported
                if (!vibrator.hasAmplitudeControl()) {
                    // Fallback to duration only, but we are already passing scaled duration
                    // Use MAX amplitude just to be sure it triggers
                    amplitude = 255;
                }
                vibrator.vibrate(VibrationEffect.createOneShot(ms, amplitude));
            } else {
                vibrator.vibrate(ms);
            }
        }
    }
}