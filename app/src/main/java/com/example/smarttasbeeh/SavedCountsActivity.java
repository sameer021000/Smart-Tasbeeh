package com.example.smarttasbeeh;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import android.widget.ImageView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SavedCountsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private View emptyView;
    private DbHelper dbHelper;
    private ImageView ivSort;

    // Sorting Constants
    private static final int SORT_LATEST = 0;
    private static final int SORT_EARLIEST = 1;
    private static final int SORT_HIGHEST = 2;
    private static final int SORT_LOWEST = 3;
    private static final int SORT_A_Z = 4;
    private static final int SORT_Z_A = 5;

    private int currentSort = SORT_LATEST;
    private static final String PREFS_NAME = "TasbeehPrefs";
    private static final String KEY_SORT_ORDER = "sort_order";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_counts);
        
        // Hide status bar in landscape
        if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        // Apply theme 
        // Logic handled by device usually, but ensuring consistent prefs read if we had complex theming

        dbHelper = new DbHelper(this);

        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);
        // Load saved sort order
        currentSort = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(KEY_SORT_ORDER, SORT_LATEST);

        ivSort = findViewById(R.id.btnSort); // Correct ID from layout change
        ivSort.setOnClickListener(this::showSortMenu);

        // Top Bar Logic (Custom Header)
        // Ensure to reference new nav buttons if the layout changed
        findViewById(R.id.btnNavCounter).setOnClickListener(v -> {
            Intent intent = new Intent(SavedCountsActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        findViewById(R.id.btnNavSettings).setOnClickListener(v -> {
            startActivity(new Intent(SavedCountsActivity.this, SettingsActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        findViewById(R.id.btnNavAnalysis).setOnClickListener(v -> {
            startActivity(new Intent(SavedCountsActivity.this, AnalysisActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        loadCounts();
    }

    private void loadCounts() {
        List<SavedCount> counts = dbHelper.getAllCounts();

        // Sort the list based on currentSort
        sortList(counts);

        // Update Badge
        TextView badge = findViewById(R.id.recordCountBadge);
        if (badge != null) {
            badge.setText(counts.size() + " records");
        }

        if (counts.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);

            SavedCountsAdapter adapter = new SavedCountsAdapter(counts, new SavedCountsAdapter.OnItemClickListener() {
                @Override
                public void onContinueClick(SavedCount item) {
                    getSharedPreferences("TasbeehPrefs", MODE_PRIVATE)
                            .edit()
                            .putInt("count", item.getCount())
                            .putInt("resume_id", item.getId())
                            .putString("resume_title", item.getTitle())
                            .apply();
                    // Go to MainActivity
                    Intent intent = new Intent(SavedCountsActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                }

                @Override
                public void onDeleteClick(SavedCount item) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SavedCountsActivity.this);
                    View view = getLayoutInflater().inflate(R.layout.dialog_delete_confirm, null);
                    builder.setView(view);
                    AlertDialog dialog = builder.create();

                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    }

                    view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
                    view.findViewById(R.id.btnDeleteConfirm).setOnClickListener(v -> {
                        dbHelper.deleteCount(item.getId());
                        loadCounts(); // Refresh list
                        dialog.dismiss();
                    });

                    dialog.show();
                }

                @Override
                public void onItemLongClick(SavedCount item) {
                    showPinDialog(item);
                }
            });
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        }
    }
    private void showSortMenu(View view) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_sort, null);
        bottomSheetDialog.setContentView(sheetView);

        // Initial Highlight Logic
        updateSortHighlight(sheetView, currentSort);

        // Click Listeners
        sheetView.findViewById(R.id.optionLatest).setOnClickListener(v -> updateSort(SORT_LATEST, bottomSheetDialog));
        sheetView.findViewById(R.id.optionEarliest).setOnClickListener(v -> updateSort(SORT_EARLIEST, bottomSheetDialog));
        sheetView.findViewById(R.id.optionHighest).setOnClickListener(v -> updateSort(SORT_HIGHEST, bottomSheetDialog));
        sheetView.findViewById(R.id.optionLowest).setOnClickListener(v -> updateSort(SORT_LOWEST, bottomSheetDialog));
        sheetView.findViewById(R.id.optionAZ).setOnClickListener(v -> updateSort(SORT_A_Z, bottomSheetDialog));
        sheetView.findViewById(R.id.optionZA).setOnClickListener(v -> updateSort(SORT_Z_A, bottomSheetDialog));

        bottomSheetDialog.show();
    }

    private void updateSortHighlight(View sheetView, int sortType) {
        // Reset all backgrounds first (or simpler: assume default is selectable item background)
        // Here we will set a specific background for the selected one, and default for others.
        // For simplicity, let's just use semi-transparent blue for selected.
        
        int[] ids = {R.id.optionLatest, R.id.optionEarliest, R.id.optionHighest, R.id.optionLowest, R.id.optionAZ, R.id.optionZA};
        int[] types = {SORT_LATEST, SORT_EARLIEST, SORT_HIGHEST, SORT_LOWEST, SORT_A_Z, SORT_Z_A};

        for (int i = 0; i < ids.length; i++) {
            View option = sheetView.findViewById(ids[i]);
            if (types[i] == sortType) {
                // Selected: Highlight with stroke/background
                option.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_sort_selected)); 
            } else {
                // Default: Selectable Item Background
                android.util.TypedValue outValue = new android.util.TypedValue();
                getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                option.setBackgroundResource(outValue.resourceId);
            }
        }
    }

    private void updateSort(int sortType, BottomSheetDialog dialog) {
        currentSort = sortType;
        // Save preference
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(KEY_SORT_ORDER, currentSort)
                .apply();

        loadCounts();
        dialog.dismiss();
    }

    private void sortList(List<SavedCount> counts) {
        Collections.sort(counts, new Comparator<SavedCount>() {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
            
            @Override
            public int compare(SavedCount o1, SavedCount o2) {
                // Always prioritize Pinned items first
                boolean p1 = o1.isPinned();
                boolean p2 = o2.isPinned();
                
                if (p1 && !p2) return -1;
                if (!p1 && p2) return 1;
                
                if (p1 && p2) {
                    // Both pinned: Sort by Pinned Timestamp DESC (Latest Pin First)
                    return Long.compare(o2.getPinnedTimestamp(), o1.getPinnedTimestamp());
                }

                // If both unpinned, sort by selected criteria
                switch (currentSort) {
                    case SORT_HIGHEST:
                        return Integer.compare(o2.getCount(), o1.getCount());
                    case SORT_LOWEST:
                        return Integer.compare(o1.getCount(), o2.getCount());
                    case SORT_A_Z:
                        return o1.getTitle().compareToIgnoreCase(o2.getTitle());
                    case SORT_Z_A:
                        return o2.getTitle().compareToIgnoreCase(o1.getTitle());
                    case SORT_EARLIEST: // Oldest first
                        try {
                            return sdf.parse(o1.getTimestamp()).compareTo(sdf.parse(o2.getTimestamp()));
                        } catch (ParseException e) {
                            return 0;
                        }
                    case SORT_LATEST: // Newest first
                    default:
                        try {
                            return sdf.parse(o2.getTimestamp()).compareTo(sdf.parse(o1.getTimestamp()));
                        } catch (ParseException e) {
                            return 0;
                        }
                }
            }
        });
    }


    private void showPinDialog(SavedCount item) {
        boolean isPinned = item.isPinned();
        
        // Check limit only if we are trying to PIN (not unpin)
        if (!isPinned && dbHelper.getPinnedCount() >= 3) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = getLayoutInflater().inflate(R.layout.dialog_message_ok, null);
            builder.setView(view);
            AlertDialog dialog = builder.create();
            
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            
            view.findViewById(R.id.btnOk).setOnClickListener(v -> dialog.dismiss());
            
            dialog.show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_pin_confirm, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView title = view.findViewById(R.id.tvPinDialogTitle);
        TextView msg = view.findViewById(R.id.tvPinDialogMessage);
        android.widget.Button btnAction = view.findViewById(R.id.btnPin);
        
        if (isPinned) {
            title.setText("Unpin Item");
            msg.setText("Do you want to unpin this item?");
            btnAction.setText("Unpin");
        } else {
            title.setText("Pin Item");
            msg.setText("Do you want to pin this item to the top?");
            btnAction.setText("Pin");
        }

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        btnAction.setOnClickListener(v -> {
            dbHelper.togglePin(item.getId(), !isPinned);
            loadCounts();
            dialog.dismiss();
        });

        dialog.show();
    }
}