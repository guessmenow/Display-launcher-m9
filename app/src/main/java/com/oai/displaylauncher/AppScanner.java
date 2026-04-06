package com.oai.displaylauncher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.annotation.Nullable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AppScanner {
    private AppScanner() {
    }

    static List<AppTarget> loadLaunchableUserApps(Context context, @Nullable Set<String> allowedPackages) {
        List<AppTarget> result = new ArrayList<>();
        PackageManager pm = context.getPackageManager();

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
            if (packageName.equals(context.getPackageName())) {
                continue;
            }
            if (isVendorPackage(packageName)) {
                continue;
            }
            if (allowedPackages != null && !allowedPackages.isEmpty() && !allowedPackages.contains(packageName)) {
                continue;
            }
            if (seenPackages.contains(packageName)) {
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

    static Set<String> loadManagedPackages(Context context) {
        return context.getSharedPreferences("managed_apps", Context.MODE_PRIVATE)
                .getStringSet("packages", new HashSet<>());
    }

    static void saveManagedPackages(Context context, Set<String> packages) {
        context.getSharedPreferences("managed_apps", Context.MODE_PRIVATE)
                .edit()
                .putStringSet("packages", packages)
                .apply();
    }

    static boolean isSystemApp(ApplicationInfo applicationInfo) {
        int flags = applicationInfo.flags;
        return (flags & ApplicationInfo.FLAG_SYSTEM) != 0
                || (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
    }

    static boolean isVendorPackage(String packageName) {
        String pkg = packageName == null ? "" : packageName.toLowerCase(Locale.ROOT);
        return pkg.startsWith("com.geely")
                || pkg.startsWith("com.flyme")
                || pkg.startsWith("com.ecarx")
                || pkg.startsWith("com.ts.")
                || pkg.startsWith("com.qti")
                || pkg.startsWith("com.qualcomm")
                || pkg.startsWith("com.android.car")
                || pkg.startsWith("android")
                || pkg.startsWith("vendor.")
                || pkg.startsWith("com.huawei")
                || pkg.startsWith("com.aispeech")
                || pkg.startsWith("com.arcvideo")
                || pkg.startsWith("com.tencent.karaokecar")
                || pkg.startsWith("com.upuphone")
                || pkg.startsWith("com.junlian")
                || pkg.startsWith("com.kika")
                || pkg.startsWith("geely.");
    }

    private static String normalizeActivityName(String packageName, @Nullable String activityName) {
        if (activityName == null || activityName.trim().isEmpty()) {
            return packageName;
        }
        if (activityName.startsWith(".")) {
            return packageName + activityName;
        }
        return activityName;
    }

    private static String safeLabel(ResolveInfo info, PackageManager pm) {
        CharSequence label = info.loadLabel(pm);
        String value = label == null ? null : label.toString().trim();
        if (value == null || value.isEmpty()) {
            return info.activityInfo == null ? "Без названия" : info.activityInfo.packageName;
        }
        return value;
    }
}
