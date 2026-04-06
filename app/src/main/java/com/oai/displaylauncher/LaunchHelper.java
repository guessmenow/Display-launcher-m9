package com.oai.displaylauncher;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import androidx.annotation.Nullable;

import java.util.List;

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

    public static boolean startCarApp(Activity activity) {
        boolean started = false;
        started |= startCarAppForPackage(activity,
                "ru.dublgis.dgismobile",
                "ru.dublgis.car.CarService");
        started |= startCarAppForPackage(activity,
                "ru.yandex.yandexnavi",
                null);
        return started;
    }

    public static boolean hasCarApp(Context context) {
        return hasCarAppForPackage(context, "ru.dublgis.dgismobile", "ru.dublgis.car.CarService")
                || hasCarAppForPackage(context, "ru.yandex.yandexnavi", null);
    }

    private static boolean hasCarAppForPackage(Context context, String packageName, @Nullable String explicitService) {
        if (!isPackageInstalled(context, packageName)) {
            return false;
        }
        if (explicitService != null && isServiceDeclared(context, packageName, explicitService)) {
            return true;
        }
        return findCarAppService(context, packageName) != null;
    }

    private static boolean startCarAppForPackage(Activity activity,
                                                 String packageName,
                                                 @Nullable String explicitService) {
        if (!isPackageInstalled(activity, packageName)) {
            return false;
        }
        ComponentName component = null;
        if (explicitService != null && isServiceDeclared(activity, packageName, explicitService)) {
            component = new ComponentName(packageName, explicitService);
        } else {
            component = findCarAppService(activity, packageName);
        }
        if (component == null) {
            return false;
        }
        try {
            Intent intent = new Intent();
            intent.setComponent(component);
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

    @Nullable
    private static ComponentName findCarAppService(Context context, String packageName) {
        Intent intent = new Intent("androidx.car.app.CarAppService");
        intent.addCategory("androidx.car.app.category.NAVIGATION");
        intent.setPackage(packageName);
        List<ResolveInfo> services = context.getPackageManager().queryIntentServices(intent, 0);
        if (services == null || services.isEmpty()) {
            return null;
        }
        ResolveInfo service = services.get(0);
        if (service.serviceInfo == null) {
            return null;
        }
        return new ComponentName(service.serviceInfo.packageName, service.serviceInfo.name);
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
