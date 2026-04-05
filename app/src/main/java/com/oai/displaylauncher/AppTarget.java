package com.oai.displaylauncher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

public class AppTarget {
    public final String title;
    public final String description;
    public final String packageName;
    public final String explicitActivity;
    public final int fallbackIconRes;

    public AppTarget(String title, String description, String packageName, String explicitActivity, int fallbackIconRes) {
        this.title = title;
        this.description = description;
        this.packageName = packageName;
        this.explicitActivity = explicitActivity;
        this.fallbackIconRes = fallbackIconRes;
    }

    public Drawable loadIcon(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getApplicationIcon(packageName);
        } catch (Exception ignored) {
            return ContextCompat.getDrawable(context, fallbackIconRes);
        }
    }

    public Intent buildLaunchIntent(Context context) {
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
}
