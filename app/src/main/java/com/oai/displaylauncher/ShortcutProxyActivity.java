package com.oai.displaylauncher;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ShortcutProxyActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String packageName = getIntent().getStringExtra(LaunchHelper.EXTRA_PACKAGE_NAME);
        String explicitActivity = getIntent().getStringExtra(LaunchHelper.EXTRA_EXPLICIT_ACTIVITY);
        int displayId = getIntent().getIntExtra(LaunchHelper.EXTRA_DISPLAY_ID, 0);
        String screenLabel = getIntent().getStringExtra(LaunchHelper.EXTRA_SCREEN_LABEL);

        if (packageName == null || packageName.trim().isEmpty()) {
            finish();
            return;
        }

        try {
            LaunchHelper.launch(this, packageName, explicitActivity, displayId);
        } catch (Exception e) {
            String message = e.getClass().getSimpleName();
            if (e.getMessage() != null && !e.getMessage().trim().isEmpty()) {
                message += ": " + e.getMessage();
            }
            Toast.makeText(this, "Не удалось открыть на " + (screenLabel == null ? "экране" : screenLabel) + "\n" + message, Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
