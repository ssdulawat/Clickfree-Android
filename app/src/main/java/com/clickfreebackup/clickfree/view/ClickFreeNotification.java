package com.clickfreebackup.clickfree.view;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.clickfreebackup.clickfree.R;

import static com.clickfreebackup.clickfree.view.first_run_fragment.EmailFragment.EMAIL_SENT_FAIL;
import static com.clickfreebackup.clickfree.view.first_run_fragment.EmailFragment.EMAIL_SENT_SUCCESSFULLY;

public class ClickFreeNotification extends DialogFragment {
    private ClickFreeNotificationListener mClickFreeNotificationListener;
    private String mDialogType;

    public ClickFreeNotification(ClickFreeNotificationListener clickFreeNotificationListener, String dialogType) {
        mClickFreeNotificationListener = clickFreeNotificationListener;
        mDialogType = dialogType;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setWindowNoTitle();
        final View view = inflater.inflate(R.layout.rate_us_layout, container, false);
        setUpWidgets(view);
        return view;
    }

    private void setWindowNoTitle() {
        final Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setUpWidgets(view);
    }

    private void setUpWidgets(View view) {

        final TextView title = view.findViewById(R.id.title_text);
        final TextView description = view.findViewById(R.id.description_text);
        final Button actionButton = view.findViewById(R.id.action_button);
        title.setText(getText(R.string.sending_title_text));

        if (mDialogType.equals(EMAIL_SENT_SUCCESSFULLY)) {
            description.setText(getText(R.string.sending_description_success_text));
            actionButton.setText(getText(R.string.ok));
        } else if (mDialogType.equals(EMAIL_SENT_FAIL)) {
            description.setText(getText(R.string.sending_description_fail_text));
            actionButton.setText(getText(R.string.ok));
        }

        actionButton.setOnClickListener(actionButtonView -> mClickFreeNotificationListener.onActionButtonClicked());
    }
}
