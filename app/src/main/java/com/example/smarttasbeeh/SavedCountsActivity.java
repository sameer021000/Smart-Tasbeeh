package com.example.smarttasbeeh;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SavedCountsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private View emptyView;
    private DbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_counts);

        // Apply theme 
        // Logic handled by device usually, but ensuring consistent prefs read if we had complex theming
        
        dbHelper = new DbHelper(this);
        
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);
        
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
}
