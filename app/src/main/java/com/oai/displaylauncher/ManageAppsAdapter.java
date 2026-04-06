package com.oai.displaylauncher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ManageAppsAdapter extends RecyclerView.Adapter<ManageAppsAdapter.AppViewHolder> {
    interface OnSelectionChanged {
        void onSelectionChanged(int selectedCount, int totalCount);
    }

    private final List<AppTarget> items = new ArrayList<>();
    private final Set<String> selectedPackages = new HashSet<>();
    private final OnSelectionChanged callback;

    ManageAppsAdapter(Set<String> initialSelection, OnSelectionChanged callback) {
        if (initialSelection != null) {
            selectedPackages.addAll(initialSelection);
        }
        this.callback = callback;
    }

    void submit(List<AppTarget> apps) {
        items.clear();
        items.addAll(apps);
        notifyDataSetChanged();
        notifySelection();
    }

    void selectAll() {
        selectedPackages.clear();
        for (AppTarget target : items) {
            selectedPackages.add(target.packageName);
        }
        notifyDataSetChanged();
        notifySelection();
    }

    void clearAll() {
        selectedPackages.clear();
        notifyDataSetChanged();
        notifySelection();
    }

    Set<String> getSelectedPackages() {
        return new HashSet<>(selectedPackages);
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manage_app_row, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppTarget target = items.get(position);
        holder.bind(target, selectedPackages.contains(target.packageName));
        holder.itemView.setOnClickListener(v -> toggle(target, holder));
        holder.checkbox.setOnClickListener(v -> toggle(target, holder));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void toggle(AppTarget target, AppViewHolder holder) {
        if (selectedPackages.contains(target.packageName)) {
            selectedPackages.remove(target.packageName);
            holder.checkbox.setChecked(false);
        } else {
            selectedPackages.add(target.packageName);
            holder.checkbox.setChecked(true);
        }
        notifySelection();
    }

    private void notifySelection() {
        if (callback != null) {
            callback.onSelectionChanged(selectedPackages.size(), items.size());
        }
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView subtitle;
        final MaterialCheckBox checkbox;

        AppViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.manageAppIcon);
            title = itemView.findViewById(R.id.manageAppTitle);
            subtitle = itemView.findViewById(R.id.manageAppSubtitle);
            checkbox = itemView.findViewById(R.id.manageAppCheckbox);
        }

        void bind(AppTarget target, boolean checked) {
            icon.setImageDrawable(target.loadIcon(itemView.getContext()));
            title.setText(target.title);
            subtitle.setText(target.packageName);
            checkbox.setChecked(checked);
        }
    }
}
