package com.example.flutteraar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DemoListAdapter extends RecyclerView.Adapter<DemoListAdapter.DemoViewHolder> {
    public interface OnDemoClickListener {
        void onClick(DemoItem item);
    }

    private final List<DemoItem> items;
    private final OnDemoClickListener onDemoClickListener;

    public DemoListAdapter(List<DemoItem> items, OnDemoClickListener onDemoClickListener) {
        this.items = items;
        this.onDemoClickListener = onDemoClickListener;
    }

    @NonNull
    @Override
    public DemoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_demo, parent, false);
        return new DemoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DemoViewHolder holder, int position) {
        DemoItem item = items.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvDescription.setText(item.getDescription());
        holder.itemView.setOnClickListener(v -> onDemoClickListener.onClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class DemoViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvDescription;

        DemoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }
}
