package com.oai.displaylauncher;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DebugActivity extends AppCompatActivity {
    private TextView diagnosticsText;
    private TextView debugLogText;
    private androidx.appcompat.widget.SwitchCompat swapToggle;
    private androidx.appcompat.widget.SwitchCompat distinctToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        diagnosticsText = findViewById(R.id.diagnosticsText);
        debugLogText = findViewById(R.id.debugLogText);
        swapToggle = findViewById(R.id.debugSwapToggle);
        distinctToggle = findViewById(R.id.debugDistinctShortcutsToggle);

        View backButton = findViewById(R.id.debugBackButton);
        View refreshButton = findViewById(R.id.debugRefreshButton);
        View diagnosticsRefresh = findViewById(R.id.diagnosticsRefreshButton);
        View engineerButton = findViewById(R.id.engineerButton);

        backButton.setOnClickListener(v -> finish());
        refreshButton.setOnClickListener(v -> refreshDiagnostics());
        diagnosticsRefresh.setOnClickListener(v -> refreshDiagnostics());
        engineerButton.setOnClickListener(v -> openEngineerMenu());
        setupSwapToggle();
        setupDistinctToggle();

        refreshDiagnostics();
    }

    private void refreshDiagnostics() {
        if (diagnosticsText != null) {
            diagnosticsText.setText(buildDiagnosticsReport());
        }
    }

    private void setupSwapToggle() {
        if (swapToggle == null) {
            return;
        }
        swapToggle.setChecked(isSwapEnabled());
        swapToggle.setOnCheckedChangeListener((buttonView, isChecked) ->
                saveSwapEnabled(isChecked));
    }

    private void setupDistinctToggle() {
        if (distinctToggle == null) {
            return;
        }
        distinctToggle.setChecked(ShortcutModeManager.isDistinctModeEnabled(this));
        distinctToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ShortcutModeManager.setDistinctMode(this, isChecked);
            appendLog(isChecked
                    ? getString(R.string.debug_shortcut_mode_enabled)
                    : getString(R.string.debug_shortcut_mode_disabled));
            refreshDiagnostics();
        });
    }

    private boolean isSwapEnabled() {
        return getSharedPreferences("ui_prefs", MODE_PRIVATE)
                .getBoolean("swap_app_row", false);
    }

    private void saveSwapEnabled(boolean enabled) {
        getSharedPreferences("ui_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("swap_app_row", enabled)
                .apply();
    }

    private String buildDiagnosticsReport() {
        StringBuilder builder = new StringBuilder();
        android.hardware.display.DisplayManager displayManager =
                (android.hardware.display.DisplayManager) getSystemService(DISPLAY_SERVICE);
        android.view.Display[] displays = displayManager == null
                ? new android.view.Display[0] : displayManager.getDisplays();

        builder.append(getString(R.string.debug_last_update))
                .append(" ")
                .append(new SimpleDateFormat("HH:mm:ss", new Locale("ru"))
                        .format(new Date()))
                .append("\n");

        builder.append(getString(R.string.debug_displays_label));
        if (displays.length == 0) {
            builder.append(" ").append(getString(R.string.debug_displays_none));
        } else {
            for (int i = 0; i < displays.length; i++) {
                android.view.Display display = displays[i];
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append("#")
                        .append(display.getDisplayId())
                        .append(" (")
                        .append(display.getName())
                        .append(")");
            }
        }

        builder.append("\n")
                .append(getString(R.string.debug_bindings_label));
        boolean hasBindings = false;
        List<DisplayEntry> entries = buildDisplayEntries();
        for (DisplayEntry entry : entries) {
            DisplayBinding binding = loadDisplayBinding(entry.displayId);
            if (binding == null) {
                continue;
            }
            if (!hasBindings) {
                builder.append("\n");
            }
            hasBindings = true;
            builder.append("- ")
                    .append(entry.label)
                    .append(" (Display ")
                    .append(entry.displayId)
                    .append("): ")
                    .append(binding.title)
                    .append("\n");
        }
        if (!hasBindings) {
            builder.append(getString(R.string.debug_bindings_none));
        }

        builder.append("\n")
                .append(getString(R.string.debug_shortcuts_label))
                .append(" ")
                .append(ShortcutPinReceiver.getLastStatus(this));

        return builder.toString().trim();
    }

    private void openEngineerMenu() {
        appendLog(getString(R.string.debug_engineer_request));
        boolean settingsOk = runShellCommand("settings put global persist.switch.usbmode true");
        if (settingsOk) {
            appendLog(getString(R.string.debug_engineer_settings_ok));
        } else {
            appendLog(getString(R.string.debug_engineer_settings_failed));
        }

        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.geely.engineermode", "com.geely.engineermode.MainActivity"));
            startActivity(intent);
            appendLog(getString(R.string.debug_engineer_activity_ok));
        } catch (Exception e) {
            appendLog(getString(R.string.debug_engineer_activity_failed));
            Toast.makeText(this, getString(R.string.debug_engineer_activity_failed), Toast.LENGTH_LONG).show();
        }
    }

    private boolean runShellCommand(String command) {
        try {
            Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void appendLog(String line) {
        if (debugLogText == null) {
            return;
        }
        String prefix = new SimpleDateFormat("HH:mm:ss", new Locale("ru")).format(new Date());
        String current = debugLogText.getText() == null ? "" : debugLogText.getText().toString();
        String next = current;
        if (next == null || next.trim().isEmpty()
                || next.equals(getString(R.string.debug_log_empty))) {
            next = "";
        }
        if (!next.isEmpty()) {
            next += "\n";
        }
        next += prefix + " · " + line;
        debugLogText.setText(next);
    }

    private List<DisplayEntry> buildDisplayEntries() {
        List<DisplayEntry> entries = new ArrayList<>();
        entries.add(new DisplayEntry("Основной", 1001));
        entries.add(new DisplayEntry("Пассажирский", 1002));
        entries.add(new DisplayEntry("Весь экран", 1003));
        entries.add(new DisplayEntry("Потолочный", 3));
        return entries;
    }

    private DisplayBinding loadDisplayBinding(int displayId) {
        String pkg = getSharedPreferences("display_bindings", MODE_PRIVATE)
                .getString(displayId + ".package", null);
        if (pkg == null || pkg.trim().isEmpty()) {
            return null;
        }
        String title = getSharedPreferences("display_bindings", MODE_PRIVATE)
                .getString(displayId + ".title", pkg);
        String activity = getSharedPreferences("display_bindings", MODE_PRIVATE)
                .getString(displayId + ".activity", null);
        return new DisplayBinding(title, pkg, activity);
    }

    private record DisplayEntry(String label, int displayId) {
    }

    private record DisplayBinding(String title, String packageName, String activityName) {
    }
}
