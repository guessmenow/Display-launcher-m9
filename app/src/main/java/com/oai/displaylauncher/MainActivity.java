package com.oai.displaylauncher;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private final List<AppTarget> appTargets = new ArrayList<>();
    private final List<DisplayTarget> displayTargets = new ArrayList<>();
    @Nullable
    private AppTarget selectedTarget;

    // selected app summary removed
    private View hideShortcutsButton;
    private View hideSelectedShortcutsButton;
    private View carAppCard;
    private TextView carAppStatusText;
    private View debugButton;
    private View communityLink;
    // app version text handled locally
    // quick launch removed

    private RecyclerView appsRecycler;
    private AppListAdapter appListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prepareDisplayTargets();
        bindViews();
        setupClickListeners();
        updateLoadingState(true);
        loadInstalledTargetsAsync();
    }

    private void prepareDisplayTargets() {
        displayTargets.add(new DisplayTarget("Основной", 1001));
        displayTargets.add(new DisplayTarget("Пассажирский", 1002));
        displayTargets.add(new DisplayTarget("Весь экран", 1003));
        displayTargets.add(new DisplayTarget("Потолочный", 3));
    }

    private void bindViews() {
        // selected app summary removed
        hideShortcutsButton = findViewById(R.id.hideShortcutsButton);
        hideSelectedShortcutsButton = findViewById(R.id.hideSelectedShortcutsButton);
        carAppCard = findViewById(R.id.carAppCard);
        carAppStatusText = findViewById(R.id.carAppStatusText);
        debugButton = findViewById(R.id.debugButton);
        communityLink = findViewById(R.id.appCommunityLink);
        TextView appVersionText = findViewById(R.id.appVersionText);
        // quick launch removed from layout
        appsRecycler = findViewById(R.id.appsRecycler);

        appListAdapter = new AppListAdapter(this, new AppListAdapter.OnAppSelectedListener() {
            @Override
            public void onAppSelected(AppTarget target) {
                selectedTarget = target;
                // selected app summary removed
                rebuildAppList();
            }

            @Override
            public void onLaunchRequested(AppTarget target, int displayId, String label) {
                selectedTarget = target;
                // selected app summary removed
                launchTargetOnDisplay(target, displayId, label);
                rebuildAppList();
            }

            @Override
            public void onBindRequested(AppTarget target, int displayId, String label) {
                handleDistinctBinding(target, displayId, label);
                return;

            }
        });
        appsRecycler.setLayoutManager(new LinearLayoutManager(this));
        appsRecycler.setAdapter(appListAdapter);
        appListAdapter.setSwapLayout(shouldSwapAppRow());
        if (appVersionText != null) {
            appVersionText.setText(getString(R.string.version_format, readVersionName()));
        }
    }

    private void setupClickListeners() {
        // app picker removed; selection happens in the list
        findViewById(R.id.settingsButton)
                .setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        hideShortcutsButton.setOnClickListener(this::hideAllAliasShortcuts);
        hideSelectedShortcutsButton.setOnClickListener(this::hideSelectedAliasShortcuts);
        carAppCard.setOnClickListener(v -> triggerCarApp());
        findViewById(R.id.carAppTriggerButton).setOnClickListener(v -> triggerCarApp());
        debugButton.setOnClickListener(v -> startActivity(new Intent(this, DebugActivity.class)));
        communityLink.setOnClickListener(v -> openCommunityLink());
        // diagnostics moved to SettingsActivity
        // quick launch + launch cards removed from UI
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLoadingState(true);
        loadInstalledTargetsAsync();
        appListAdapter.setSwapLayout(shouldSwapAppRow());
    }

    private void updateLoadingState(boolean loading) {
        hideShortcutsButton.setEnabled(!loading);
        hideSelectedShortcutsButton.setEnabled(!loading);
        carAppCard.setEnabled(!loading);
        appsRecycler.setVisibility(loading ? View.GONE : View.VISIBLE);
        // selected app summary removed
    }

    private void loadInstalledTargetsAsync() {
        new Thread(() -> {
            try {
                List<AppTarget> loaded = AppScanner.loadLaunchableUserApps(this, AppScanner.loadManagedPackages(this));
                runOnUiThread(() -> applyLoadedTargets(loaded));
            } catch (Throwable t) {
                runOnUiThread(() -> {
                    updateLoadingState(false);
                    selectedTarget = null;
                    // selected app summary removed
                });
            }
        }).start();
    }

    private void applyLoadedTargets(List<AppTarget> loaded) {
        appTargets.clear();
        appTargets.addAll(loaded);

        if (appTargets.isEmpty()) {
            updateLoadingState(false);
            selectedTarget = null;
            // selected app summary removed
            appsRecycler.setVisibility(View.GONE);
            return;
        }

        selectedTarget = null;
        updateLoadingState(false);
        hideShortcutsButton.setEnabled(true);
        hideSelectedShortcutsButton.setEnabled(true);
        // selected app summary removed
        updateCarAppStatus();
        rebuildAppList();
    }

    // quick launch removed

    private void rebuildAppList() {
        if (appTargets.isEmpty()) {
            appListAdapter.submitRows(new ArrayList<>());
            return;
        }

        List<AppTarget> navApps = new ArrayList<>();
        List<AppTarget> mediaApps = new ArrayList<>();
        List<AppTarget> otherApps = new ArrayList<>();
        for (AppTarget target : appTargets) {
            SectionType section = classifyApp(target);
            if (section == SectionType.NAVIGATION) {
                navApps.add(target);
            } else if (section == SectionType.MEDIA) {
                mediaApps.add(target);
            } else {
                otherApps.add(target);
            }
        }

        List<AppListRow> rows = new ArrayList<>();
        if (!navApps.isEmpty()) {
            rows.add(AppListRow.header("Навигация"));
            addAppsBySection(rows, navApps);
        }
        if (!mediaApps.isEmpty()) {
            rows.add(AppListRow.header("Медиа"));
            addAppsBySection(rows, mediaApps);
        }
        if (!otherApps.isEmpty()) {
            rows.add(AppListRow.header("Прочее"));
            addAppsBySection(rows, otherApps);
        }
        appListAdapter.submitRows(rows);
    }

    private void addAppsBySection(List<AppListRow> rows, List<AppTarget> targets) {
        for (AppTarget target : targets) {
            boolean selected = selectedTarget != null && target.packageName.equals(selectedTarget.packageName);
            boolean supported = SupportedAliasCatalog.findForTarget(target) != null;
            rows.add(AppListRow.app(target, selected, supported, buildBindingSummary(target)));
        }
    }

    private void handleDistinctBinding(AppTarget target, int displayId, String label) {
        SupportedAliasCatalog.SupportedAliasApp supported = SupportedAliasCatalog.findForTarget(target);
        if (supported != null) {
            String aliasClass = aliasForDisplay(supported, displayId);
            if (aliasClass == null) {
                showToast("Для этого экрана нет ярлыка");
                return;
            }

            DisplayBinding existing = loadDisplayBinding(displayId);
            if (existing != null && existing.aliasClass != null
                    && existing.aliasClass.equals(aliasClass)
                    && existing.packageName.equals(target.packageName)) {
                setComponentsEnabled(new String[] { existing.aliasClass },
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
                clearDisplayBinding(displayId);
                rebuildAppList();
                showToast("Ярлык удален: " + label);
                return;
            }

            if (existing != null && existing.aliasClass != null) {
                setComponentsEnabled(new String[] { existing.aliasClass },
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
            }

            saveSelectedLauncherTarget(supported, target);
            setComponentsEnabled(new String[] { aliasClass },
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
            saveDisplayBinding(displayId, target, aliasClass);
            rebuildAppList();
            showToast("Ярлык создан: " + target.title + " " + displayIndex(displayId));
            return;
        }

        // fallback to legacy shortcuts
        DisplayBinding existing = loadDisplayBinding(displayId);
        if (existing != null) {
            if (existing.aliasClass != null) {
                setComponentsEnabled(new String[] { existing.aliasClass },
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
            }
            if (existing.packageName.equals(target.packageName)) {
                clearDisplayBinding(displayId);
                ShortcutPinReceiver.removeLegacyShortcutForDisplay(
                        MainActivity.this,
                        existing.packageName,
                        existing.activityName,
                        existing.title,
                        label,
                        displayId
                );
                rebuildAppList();
                showToast("Ярлык удален: " + label);
                return;
            }

            ShortcutPinReceiver.removeLegacyShortcutForDisplay(
                    MainActivity.this,
                    existing.packageName,
                    existing.activityName,
                    existing.title,
                    label,
                    displayId
            );
            clearDisplayBinding(displayId);
        }

        saveDisplayBinding(displayId, target, null);
        ShortcutPinReceiver.createShortcutForDisplay(
                MainActivity.this,
                target.packageName,
                target.explicitActivity,
                target.title,
                label,
                displayId
        );
        rebuildAppList();
        showToast("Ярлык: " + ShortcutPinReceiver.getLastStatus(MainActivity.this));
    }

    private void launchTargetOnDisplay(AppTarget target, int displayId, String label) {
        if (!LaunchHelper.isPackageInstalled(this, target.packageName)) {
            showToast("Приложение не найдено: " + target.title);
            return;
        }

        if (!LaunchHelper.isExplicitActivityValid(this, target.packageName, target.explicitActivity)) {
            showToast("Launcher activity не найдена: " + target.explicitActivity);
            return;
        }

        try {
            LaunchHelper.launch(this, target.packageName, target.explicitActivity, displayId);
        } catch (Exception e) {
            String message = e.getClass().getSimpleName() + ": " + safeMessage(e.getMessage());
            new AlertDialog.Builder(this)
                .setTitle(getString(R.string.launch_failed_title, label))
                .setMessage(message)
                .setPositiveButton("Понятно", null)
                .show();
        }
    }


@Nullable
private SupportedAliasCatalog.SupportedAliasApp findSupportedAliasApp() {
    if (selectedTarget == null) {
        return null;
    }
    return SupportedAliasCatalog.findForTarget(selectedTarget);
}

@Nullable
private String aliasForDisplay(SupportedAliasCatalog.SupportedAliasApp supported, int displayId) {
    if (supported == null || supported.aliasClassNames == null || supported.aliasClassNames.length < 4) {
        return null;
    }
    if (displayId == 1001) {
        return supported.aliasClassNames[0];
    }
    if (displayId == 1002) {
        return supported.aliasClassNames[1];
    }
    if (displayId == 1003) {
        return supported.aliasClassNames[2];
    }
    if (displayId == 3) {
        return supported.aliasClassNames[3];
    }
    return null;
}

private boolean isAliasEnabled(String className) {
    if (className == null) {
        return false;
    }
    PackageManager pm = getPackageManager();
    ComponentName componentName = new ComponentName(this, className);
    int state = pm.getComponentEnabledSetting(componentName);
    if (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0).enabled;
        } catch (Exception ignored) {
            return false;
        }
    }
    return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
}

private boolean isAliasTargetMatch(String groupKey, String packageName) {
    if (groupKey == null || packageName == null) {
        return false;
    }
    String saved = getSharedPreferences("launcher_targets", MODE_PRIVATE)
            .getString(groupKey + ".package", null);
    return packageName.equals(saved);
}

private void clearAliasTarget(String groupKey) {
    if (groupKey == null) {
        return;
    }
    getSharedPreferences("launcher_targets", MODE_PRIVATE)
            .edit()
            .remove(groupKey + ".package")
            .remove(groupKey + ".activity")
            .remove(groupKey + ".title")
            .apply();
}


    private void hideAllAliasShortcuts(View anchor) {
        int disabled = setComponentsEnabled(
                SupportedAliasCatalog.getAllAliasClassNames(),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        );
        clearAllDisplayBindings();
        if (disabled > 0) {
            showToast("Ярлыки скрыты. Лаунчер может обновиться не сразу");
            updateCarAppStatus();
            rebuildAppList();
        } else {
            showToast("Не удалось скрыть ярлыки");
        }
    }

    private void hideSelectedAliasShortcuts(View anchor) {
        SupportedAliasCatalog.SupportedAliasApp supported = findSupportedAliasApp();
        if (supported == null) {
            showToast("Для выбранного приложения ярлыки не поддерживаются");
            return;
        }

        int disabled = setComponentsEnabled(
                supported.aliasClassNames,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        );
        getSharedPreferences("launcher_targets", MODE_PRIVATE)
                .edit()
                .remove(supported.groupKey + ".package")
                .remove(supported.groupKey + ".activity")
                .remove(supported.groupKey + ".title")
                .apply();
        if (disabled > 0) {
            showToast("Ярлыки скрыты: " + supported.appTitle + ". Лаунчер может обновиться не сразу");
            updateCarAppStatus();
            rebuildAppList();
        } else {
            showToast("Не удалось скрыть ярлыки");
        }
    }

    private void clearAllDisplayBindings() {
        for (DisplayTarget target : displayTargets) {
            DisplayBinding binding = loadDisplayBinding(target.displayId);
            if (binding == null) {
                continue;
            }
            if (binding.aliasClass != null) {
                setComponentsEnabled(new String[] { binding.aliasClass },
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
            } else {
                ShortcutPinReceiver.removeLegacyShortcutForDisplay(
                        MainActivity.this,
                        binding.packageName,
                        binding.activityName,
                        binding.title,
                        target.label,
                        target.displayId
                );
            }
            clearDisplayBinding(target.displayId);
        }
    }

    private void updateCarAppStatus() {
        boolean anyInstalled = LaunchHelper.isPackageInstalled(this, "ru.dublgis.dgismobile")
                || LaunchHelper.isPackageInstalled(this, "ru.yandex.yandexnavi");
        if (!anyInstalled) {
            carAppStatusText.setText(getString(R.string.carapp_status_not_installed));
            return;
        }
        carAppStatusText.setText(LaunchHelper.hasCarApp(this)
                ? getString(R.string.carapp_status_available)
                : getString(R.string.carapp_status_not_found));
    }

    private void triggerCarApp() {
        if (!LaunchHelper.isPackageInstalled(this, "ru.dublgis.dgismobile")
                && !LaunchHelper.isPackageInstalled(this, "ru.yandex.yandexnavi")) {
            showToast("Навигация не установлена");
            return;
        }
        boolean sent = LaunchHelper.startCarApp(this);
        if (sent) {
            showToast("CarApp trigger отправлен");
            carAppStatusText.setText(getString(R.string.carapp_trigger_sent));
        } else {
            showToast("Не удалось отправить CarApp trigger");
        }
    }

    @Nullable
    private String buildBindingSummary(AppTarget target) {
        if (target == null || target.packageName == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (DisplayTarget displayTarget : displayTargets) {
            DisplayBinding binding = loadDisplayBinding(displayTarget.displayId);
            if (binding == null) {
                continue;
            }
            if (!target.packageName.equals(binding.packageName)) {
                continue;
            }
            if (!isEmpty(builder)) {
                builder.append(", ");
            }
            builder.append(displayTarget.label);
        }
        return isEmpty(builder) ? null : builder.toString();
    }

    private void saveDisplayBinding(int displayId, AppTarget target, @Nullable String aliasClass) {
        getSharedPreferences("display_bindings", MODE_PRIVATE)
                .edit()
                .putString(displayId + ".package", target.packageName)
                .putString(displayId + ".activity", target.explicitActivity)
                .putString(displayId + ".title", target.title)
                .putString(displayId + ".alias", aliasClass)
                .apply();
    }

    private void clearDisplayBinding(int displayId) {
        getSharedPreferences("display_bindings", MODE_PRIVATE)
                .edit()
                .remove(displayId + ".package")
                .remove(displayId + ".activity")
                .remove(displayId + ".title")
                .remove(displayId + ".alias")
                .apply();
    }

    @Nullable
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
        String aliasClass = getSharedPreferences("display_bindings", MODE_PRIVATE)
                .getString(displayId + ".alias", null);
        return new DisplayBinding(title, pkg, activity, aliasClass);
    }

private void saveSelectedLauncherTarget(SupportedAliasCatalog.SupportedAliasApp supported, AppTarget target) {
    getSharedPreferences("launcher_targets", MODE_PRIVATE)
            .edit()
            .putString(supported.groupKey + ".package", target.packageName)
            .putString(supported.groupKey + ".activity", target.explicitActivity)
            .putString(supported.groupKey + ".title", target.title)
            .apply();
}

private int setComponentsEnabled(String[] classNames, int newState) {
    PackageManager pm = getPackageManager();
    int changed = 0;
    for (String className : classNames) {
        try {
            ComponentName componentName = new ComponentName(this, className);
            if (pm.getComponentEnabledSetting(componentName) != newState) {
                pm.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP);
            }
            changed++;
        } catch (Exception ignored) {
        }
    }
    return changed;
}


    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    private String readVersionName() {
        try {
            return getPackageManager()
                    .getPackageInfo(getPackageName(), 0)
                    .versionName;
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    private String safeMessage(@Nullable String message) {
        return message == null || message.trim().isEmpty() ? "Без подробностей" : message;
    }

    private void openCommunityLink() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/GeelyGalaxyClub"));
            startActivity(intent);
        } catch (Exception e) {
            showToast("Не удалось открыть ссылку");
        }
    }

    private boolean shouldSwapAppRow() {
        return getSharedPreferences("ui_prefs", MODE_PRIVATE)
                .getBoolean("swap_app_row", false);
    }

    @SuppressWarnings("SpellCheckingInspection")
    private SectionType classifyApp(AppTarget target) {
        String pkg = target.packageName == null ? "" : target.packageName.toLowerCase(Locale.ROOT);
        String title = target.title == null ? "" : target.title.toLowerCase(Locale.ROOT);
        String combined = pkg + " " + title;
        if (combined.contains("nav")
                || combined.contains("map")
                || combined.contains("maps")
                || combined.contains("2gis")
                || combined.contains("дубльгис")
                || combined.contains("яндекс")
                || combined.contains("нави")) {
            return SectionType.NAVIGATION;
        }
        if (combined.contains("music")
                || combined.contains("муз")
                || combined.contains("video")
                || combined.contains("youtube")
                || combined.contains("rutube")
                || combined.contains("кино")
                || combined.contains("ivi")
                || combined.contains("okko")
                || combined.contains("tv")
                || combined.contains("радио")
                || combined.contains("vk")
                || combined.contains("ximalaya")
                || combined.contains("kugou")
                || combined.contains("tencent")) {
            return SectionType.MEDIA;
        }
        return SectionType.OTHER;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static boolean isEmpty(StringBuilder builder) {
        return builder.length() == 0;
    }

    private String displayLabel(int displayId) {
        for (DisplayTarget target : displayTargets) {
            if (target.displayId == displayId) {
                return target.label;
            }
        }
        return null;
    }

    private enum SectionType {
        NAVIGATION,
        MEDIA,
        OTHER
    }
    private record DisplayBinding(String title, String packageName, @Nullable String activityName,
                                  @Nullable String aliasClass) {
    }

    private record DisplayTarget(String label, int displayId) {
    }

    private String displayIndex(int displayId) {
        if (displayId == 1001) {
            return "1";
        }
        if (displayId == 1002) {
            return "2";
        }
        if (displayId == 1003) {
            return "3";
        }
        if (displayId == 3) {
            return "4";
        }
        return String.valueOf(displayId);
    }
}
