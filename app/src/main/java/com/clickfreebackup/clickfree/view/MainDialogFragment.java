package com.clickfreebackup.clickfree.view;

import android.app.Dialog;
import android.content.Context;
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
import com.clickfreebackup.clickfree.util.SenderUtil;

import static com.clickfreebackup.clickfree.MainActivity.CAMERA_REQUEST_PERMISSION_DIALOG_TYPE;
import static com.clickfreebackup.clickfree.MainActivity.CONTACT_REQUEST_PERMISSION_DIALOG_TYPE;
import static com.clickfreebackup.clickfree.MainActivity.CONTACT_US_DIALOG_TYPE;
import static com.clickfreebackup.clickfree.MainActivity.STORAGE_REQUEST_PERMISSION_DIALOG_TYPE;

public class MainDialogFragment extends DialogFragment {
    private MainDialogListener mMainDialogListener;
    private String mDialogType;
    private String mTitleText;
    private String mDescriptionText;

    public MainDialogFragment(MainDialogListener mainDialogListener, String dialogType, String titleText, String descriptionText) {
        mMainDialogListener = mainDialogListener;
        mDialogType = dialogType;
        mTitleText = titleText;
        mDescriptionText = descriptionText;
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

        final Context context = getContext();
        final TextView title = view.findViewById(R.id.title_text);
        final TextView description = view.findViewById(R.id.description_text);
        final Button actionButton = view.findViewById(R.id.action_button);

        switch (mDialogType) {
            case STORAGE_REQUEST_PERMISSION_DIALOG_TYPE:
                title.setText(mTitleText);
                description.setText(mDescriptionText);
                actionButton.setText(getResources().getText(R.string.ok));

                actionButton.setOnClickListener(contactUsButton -> {
                    if (context != null) {
                        mMainDialogListener.onStoragePermissionClicked();
                        mMainDialogListener.onDismissDialog();
                    }
                });
                break;
            case CAMERA_REQUEST_PERMISSION_DIALOG_TYPE:
                title.setText(mTitleText);
                description.setText(mDescriptionText);
                actionButton.setText(getResources().getText(R.string.ok));

                actionButton.setOnClickListener(contactUsButton -> {
                    if (context != null) {
                        mMainDialogListener.onCameraPermissionClicked();
                        mMainDialogListener.onDismissDialog();
                    }
                });
                break;
            case CONTACT_REQUEST_PERMISSION_DIALOG_TYPE:
                title.setText(mTitleText);
                description.setText(mDescriptionText);
                actionButton.setText(getResources().getText(R.string.ok));

                actionButton.setOnClickListener(contactUsButton -> {
                    if (context != null) {
                        mMainDialogListener.onContactPermissionClicked();
                        mMainDialogListener.onDismissDialog();
                    }
                });
                break;
            case CONTACT_US_DIALOG_TYPE:
                actionButton.setOnClickListener(contactUsButton -> {
                    if (context != null) {
                        SenderUtil.sendMessageToBackup(context);
                        mMainDialogListener.onDismissDialog();
                    }
                });
                break;
            default:
                title.setText(mTitleText);
                description.setText(mDescriptionText);
                actionButton.setText(getResources().getText(R.string.ok));

                actionButton.setOnClickListener(contactUsButton -> {
                    mMainDialogListener.onDismissDialog();
                });
                break;
        }
    }
}
