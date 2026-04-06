package com.oai.displaylauncher;

import android.content.Context;
import android.content.pm.PackageManager;

public final class ShortcutModeManager {
    private static final String PREFS = "ui_prefs";
    private static final String KEY_DISTINCT = "distinct_shortcuts";
    private static final String KEY_DISTINCT_SET = "distinct_shortcuts_user_set";

    private ShortcutModeManager() {
    }

    public static boolean isDistinctModeEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_DISTINCT, false);
    }

    public static boolean isDistinctModeUserSet(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_DISTINCT_SET, false);
    }

    public static void setDistinctMode(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_DISTINCT, enabled)
                .putBoolean(KEY_DISTINCT_SET, true)
                .apply();
        if (enabled) {
            disableAllAliases(context);
        }
    }

    private static void disableAllAliases(Context context) {
        PackageManager pm = context.getPackageManager();
        for (String className : SupportedAliasCatalog.getAllAliasClassNames()) {
            try {
                pm.setComponentEnabledSetting(
                        new android.content.ComponentName(context, className),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                );
            } catch (Exception ignored) {
            }
        }
    }
}
