package com.oai.displaylauncher;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {
    private ManageAppsAdapter adapter;
    private TextView selectionSummary;
    private View loadingView;
    private RecyclerView recycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        selectionSummary = findViewById(R.id.selectionSummary);
        loadingView = findViewById(R.id.settingsLoading);
        recycler = findViewById(R.id.settingsRecycler);

        Set<String> initial = AppScanner.loadManagedPackages(this);
        adapter = new ManageAppsAdapter(initial, this::updateSelectionSummary);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        MaterialButton selectAll = findViewById(R.id.selectAllButton);
        MaterialButton clearAll = findViewById(R.id.clearAllButton);
        MaterialButton save = findViewById(R.id.saveSelectionButton);
        View refresh = findViewById(R.id.refreshListButton);
        findViewById(R.id.settingsBackButton).setOnClickListener(v -> finish());

        selectAll.setOnClickListener(v -> adapter.selectAll());
        clearAll.setOnClickListener(v -> adapter.clearAll());
        save.setOnClickListener(v -> saveSelection());
        refresh.setOnClickListener(v -> loadApps());

        loadApps();
    }

    private void loadApps() {
        setLoading(true);
        new Thread(() -> {
            try {
                List<AppTarget> apps = AppScanner.loadLaunchableUserApps(this, null);
                runOnUiThread(() -> {
                    adapter.submit(apps);
                    setLoading(false);
                });
            } catch (Throwable t) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Ошибка сканирования: " + t.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void saveSelection() {
        AppScanner.saveManagedPackages(this, adapter.getSelectedPackages());
        Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void setLoading(boolean loading) {
        loadingView.setVisibility(loading ? View.VISIBLE : View.GONE);
        recycler.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void updateSelectionSummary(int selectedCount, int totalCount) {
        selectionSummary.setText(getString(R.string.settings_selection_summary, selectedCount, totalCount));
    }

}
