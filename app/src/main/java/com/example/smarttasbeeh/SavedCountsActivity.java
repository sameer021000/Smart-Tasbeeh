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
            });
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        }
    }
    private void showSortMenu(View view) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_sort, null);
        bottomSheetDialog.setContentView(sheetView);

        // Click Listeners
        sheetView.findViewById(R.id.optionLatest).setOnClickListener(v -> updateSort(SORT_LATEST, bottomSheetDialog));
        sheetView.findViewById(R.id.optionEarliest).setOnClickListener(v -> updateSort(SORT_EARLIEST, bottomSheetDialog));
        sheetView.findViewById(R.id.optionHighest).setOnClickListener(v -> updateSort(SORT_HIGHEST, bottomSheetDialog));
        sheetView.findViewById(R.id.optionLowest).setOnClickListener(v -> updateSort(SORT_LOWEST, bottomSheetDialog));
        sheetView.findViewById(R.id.optionAZ).setOnClickListener(v -> updateSort(SORT_A_Z, bottomSheetDialog));
        sheetView.findViewById(R.id.optionZA).setOnClickListener(v -> updateSort(SORT_Z_A, bottomSheetDialog));

        bottomSheetDialog.show();
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
}