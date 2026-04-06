package com.oai.displaylauncher;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import android.animation.ValueAnimator;
import android.content.res.ColorStateList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    interface OnAppSelectedListener {
        void onAppSelected(AppTarget target);

        void onLaunchRequested(AppTarget target, int displayId, String label);

        void onBindRequested(AppTarget target, int displayId, String label);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_APP = 1;
    private static final int TYPE_APP_SWAP = 2;

    private final LayoutInflater inflater;
    private final List<AppListRow> rows = new ArrayList<>();
    private final OnAppSelectedListener listener;
    private boolean swapLayout;

    AppListAdapter(Context context, OnAppSelectedListener listener) {
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    void submitRows(List<AppListRow> items) {
        rows.clear();
        rows.addAll(items);
        notifyDataSetChanged();
    }

    void setSwapLayout(boolean swapLayout) {
        if (this.swapLayout == swapLayout) {
            return;
        }
        this.swapLayout = swapLayout;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (rows.get(position).type == AppListRow.RowType.HEADER) {
            return TYPE_HEADER;
        }
        return swapLayout ? TYPE_APP_SWAP : TYPE_APP;
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
        int layout = viewType == TYPE_APP_SWAP
                ? R.layout.item_app_list_row_swapped
                : R.layout.item_app_list_row;
        View view = inflater.inflate(layout, parent, false);
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
        private final View selectedBadge;
        private final com.google.android.material.button.MaterialButton mainButton;
        private final com.google.android.material.button.MaterialButton passengerButton;
        private final com.google.android.material.button.MaterialButton fullButton;
        private final com.google.android.material.button.MaterialButton ceilingButton;
        private AppTarget boundTarget;

        AppHolder(@NonNull View itemView, OnAppSelectedListener listener) {
            super(itemView);
            card = itemView.findViewById(R.id.appRowCard);
            icon = itemView.findViewById(R.id.rowAppIcon);
            title = itemView.findViewById(R.id.rowAppTitle);
            subtitle = itemView.findViewById(R.id.rowAppSubtitle);
            status = itemView.findViewById(R.id.rowAppStatus);
            selectedBadge = itemView.findViewById(R.id.rowAppSelected);
            mainButton = itemView.findViewById(R.id.rowBtnMain);
            passengerButton = itemView.findViewById(R.id.rowBtnPassenger);
            fullButton = itemView.findViewById(R.id.rowBtnFull);
            ceilingButton = itemView.findViewById(R.id.rowBtnCeiling);
            itemView.setOnClickListener(v -> {
                if (boundTarget != null) {
                    listener.onAppSelected(boundTarget);
                }
            });
            mainButton.setOnClickListener(v -> {
                if (boundTarget != null) {
                    listener.onLaunchRequested(boundTarget, 1001, "основном экране");
                }
            });
            mainButton.setOnLongClickListener(v -> {
                if (boundTarget != null) {
                    listener.onBindRequested(boundTarget, 1001, "Основной");
                    return true;
                }
                return false;
            });
            passengerButton.setOnClickListener(v -> {
                if (boundTarget != null) {
                    listener.onLaunchRequested(boundTarget, 1002, "пассажирском экране");
                }
            });
            passengerButton.setOnLongClickListener(v -> {
                if (boundTarget != null) {
                    listener.onBindRequested(boundTarget, 1002, "Пассажирский");
                    return true;
                }
                return false;
            });
            fullButton.setOnClickListener(v -> {
                if (boundTarget != null) {
                    listener.onLaunchRequested(boundTarget, 1003, "весь экран");
                }
            });
            fullButton.setOnLongClickListener(v -> {
                if (boundTarget != null) {
                    listener.onBindRequested(boundTarget, 1003, "Весь экран");
                    return true;
                }
                return false;
            });
            ceilingButton.setOnClickListener(v -> {
                if (boundTarget != null) {
                    listener.onLaunchRequested(boundTarget, 3, "потолочном экране");
                }
            });
            ceilingButton.setOnLongClickListener(v -> {
                if (boundTarget != null) {
                    listener.onBindRequested(boundTarget, 3, "Потолочный");
                    return true;
                }
                return false;
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
            if (row.bindingSummary != null && !row.bindingSummary.trim().isEmpty()) {
                status.setText(itemView.getContext().getString(
                        R.string.shortcuts_pinned_status, row.bindingSummary));
            } else {
                status.setText(row.shortcutsSupported
                        ? itemView.getContext().getString(R.string.shortcuts_available)
                        : itemView.getContext().getString(R.string.shortcuts_hidden));
            }
            String summary = row.bindingSummary == null ? "" : row.bindingSummary;
            applyPinnedStyle(mainButton, summary.contains("Основной"));
            applyPinnedStyle(passengerButton, summary.contains("Пассажирский"));
            applyPinnedStyle(fullButton, summary.contains("Весь экран"));
            applyPinnedStyle(ceilingButton, summary.contains("Потолочный"));
            selectedBadge.setVisibility(View.GONE);
            if (row.selected) {
                card.setStrokeColor(itemView.getContext().getColor(R.color.dl_primary));
            } else {
                card.setStrokeColor(itemView.getContext().getColor(R.color.card_stroke));
            }
        }

        private void applyPinnedStyle(com.google.android.material.button.MaterialButton button, boolean pinned) {
            int tint = itemView.getContext().getColor(pinned ? R.color.dl_primary : R.color.text_primary);
            int stroke = itemView.getContext().getColor(pinned ? R.color.dl_primary : R.color.card_stroke);
            int base = itemView.getContext().getColor(R.color.dl_primary);
            int surface = itemView.getContext().getColor(R.color.card_surface);
            button.setIconTint(ColorStateList.valueOf(tint));
            button.setStrokeColor(ColorStateList.valueOf(stroke));
            if (pinned) {
                startPulse(button, base);
            } else {
                stopPulse(button);
                button.setBackgroundTintList(ColorStateList.valueOf(surface));
            }
        }

        private void startPulse(com.google.android.material.button.MaterialButton button, int baseColor) {
            ValueAnimator existing = (ValueAnimator) button.getTag(R.id.tag_pulse_animator);
            if (existing != null) {
                return;
            }
            ValueAnimator animator = ValueAnimator.ofFloat(0.08f, 0.25f);
            animator.setDuration(2200);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.addUpdateListener(animation -> {
                float alpha = (float) animation.getAnimatedValue();
                int color = applyAlpha(baseColor, alpha);
                button.setBackgroundTintList(ColorStateList.valueOf(color));
            });
            button.setTag(R.id.tag_pulse_animator, animator);
            animator.start();
        }

        private void stopPulse(com.google.android.material.button.MaterialButton button) {
            ValueAnimator existing = (ValueAnimator) button.getTag(R.id.tag_pulse_animator);
            if (existing != null) {
                existing.cancel();
                button.setTag(R.id.tag_pulse_animator, null);
            }
        }

        private int applyAlpha(int color, float alpha) {
            int a = Math.min(255, Math.max(0, (int) (alpha * 255)));
            return (color & 0x00ffffff) | (a << 24);
        }
    }
}
