package com.oai.displaylauncher;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class AppChoiceAdapter extends ArrayAdapter<AppTarget> {
    private final LayoutInflater inflater;

    public AppChoiceAdapter(@NonNull Context context, @NonNull List<AppTarget> objects) {
        super(context, 0, objects);
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return buildView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return buildView(position, convertView, parent);
    }

    private View buildView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_app_option, parent, false);
        }

        AppTarget item = getItem(position);
        ImageView icon = view.findViewById(R.id.appIcon);
        TextView title = view.findViewById(R.id.appTitle);
        TextView subtitle = view.findViewById(R.id.appSubtitle);

        if (item != null) {
            icon.setImageDrawable(item.loadIcon(getContext()));
            title.setText(item.title);
            subtitle.setText(item.description);
        }
        return view;
    }
}
