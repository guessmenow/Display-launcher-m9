
package com.oai.displaylauncher;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AliasLaunchActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AliasConfig config = readConfig();
        if (config == null) {
            Toast.makeText(this, "Не удалось прочитать ярлык", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!LaunchHelper.isPackageInstalled(this, config.packageName)) {
            Toast.makeText(this, "Приложение не найдено: " + config.appTitle, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
            LaunchHelper.launch(this, config.packageName, config.explicitActivity, config.displayId);
        } catch (Exception e) {
            String message = e.getClass().getSimpleName() + ": " + safeMessage(e.getMessage());
            Toast.makeText(this, "Не удалось открыть на " + config.screenLabel + "\n" + message, Toast.LENGTH_LONG).show();
        }
        finish();
    }

    @Nullable
    private AliasConfig readConfig() {
        try {
            ActivityInfo info = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
            if (info.metaData == null) {
                return null;
            }
            String packageName = info.metaData.getString("target_package");
            String explicitActivity = info.metaData.getString("target_activity");
            String appTitle = info.metaData.getString("target_app_title");
            String screenLabel = info.metaData.getString("target_screen_label");
            String groupKey = info.metaData.getString("target_group_key");
            int displayId = info.metaData.getInt("target_display_id", Integer.MIN_VALUE);
            if (packageName == null || packageName.trim().isEmpty() || displayId == Integer.MIN_VALUE) {
                return null;
            }

            if (groupKey != null && !groupKey.trim().isEmpty()) {
                android.content.SharedPreferences prefs = getSharedPreferences("launcher_targets", MODE_PRIVATE);
                String savedPackage = prefs.getString(groupKey + ".package", null);
                String savedActivity = prefs.getString(groupKey + ".activity", null);
                String savedTitle = prefs.getString(groupKey + ".title", null);
                if (savedPackage != null && !savedPackage.trim().isEmpty()) {
                    packageName = savedPackage;
                    explicitActivity = savedActivity;
                    if (savedTitle != null && !savedTitle.trim().isEmpty()) {
                        appTitle = savedTitle;
                    }
                }
            }

            return new AliasConfig(packageName, explicitActivity, appTitle == null ? packageName : appTitle,
                    screenLabel == null ? "экран" : screenLabel, displayId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String safeMessage(@Nullable String message) {
        return message == null || message.trim().isEmpty() ? "Без подробностей" : message;
    }

    private static class AliasConfig {
        final String packageName;
        final String explicitActivity;
        final String appTitle;
        final String screenLabel;
        final int displayId;

        AliasConfig(String packageName, String explicitActivity, String appTitle, String screenLabel, int displayId) {
            this.packageName = packageName;
            this.explicitActivity = explicitActivity;
            this.appTitle = appTitle;
            this.screenLabel = screenLabel;
            this.displayId = displayId;
        }
    }
}
