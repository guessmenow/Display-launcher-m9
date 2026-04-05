package com.oai.displaylauncher;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.Nullable;

public final class LaunchHelper {
    public static final String EXTRA_PACKAGE_NAME = "extra_package_name";
    public static final String EXTRA_EXPLICIT_ACTIVITY = "extra_explicit_activity";
    public static final String EXTRA_DISPLAY_ID = "extra_display_id";
    public static final String EXTRA_SCREEN_LABEL = "extra_screen_label";
    public static final String EXTRA_APP_TITLE = "extra_app_title";

    private LaunchHelper() {
    }

    @Nullable
    public static Intent buildLaunchIntent(Context context, String packageName, @Nullable String explicitActivity) {
        try {
            if (explicitActivity != null && !explicitActivity.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setComponent(new ComponentName(packageName, explicitActivity));
                return intent;
            }
            return context.getPackageManager().getLaunchIntentForPackage(packageName);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean isExplicitActivityValid(Context context, String packageName, @Nullable String explicitActivity) {
        if (explicitActivity == null || explicitActivity.trim().isEmpty()) {
            return true;
        }
        try {
            context.getPackageManager().getActivityInfo(new ComponentName(packageName, explicitActivity), 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isServiceDeclared(Context context, String packageName, String serviceClassName) {
        try {
            context.getPackageManager().getServiceInfo(new ComponentName(packageName, serviceClassName), 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean startTwoGisCarApp(Activity activity) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("ru.dublgis.dgismobile", "ru.dublgis.car.CarService"));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(intent);
            } else {
                activity.startService(intent);
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static void launch(Activity activity,
                              String packageName,
                              @Nullable String explicitActivity,
                              int displayId) {
        Intent intent = buildLaunchIntent(activity, packageName, explicitActivity);
        if (intent == null) {
            throw new IllegalStateException("Приложение не найдено");
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchDisplayId(displayId);
            activity.startActivity(intent, options.toBundle());
        } else {
            activity.startActivity(intent);
        }
    }

    public static boolean isPackageInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
