package com.clickfreebackup.clickfree;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class ToolbarView extends LinearLayout {
    private TextView doneText, appNameText;
    private View toolbar;

    public ToolbarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.setOrientation(HORIZONTAL);
        init(context);
    }

    public ToolbarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.setOrientation(HORIZONTAL);
        init(context);
    }

    public ToolbarView(Context context) {
        super(context);
        this.setOrientation(HORIZONTAL);
        init(context);
    }

    private void init(Context context) {

        inflate(context, R.layout.toolbar_layout, this);

        doneText = findViewById(R.id.done_text);
        appNameText = findViewById(R.id.app_name_text);
        toolbar = findViewById(R.id.toolbar);

    }
}
