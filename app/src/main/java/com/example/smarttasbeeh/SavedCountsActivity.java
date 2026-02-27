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
        
        dbHelper = new DbHelper(this);
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);

        // Load saved sort order
        currentSort = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(KEY_SORT_ORDER, SORT_LATEST);

        ivSort = findViewById(R.id.btnSort); 
        ivSort.setOnClickListener(this::showSortMenu);

        // Top Bar Logic (Custom Header)
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

        // 1. Separate into Pinned and Unpinned
        java.util.List<SavedCount> pinnedList = new java.util.ArrayList<>();
        java.util.List<SavedCount> unpinnedList = new java.util.ArrayList<>();

        for (SavedCount item : counts) {
            if (item.isPinned()) {
                pinnedList.add(item);
            } else {
                unpinnedList.add(item);
            }
        }

        // 2. Sort Pinned List (Always Latest Pinned First)
        Collections.sort(pinnedList, (o1, o2) -> Long.compare(o2.getPinnedTimestamp(), o1.getPinnedTimestamp()));

        // 3. Sort Unpinned List (Based on User Preference)
        sortUnpinnedList(unpinnedList);

        // Update Badge
        TextView badge = findViewById(R.id.recordCountBadge);
        if (badge != null) {
            badge.setText(counts.size() + " records");
        }

        // Listener
        SavedCountsAdapter.OnItemClickListener listener = new SavedCountsAdapter.OnItemClickListener() {
            @Override
            public void onContinueClick(SavedCount item) {
                getSharedPreferences("TasbeehPrefs", MODE_PRIVATE)
                        .edit()
                        .putInt("count", item.getCount())
                        .putInt("resume_id", item.getId())
                        .putString("resume_title", item.getTitle())
                        .apply();
                Intent intent = new Intent(SavedCountsActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }

            @Override
            public void onDeleteClick(SavedCount item) {
                showDeleteDialog(item);
            }

            @Override
            public void onItemLongClick(SavedCount item) {
                showPinDialog(item);
            }
        };

        if (counts.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            
            List<SavedCount> fullList = new java.util.ArrayList<>();
            fullList.addAll(pinnedList);
            fullList.addAll(unpinnedList);
            
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new SavedCountsAdapter(fullList, listener));
        }
    }

    private void showDeleteDialog(SavedCount item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SavedCountsActivity.this);
        View view = getLayoutInflater().inflate(R.layout.dialog_delete_confirm, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Calculate Fluid Text Sizes based on Screen Width
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        float screenWidth = metrics.widthPixels;
        float titleSize = screenWidth * 0.044f; // ~4.4% of width
        float messageSize = screenWidth * 0.035f; // ~3.5% of width

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        if (tvTitle != null) {
            tvTitle.setText(getString(R.string.dialog_delete_title, item.getTitle()));
            tvTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, titleSize);
        }

        TextView tvMsg = view.findViewById(R.id.tvMessage);
        if (tvMsg != null) {
            tvMsg.setText(getString(R.string.dialog_delete_message, item.getTitle()));
            tvMsg.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, messageSize);
        }

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnDeleteConfirm).setOnClickListener(v -> {
            dbHelper.deleteCount(item.getId());
            
            // Clear resume state if the deleted item was currently being resumed
            android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            int resumeId = prefs.getInt("resume_id", -1);
            if (resumeId == item.getId()) {
                prefs.edit()
                    .remove("resume_id")
                    .remove("resume_title")
                    .apply();
            }
            
            loadCounts(); // Refresh list
            dialog.dismiss();
        });

        dialog.show();
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
            final View option = sheetView.findViewById(ids[i]);
            if (types[i] == sortType) {
                // Selected: Highlight with stroke/background
                option.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_sort_selected));
                
                // Fluid Corner Radius: Clip to 20% of the view's height
                option.setOutlineProvider(new android.view.ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, android.graphics.Outline outline) {
                        int radius = (int) (view.getHeight() * 0.20f); // 20% of height for ideal proportionality
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                    }
                });
                option.setClipToOutline(true);
            } else {
                // Default: Selectable Item Background
                android.util.TypedValue outValue = new android.util.TypedValue();
                getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                option.setBackgroundResource(outValue.resourceId);
                option.setClipToOutline(false); // Reset clipping for unselected
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

    private void sortUnpinnedList(List<SavedCount> counts) {
        Collections.sort(counts, new Comparator<SavedCount>() {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
            
            @Override
            public int compare(SavedCount o1, SavedCount o2) {
                // Only normal sorting logic here
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

            // Calculate Fluid Text Sizes based on Screen Width
            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
            float screenWidth = metrics.widthPixels;
            float titleSize = screenWidth * 0.044f; // ~4.4% of width
            float messageSize = screenWidth * 0.035f; // ~3.5% of width

            TextView tvTitle = view.findViewById(R.id.tvPinDialogTitle);
            if (tvTitle != null) {
                tvTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, titleSize);
            }

            TextView tvMsg = view.findViewById(R.id.tvPinDialogMessage);
            if (tvMsg != null) {
                tvMsg.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, messageSize);
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

        // Calculate Fluid Text Sizes based on Screen Width
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        float screenWidth = metrics.widthPixels;
        float titleSize = screenWidth * 0.044f; // Matches refined delete dialog
        float messageSize = screenWidth * 0.035f;

        TextView title = view.findViewById(R.id.tvPinDialogTitle);
        TextView msg = view.findViewById(R.id.tvPinDialogMessage);
        android.widget.Button btnAction = view.findViewById(R.id.btnPin);

        if (title != null) title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, titleSize);
        if (msg != null) msg.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, messageSize);
        
        if (isPinned) {
            if (title != null) title.setText(getString(R.string.dialog_unpin_title, item.getTitle()));
            if (msg != null) msg.setText(getString(R.string.dialog_unpin_message, item.getTitle()));
            if (btnAction != null) btnAction.setText(R.string.dialog_unpin_confirm);
        } else {
            if (title != null) title.setText(getString(R.string.dialog_pin_title, item.getTitle()));
            if (msg != null) msg.setText(getString(R.string.dialog_pin_message, item.getTitle()));
            if (btnAction != null) btnAction.setText(R.string.dialog_pin_confirm);
        }

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        if (btnAction != null) {
            btnAction.setOnClickListener(v -> {
                dbHelper.togglePin(item.getId(), !isPinned);
                loadCounts();
                dialog.dismiss();
            });
        }

        dialog.show();
    }
}