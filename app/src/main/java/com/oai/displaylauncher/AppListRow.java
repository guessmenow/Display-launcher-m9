package com.oai.displaylauncher;

import androidx.annotation.Nullable;

public class AppListRow {
    enum RowType {
        HEADER,
        APP
    }

    final RowType type;
    final String title;
    @Nullable
    final AppTarget app;
    final boolean selected;
    final boolean shortcutsSupported;

    private AppListRow(RowType type, String title, @Nullable AppTarget app, boolean selected, boolean shortcutsSupported) {
        this.type = type;
        this.title = title;
        this.app = app;
        this.selected = selected;
        this.shortcutsSupported = shortcutsSupported;
    }

    static AppListRow header(String title) {
        return new AppListRow(RowType.HEADER, title, null, false, false);
    }

    static AppListRow app(AppTarget app, boolean selected, boolean shortcutsSupported) {
        return new AppListRow(RowType.APP, app.title, app, selected, shortcutsSupported);
    }
}
