package com.oai.displaylauncher;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class ShortcutPinReceiver extends BroadcastReceiver {
    public static final String EXTRA_SHORTCUT_INDEX = "extra_shortcut_index";

    private static final String[] LABELS = {
            "Главный",
            "Пассажирский",
            "Весь экран",
            "Потолочный"
    };

    private static final int[] DISPLAY_IDS = {
            1001,
            1002,
            1003,
            3
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        int currentIndex = intent.getIntExtra(EXTRA_SHORTCUT_INDEX, -1);
        String packageName = intent.getStringExtra(LaunchHelper.EXTRA_PACKAGE_NAME);
        String explicitActivity = intent.getStringExtra(LaunchHelper.EXTRA_EXPLICIT_ACTIVITY);
        String appTitle = intent.getStringExtra(LaunchHelper.EXTRA_APP_TITLE);

        if (packageName == null || packageName.trim().isEmpty()) {
            return;
        }

        int nextIndex = currentIndex + 1;
        if (nextIndex >= DISPLAY_IDS.length) {
            Toast.makeText(context, "Все ярлыки добавлены", Toast.LENGTH_SHORT).show();
            return;
        }

        requestPinnedShortcut(context, packageName, explicitActivity, appTitle, nextIndex);
    }

    public static boolean createShortcuts(Context context,
                                          String packageName,
                                          @Nullable String explicitActivity,
                                          @Nullable String appTitle) {
        installLegacyShortcuts(context, packageName, explicitActivity, appTitle);
        Toast.makeText(context, "Ярлыки отправлены в лаунчер", Toast.LENGTH_LONG).show();
        return true;
    }

    public static boolean requestPinnedShortcut(Context context,
                                                String packageName,
                                                @Nullable String explicitActivity,
                                                @Nullable String appTitle,
                                                int index) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(context, "Ярлыки поддерживаются с Android 8.0", Toast.LENGTH_SHORT).show();
            return false;
        }

        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        if (shortcutManager == null || !shortcutManager.isRequestPinShortcutSupported()) {
            Toast.makeText(context, "Лаунчер не поддерживает закрепление ярлыков", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!LaunchHelper.isPackageInstalled(context, packageName)) {
            Toast.makeText(context, "Приложение не найдено", Toast.LENGTH_SHORT).show();
            return false;
        }

        String safeTitle = appTitle == null || appTitle.trim().isEmpty() ? packageName : appTitle;
        String label = LABELS[index];
        int displayId = DISPLAY_IDS[index];

        ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(context, packageName + ":" + displayId)
                .setShortLabel(label)
                .setLongLabel(safeTitle + " — " + label)
                .setIcon(Icon.createWithBitmap(drawableToBitmap(context, loadAppIcon(context, packageName))))
                .setIntent(buildShortcutIntent(context, packageName, explicitActivity, safeTitle, label, displayId))
                .build();

        Intent callbackIntent = new Intent(context, ShortcutPinReceiver.class)
                .putExtra(LaunchHelper.EXTRA_PACKAGE_NAME, packageName)
                .putExtra(LaunchHelper.EXTRA_EXPLICIT_ACTIVITY, explicitActivity)
                .putExtra(LaunchHelper.EXTRA_APP_TITLE, safeTitle)
                .putExtra(EXTRA_SHORTCUT_INDEX, index);

        PendingIntent callback = PendingIntent.getBroadcast(
                context,
                displayId,
                callbackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return shortcutManager.requestPinShortcut(shortcutInfo, callback.getIntentSender());
    }

    private static boolean shouldUseLegacyShortcutInstall(Context context) {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(home, 0);
            String launcherPackage = resolveInfo == null || resolveInfo.activityInfo == null
                    ? "" : resolveInfo.activityInfo.packageName;
            String value = launcherPackage == null ? "" : launcherPackage.toLowerCase();
            return value.contains("flyme") || value.contains("geely") || value.contains("ecarx");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void installLegacyShortcuts(Context context,
                                               String packageName,
                                               @Nullable String explicitActivity,
                                               @Nullable String appTitle) {
        String safeTitle = appTitle == null || appTitle.trim().isEmpty() ? packageName : appTitle;
        for (int i = 0; i < DISPLAY_IDS.length; i++) {
            String label = LABELS[i];
            int displayId = DISPLAY_IDS[i];
            Intent shortcutIntent = buildShortcutIntent(context, packageName, explicitActivity, safeTitle, label, displayId);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Intent install = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
            install.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            install.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);
            install.putExtra("duplicate", false);
            install.putExtra(Intent.EXTRA_SHORTCUT_ICON, drawableToBitmap(context, loadAppIcon(context, packageName)));
            context.sendBroadcast(install);
        }
    }

    private static Intent buildShortcutIntent(Context context,
                                              String packageName,
                                              @Nullable String explicitActivity,
                                              String appTitle,
                                              String screenLabel,
                                              int displayId) {
        Intent intent = new Intent(context, ShortcutProxyActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(LaunchHelper.EXTRA_PACKAGE_NAME, packageName);
        intent.putExtra(LaunchHelper.EXTRA_EXPLICIT_ACTIVITY, explicitActivity);
        intent.putExtra(LaunchHelper.EXTRA_DISPLAY_ID, displayId);
        intent.putExtra(LaunchHelper.EXTRA_SCREEN_LABEL, screenLabel);
        intent.putExtra(LaunchHelper.EXTRA_APP_TITLE, appTitle);
        return intent;
    }

    private static Drawable loadAppIcon(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getApplicationIcon(packageName);
        } catch (Exception ignored) {
            return ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground);
        }
    }

    private static Bitmap drawableToBitmap(Context context, @Nullable Drawable drawable) {
        Drawable safeDrawable = drawable;
        if (safeDrawable == null) {
            safeDrawable = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground);
        }
        int size = (int) (56 * context.getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        if (safeDrawable != null) {
            safeDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            safeDrawable.draw(canvas);
        }
        return bitmap;
    }
}
