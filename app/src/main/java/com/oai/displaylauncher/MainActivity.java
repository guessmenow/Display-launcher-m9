package com.oai.displaylauncher;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private final List<AppTarget> appTargets = new ArrayList<>();
    private final List<DisplayTarget> displayTargets = new ArrayList<>();
    private final Map<Integer, MaterialCardView> displayCards = new HashMap<>();

    @Nullable
    private AppTarget selectedTarget;

    private ImageView selectedAppIcon;
    private TextView selectedAppTitle;
    private TextView selectedAppSubtitle;
    private TextView selectedAppCategory;
    private MaterialCardView appSelectorCard;
    private View createShortcutsButton;
    private View hideShortcutsButton;
    private View hideSelectedShortcutsButton;
    private View carAppCard;
    private TextView carAppStatusText;
    private TextView diagnosticsText;
    private View quickLaunchCard;
    private TextView quickLaunchTitle;
    private TextView quickLaunchSubtitle;
    private View quickPassengerButton;
    private View quickCeilingButton;
    private View quickResetButton;

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
        displayTargets.add(new DisplayTarget("Главный", 1001));
        displayTargets.add(new DisplayTarget("Пассажирский", 1002));
        displayTargets.add(new DisplayTarget("Весь экран", 1003));
        displayTargets.add(new DisplayTarget("Потолочный", 3));
    }

    private void bindViews() {
        selectedAppIcon = findViewById(R.id.selectedAppIcon);
        selectedAppTitle = findViewById(R.id.selectedAppTitle);
        selectedAppSubtitle = findViewById(R.id.selectedAppSubtitle);
        selectedAppCategory = findViewById(R.id.selectedAppCategory);
        appSelectorCard = findViewById(R.id.appSelectorCard);
        createShortcutsButton = findViewById(R.id.createShortcutsButton);
        hideShortcutsButton = findViewById(R.id.hideShortcutsButton);
        hideSelectedShortcutsButton = findViewById(R.id.hideSelectedShortcutsButton);
        carAppCard = findViewById(R.id.carAppCard);
        carAppStatusText = findViewById(R.id.carAppStatusText);
        diagnosticsText = findViewById(R.id.diagnosticsText);
        quickLaunchCard = findViewById(R.id.quickLaunchCard);
        quickLaunchTitle = findViewById(R.id.quickLaunchTitle);
        quickLaunchSubtitle = findViewById(R.id.quickLaunchSubtitle);
        quickPassengerButton = findViewById(R.id.quickPassengerButton);
        quickCeilingButton = findViewById(R.id.quickCeilingButton);
        quickResetButton = findViewById(R.id.quickResetButton);
        appsRecycler = findViewById(R.id.appsRecycler);

        appListAdapter = new AppListAdapter(this, target -> {
            selectedTarget = target;
            updateSelectedAppCard();
            updateQuickLaunchState();
            rebuildAppList();
        });
        appsRecycler.setLayoutManager(new LinearLayoutManager(this));
        appsRecycler.setAdapter(appListAdapter);
    }

    private void setupClickListeners() {
        appSelectorCard.setOnClickListener(v -> showAppPicker());
        createShortcutsButton.setOnClickListener(this::createPinnedShortcuts);
        hideShortcutsButton.setOnClickListener(this::hideAllAliasShortcuts);
        hideSelectedShortcutsButton.setOnClickListener(this::hideSelectedAliasShortcuts);
        carAppCard.setOnClickListener(v -> triggerTwoGisCarApp());
        findViewById(R.id.carAppTriggerButton).setOnClickListener(v -> triggerTwoGisCarApp());
        findViewById(R.id.diagnosticsRefreshButton).setOnClickListener(v -> updateDiagnosticsPanel());
        quickPassengerButton.setOnClickListener(v -> runQuickLaunch(1002, "пассажирском экране"));
        quickCeilingButton.setOnClickListener(v -> runQuickLaunch(3, "потолочном экране"));
        quickResetButton.setOnClickListener(v -> runQuickReset());

        bindLaunchCard(R.id.cardMain, 1001, "главном экране");
        bindLaunchCard(R.id.cardPassenger, 1002, "пассажирском экране");
        bindLaunchCard(R.id.cardFull, 1003, "весь экран");
        bindLaunchCard(R.id.cardCeiling, 3, "потолочном экране");
    }

    private void bindLaunchCard(int viewId, int displayId, String label) {
        MaterialCardView card = findViewById(viewId);
        displayCards.put(displayId, card);
        card.setOnClickListener(v -> launchOnDisplay(displayId, label));
        card.setOnLongClickListener(v -> {
            handleDisplayBinding(displayId, label);
            return true;
        });
    }

    private void updateLoadingState(boolean loading) {
        appSelectorCard.setEnabled(!loading);
        createShortcutsButton.setEnabled(!loading);
        hideShortcutsButton.setEnabled(!loading);
        hideSelectedShortcutsButton.setEnabled(!loading);
        carAppCard.setEnabled(!loading);
        appsRecycler.setVisibility(loading ? View.GONE : View.VISIBLE);
        selectedAppTitle.setText(loading ? "Загрузка приложений…" : "Выберите приложение");
        selectedAppSubtitle.setText(loading ? "Собираем список пользовательских приложений" : "Нажмите, чтобы выбрать");
        selectedAppCategory.setText(loading ? "Категория: загрузка…" : "Категория: —");
        selectedAppIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_launcher_foreground));
    }

    private void loadInstalledTargetsAsync() {
        new Thread(() -> {
            try {
                List<AppTarget> loaded = loadLaunchableUserApps();
                runOnUiThread(() -> applyLoadedTargets(loaded));
            } catch (Throwable t) {
                runOnUiThread(() -> {
                    updateLoadingState(false);
                    selectedTarget = null;
                    selectedAppTitle.setText("Ошибка загрузки");
                    selectedAppSubtitle.setText(safeMessage(t.getMessage()));
                });
            }
        }).start();
    }

    private List<AppTarget> loadLaunchableUserApps() {
        List<AppTarget> result = new ArrayList<>();
        PackageManager pm = getPackageManager();

        Intent queryIntent = new Intent(Intent.ACTION_MAIN);
        queryIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> activities = new ArrayList<>(pm.queryIntentActivities(queryIntent, 0));

        Set<String> seenPackages = new HashSet<>();
        Collator collator = Collator.getInstance(new Locale("ru"));
        activities.sort(Comparator.comparing(info -> safeLabel(info, pm), collator));

        for (ResolveInfo info : activities) {
            if (info.activityInfo == null || info.activityInfo.applicationInfo == null) {
                continue;
            }

            ApplicationInfo applicationInfo = info.activityInfo.applicationInfo;
            if (isSystemApp(applicationInfo)) {
                continue;
            }

            String packageName = info.activityInfo.packageName;
            if (packageName == null || packageName.trim().isEmpty()) {
                continue;
            }
            if (getPackageName().equals(packageName) || seenPackages.contains(packageName)) {
                continue;
            }
            seenPackages.add(packageName);

            String title = safeLabel(info, pm);
            String activityName = normalizeActivityName(packageName, info.activityInfo.name);
            if ("ru.yandex.yandexnavi".equals(packageName)) {
                activityName = "ru.yandex.yandexnavi.core.NavigatorActivity";
            } else if ("ru.dublgis.dgismobile".equals(packageName)) {
                activityName = "ru.dublgis.dgismobile.GrymMobileActivity";
            }

            result.add(new AppTarget(
                    title,
                    packageName,
                    packageName,
                    activityName,
                    R.drawable.ic_launcher_foreground
            ));
        }
        return result;
    }

    private void applyLoadedTargets(List<AppTarget> loaded) {
        appTargets.clear();
        appTargets.addAll(loaded);

        if (appTargets.isEmpty()) {
            updateLoadingState(false);
            selectedTarget = null;
            selectedAppTitle.setText("Приложения не найдены");
            selectedAppSubtitle.setText("Нет сторонних приложений с ярлыком запуска");
            appsRecycler.setVisibility(View.GONE);
            return;
        }

        selectedTarget = findPreferredTarget();
        updateLoadingState(false);
        appSelectorCard.setEnabled(true);
        createShortcutsButton.setEnabled(true);
        hideShortcutsButton.setEnabled(true);
        hideSelectedShortcutsButton.setEnabled(true);
        updateSelectedAppCard();
        updateCarAppStatus();
        refreshDisplayBindings();
        updateDiagnosticsPanel();
        updateQuickLaunchState();
        rebuildAppList();
    }

    @Nullable
    private AppTarget findPreferredTarget() {
        for (AppTarget target : appTargets) {
            if ("ru.yandex.yandexnavi".equals(target.packageName)) {
                return target;
            }
        }
        for (AppTarget target : appTargets) {
            if ("ru.yandex.yandexmaps".equals(target.packageName)) {
                return target;
            }
        }
        return appTargets.isEmpty() ? null : appTargets.get(0);
    }

    private void showAppPicker() {
        if (appTargets.isEmpty()) {
            showToast("Список приложений ещё не готов");
            return;
        }

        AppChoiceAdapter adapter = new AppChoiceAdapter(this, appTargets);
        new AlertDialog.Builder(this)
                .setTitle("Выберите приложение")
                .setAdapter(adapter, (dialog, which) -> {
                    selectedTarget = adapter.getItem(which);
                    updateSelectedAppCard();
                    dialog.dismiss();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void updateSelectedAppCard() {
        if (selectedTarget == null) {
            selectedAppIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_launcher_foreground));
            selectedAppTitle.setText("Выберите приложение");
            selectedAppSubtitle.setText("Нажмите, чтобы открыть список");
            selectedAppCategory.setText("Категория: —");
            appSelectorCard.setStrokeColor(getColor(R.color.card_stroke));
            return;
        }

        selectedAppIcon.setImageDrawable(selectedTarget.loadIcon(this));
        selectedAppTitle.setText(selectedTarget.title);
        selectedAppSubtitle.setText(selectedTarget.packageName);
        selectedAppCategory.setText("Категория: " + sectionLabel(classifyApp(selectedTarget)));
        appSelectorCard.setStrokeColor(getColor(R.color.dl_primary));
    }

    private void updateQuickLaunchState() {
        if (selectedTarget == null) {
            quickLaunchTitle.setText("Быстрые действия");
            quickLaunchSubtitle.setText("Выберите приложение в списке");
            setQuickButtonsEnabled(false);
            return;
        }
        quickLaunchTitle.setText("Быстрые действия: " + selectedTarget.title);
        quickLaunchSubtitle.setText("Пассажир / Потолок / Сброс → Главный");
        setQuickButtonsEnabled(true);
    }

    private void setQuickButtonsEnabled(boolean enabled) {
        quickPassengerButton.setEnabled(enabled);
        quickCeilingButton.setEnabled(enabled);
        quickResetButton.setEnabled(enabled);
    }

    private void runQuickLaunch(int displayId, String label) {
        if (selectedTarget == null) {
            showToast("Сначала выберите приложение");
            return;
        }
        launchTargetOnDisplay(selectedTarget, displayId, label);
    }

    private void runQuickReset() {
        if (selectedTarget == null) {
            showToast("Сначала выберите приложение");
            return;
        }
        confirmResetToMain(selectedTarget);
    }

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
            rows.add(AppListRow.app(target, selected, supported));
        }
    }

    private void launchOnDisplay(int displayId, String label) {
        AppTarget target = selectedTarget;
        if (target == null) {
            DisplayBinding binding = loadDisplayBinding(displayId);
            if (binding != null) {
                target = new AppTarget(binding.title, binding.packageName, binding.packageName,
                        binding.activityName, R.drawable.ic_launcher_foreground);
            }
        }

        if (target == null) {
            showToast("Сначала выберите приложение");
            return;
        }

        launchTargetOnDisplay(target, displayId, label);
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
                .setTitle("Не удалось открыть на " + label)
                .setMessage(message)
                .setPositiveButton("Понятно", null)
                .show();
        }
    }


    private void createPinnedShortcuts(View anchor) {
        if (selectedTarget == null) {
            showToast("Сначала выберите приложение");
            return;
        }

    SupportedAliasCatalog.SupportedAliasApp supported = SupportedAliasCatalog.findForTarget(selectedTarget);
    if (supported == null) {
        showToast("Для этого приложения ярлыки пока не поддерживаются");
        return;
    }

    saveSelectedLauncherTarget(supported, selectedTarget);

    int enabled = setComponentsEnabled(supported.aliasClassNames,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        if (enabled > 0) {
            showToast("Показаны ярлыки: " + supported.appTitle + ". Лаунчер может обновиться не сразу");
            updateCarAppStatus();
            rebuildAppList();
        } else {
            showToast("Не удалось показать ярлыки");
        }
    }

@Nullable
private SupportedAliasCatalog.SupportedAliasApp findSupportedAliasApp() {
    if (selectedTarget == null) {
        return null;
    }
    return SupportedAliasCatalog.findForTarget(selectedTarget);
}


    private void hideAllAliasShortcuts(View anchor) {
        int disabled = setComponentsEnabled(
                SupportedAliasCatalog.getAllAliasClassNames(),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        );
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

    private void updateCarAppStatus() {
        boolean installed = LaunchHelper.isPackageInstalled(this, "ru.dublgis.dgismobile");
        boolean serviceDeclared = LaunchHelper.isServiceDeclared(this,
                "ru.dublgis.dgismobile",
                "ru.dublgis.car.CarService");
        if (!installed) {
            carAppStatusText.setText("Статус: 2ГИС не установлен");
            return;
        }
        carAppStatusText.setText(serviceDeclared
                ? "Статус: CarApp доступен"
                : "Статус: CarApp не обнаружен");
    }

    private void triggerTwoGisCarApp() {
        if (!LaunchHelper.isPackageInstalled(this, "ru.dublgis.dgismobile")) {
            showToast("2ГИС не установлен");
            return;
        }
        boolean sent = LaunchHelper.startTwoGisCarApp(this);
        if (sent) {
            showToast("CarApp trigger отправлен");
            carAppStatusText.setText("Статус: CarApp trigger отправлен");
        } else {
            showToast("Не удалось отправить CarApp trigger");
        }
    }

    private void handleDisplayBinding(int displayId, String label) {
        if (selectedTarget != null) {
            saveDisplayBinding(displayId, selectedTarget);
            refreshDisplayBindings();
            updateDiagnosticsPanel();
            showToast("Закреплено для " + label + ": " + selectedTarget.title);
            return;
        }
        DisplayBinding binding = loadDisplayBinding(displayId);
        if (binding != null) {
            clearDisplayBinding(displayId);
            refreshDisplayBindings();
            updateDiagnosticsPanel();
            showToast("Закрепление удалено для " + label);
        } else {
            showToast("Выберите приложение, чтобы закрепить");
        }
    }

    private void refreshDisplayBindings() {
        for (DisplayTarget target : displayTargets) {
            updateDisplayCardSubtitle(target.displayId, target.label);
        }
    }

    private void confirmResetToMain(AppTarget target) {
        new AlertDialog.Builder(this)
                .setTitle("Вернуть на главный экран")
                .setMessage("Отправить приложение на водительский экран?\n" + target.title)
                .setPositiveButton("Да", (dialog, which) -> launchTargetOnDisplay(target, 1001, "главном экране"))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void updateDiagnosticsPanel() {
        StringBuilder builder = new StringBuilder();
        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        Display[] displays = displayManager == null ? new Display[0] : displayManager.getDisplays();
        builder.append("Доступные дисплеи: ");
        if (displays.length == 0) {
            builder.append("не найдены");
        } else {
            for (int i = 0; i < displays.length; i++) {
                Display display = displays[i];
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

        builder.append("\nЗакрепления: ");
        boolean hasBindings = false;
        for (DisplayTarget target : displayTargets) {
            DisplayBinding binding = loadDisplayBinding(target.displayId);
            if (binding == null) {
                continue;
            }
            if (!hasBindings) {
                builder.append("\n");
            }
            hasBindings = true;
            builder.append("- ")
                    .append(target.label)
                    .append(" (Display ")
                    .append(target.displayId)
                    .append("): ")
                    .append(binding.title)
                    .append("\n");
        }
        if (!hasBindings) {
            builder.append("нет");
        }

        diagnosticsText.setText(builder.toString().trim());
    }

    private void updateDisplayCardSubtitle(int displayId, String label) {
        MaterialCardView card = displayCards.get(displayId);
        if (card == null) {
            return;
        }
        TextView subtitle = card.findViewById(R.id.launchCardSubtitle);
        if (subtitle == null) {
            return;
        }
        String base = "Display " + displayId + " · " + label;
        DisplayBinding binding = loadDisplayBinding(displayId);
        if (binding != null) {
            subtitle.setText(base + " · Закреплено: " + binding.title);
        } else {
            subtitle.setText(base);
        }
    }

    private void saveDisplayBinding(int displayId, AppTarget target) {
        getSharedPreferences("display_bindings", MODE_PRIVATE)
                .edit()
                .putString(displayId + ".package", target.packageName)
                .putString(displayId + ".activity", target.explicitActivity)
                .putString(displayId + ".title", target.title)
                .apply();
    }

    private void clearDisplayBinding(int displayId) {
        getSharedPreferences("display_bindings", MODE_PRIVATE)
                .edit()
                .remove(displayId + ".package")
                .remove(displayId + ".activity")
                .remove(displayId + ".title")
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
        return new DisplayBinding(title, pkg, activity);
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

    private boolean isSystemApp(ApplicationInfo applicationInfo) {
        int flags = applicationInfo.flags;
        return (flags & ApplicationInfo.FLAG_SYSTEM) != 0
                || (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
    }

    private String normalizeActivityName(String packageName, @Nullable String activityName) {
        if (activityName == null || activityName.trim().isEmpty()) {
            return packageName;
        }
        if (activityName.startsWith(".")) {
            return packageName + activityName;
        }
        return activityName;
    }

    private String safeLabel(ResolveInfo info, PackageManager pm) {
        CharSequence label = info.loadLabel(pm);
        String value = label == null ? null : label.toString().trim();
        if (value == null || value.isEmpty()) {
            return info.activityInfo == null ? "Без названия" : info.activityInfo.packageName;
        }
        return value;
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    private String safeMessage(@Nullable String message) {
        return message == null || message.trim().isEmpty() ? "Без подробностей" : message;
    }

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
                || combined.contains("кино")
                || combined.contains("tv")
                || combined.contains("радио")) {
            return SectionType.MEDIA;
        }
        return SectionType.OTHER;
    }

    private enum SectionType {
        NAVIGATION,
        MEDIA,
        OTHER
    }

    private String sectionLabel(SectionType type) {
        switch (type) {
            case NAVIGATION:
                return "Навигация";
            case MEDIA:
                return "Медиа";
            default:
                return "Прочее";
        }
    }

    private static class DisplayBinding {
        final String title;
        final String packageName;
        @Nullable
        final String activityName;

        DisplayBinding(String title, String packageName, @Nullable String activityName) {
            this.title = title;
            this.packageName = packageName;
            this.activityName = activityName;
        }
    }


    private static class DisplayTarget {
        final String label;
        final int displayId;

        DisplayTarget(String label, int displayId) {
            this.label = label;
            this.displayId = displayId;
        }
    }
}
