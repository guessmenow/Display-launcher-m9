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
    @Nullable
    final String bindingSummary;

    private AppListRow(RowType type,
                       String title,
                       @Nullable AppTarget app,
                       boolean selected,
                       boolean shortcutsSupported,
                       @Nullable String bindingSummary) {
        this.type = type;
        this.title = title;
        this.app = app;
        this.selected = selected;
        this.shortcutsSupported = shortcutsSupported;
        this.bindingSummary = bindingSummary;
    }

    static AppListRow header(String title) {
        return new AppListRow(RowType.HEADER, title, null, false, false, null);
    }

    static AppListRow app(AppTarget app,
                          boolean selected,
                          boolean shortcutsSupported,
                          @Nullable String bindingSummary) {
        return new AppListRow(RowType.APP, app.title, app, selected, shortcutsSupported, bindingSummary);
    }
}
