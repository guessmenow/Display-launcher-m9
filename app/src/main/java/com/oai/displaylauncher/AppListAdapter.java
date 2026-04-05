package com.oai.displaylauncher;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    interface OnAppSelectedListener {
        void onAppSelected(AppTarget target);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_APP = 1;

    private final LayoutInflater inflater;
    private final List<AppListRow> rows = new ArrayList<>();
    private final OnAppSelectedListener listener;

    AppListAdapter(Context context, OnAppSelectedListener listener) {
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    void submitRows(List<AppListRow> items) {
        rows.clear();
        rows.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type == AppListRow.RowType.HEADER ? TYPE_HEADER : TYPE_APP;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_app_section_header, parent, false);
            return new HeaderHolder(view);
        }
        View view = inflater.inflate(R.layout.item_app_list_row, parent, false);
        return new AppHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AppListRow row = rows.get(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).bind(row.title);
        } else if (holder instanceof AppHolder) {
            ((AppHolder) holder).bind(row);
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final View accent;

        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.sectionTitle);
            accent = itemView.findViewById(R.id.sectionAccent);
        }

        void bind(String text) {
            title.setText(text);
            if (text.toLowerCase().contains("нави")) {
                accent.setBackgroundColor(itemView.getContext().getColor(R.color.section_nav));
            } else if (text.toLowerCase().contains("медиа")) {
                accent.setBackgroundColor(itemView.getContext().getColor(R.color.section_media));
            } else {
                accent.setBackgroundColor(itemView.getContext().getColor(R.color.section_other));
            }
        }
    }

    static class AppHolder extends RecyclerView.ViewHolder {
        private final com.google.android.material.card.MaterialCardView card;
        private final ImageView icon;
        private final TextView title;
        private final TextView subtitle;
        private final TextView status;
        private final TextView selectedBadge;
        private AppTarget boundTarget;

        AppHolder(@NonNull View itemView, OnAppSelectedListener listener) {
            super(itemView);
            card = itemView.findViewById(R.id.appRowCard);
            icon = itemView.findViewById(R.id.rowAppIcon);
            title = itemView.findViewById(R.id.rowAppTitle);
            subtitle = itemView.findViewById(R.id.rowAppSubtitle);
            status = itemView.findViewById(R.id.rowAppStatus);
            selectedBadge = itemView.findViewById(R.id.rowAppSelected);
            itemView.setOnClickListener(v -> {
                if (boundTarget != null) {
                    listener.onAppSelected(boundTarget);
                }
            });
        }

        void bind(AppListRow row) {
            boundTarget = row.app;
            if (row.app == null) {
                return;
            }
            icon.setImageDrawable(row.app.loadIcon(itemView.getContext()));
            title.setText(row.app.title);
            subtitle.setText(row.app.packageName);
            status.setText(row.shortcutsSupported ? "Ярлыки: поддерживаются" : "Ярлыки: не поддерживаются");
            selectedBadge.setVisibility(row.selected ? View.VISIBLE : View.GONE);
            if (row.selected) {
                card.setStrokeColor(itemView.getContext().getColor(R.color.dl_primary));
            } else {
                card.setStrokeColor(itemView.getContext().getColor(R.color.card_stroke));
            }
        }
    }
}
