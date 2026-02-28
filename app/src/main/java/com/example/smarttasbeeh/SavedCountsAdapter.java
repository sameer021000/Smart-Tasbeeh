package com.example.smarttasbeeh;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SavedCountsAdapter extends RecyclerView.Adapter<SavedCountsAdapter.ViewHolder> {

    private final List<SavedCount> savedCounts;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onContinueClick(SavedCount item);
        void onDeleteClick(SavedCount item);
        void onItemLongClick(SavedCount item);
    }

    private int selectedPosition = -1;

    public SavedCountsAdapter(List<SavedCount> savedCounts, OnItemClickListener listener) {
        this.savedCounts = savedCounts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_count, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedCount item = savedCounts.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvDate.setText(item.getTimestamp());
        holder.tvCount.setText(String.valueOf(item.getCount()));
        
        // Date Clock Icon (14dp manually sized)
        Drawable clockIcon = ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_clock);
        if (clockIcon != null) {
            clockIcon = DrawableCompat.wrap(clockIcon).mutate();
            int size = (int) (14 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            clockIcon.setBounds(0, 0, size, size);
            DrawableCompat.setTint(clockIcon, ContextCompat.getColor(holder.itemView.getContext(), R.color.hint_text));
            holder.tvDate.setCompoundDrawables(clockIcon, null, null, null);
        }
        
        // Show/Hide Pin Icon on tvCount (16dp manually sized)
        if (item.isPinned()) {
            Drawable pinIcon = ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_pin);
            if (pinIcon != null) {
                pinIcon = DrawableCompat.wrap(pinIcon).mutate();
                int size = (int) (16 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
                pinIcon.setBounds(0, 0, size, size);
                DrawableCompat.setTint(pinIcon, ContextCompat.getColor(holder.itemView.getContext(), R.color.tasbeeh_blue_primary));
                holder.tvCount.setCompoundDrawables(pinIcon, null, null, null);
            }
            holder.tvCount.setContentDescription(holder.itemView.getContext().getString(R.string.cd_pin) + ", " + item.getCount());
        } else {
            holder.tvCount.setCompoundDrawables(null, null, null, null);
            holder.tvCount.setContentDescription(String.valueOf(item.getCount()));
        }

        // Highlight Logic
        com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) holder.itemView;
        if (selectedPosition == position) {
            // Selected: Highlight with Primary Blue
            card.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.tasbeeh_blue_primary));
            card.setStrokeWidth(4); // Thicker border
        } else {
            // Default: Standard border
            card.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.card_stroke_color));
            card.setStrokeWidth(2); // Standard width (1dp approx 2-3px, let's use pixels or keep consistent)
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();
            if (selectedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPosition);
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            listener.onItemLongClick(item);
            return true;
        });

        holder.btnContinue.setOnClickListener(v -> listener.onContinueClick(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(item));
    }

    @Override
    public int getItemCount() {
        return savedCounts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvCount;
        android.widget.Button btnContinue;
        View btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvCount = itemView.findViewById(R.id.tvCount);
            btnContinue = itemView.findViewById(R.id.btnContinue);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}