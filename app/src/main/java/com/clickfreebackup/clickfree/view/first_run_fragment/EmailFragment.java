package com.clickfreebackup.clickfree.view.first_run_fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.clickfreebackup.clickfree.R;
import com.clickfreebackup.clickfree.model.Contact;
import com.clickfreebackup.clickfree.model.ContactBody;
import com.clickfreebackup.clickfree.repository.SendGridRepositoryListener;
import com.clickfreebackup.clickfree.view.ClickFreeNotification;
import com.clickfreebackup.clickfree.view.ClickFreeNotificationListener;

import java.util.Collections;

public class EmailFragment extends Fragment implements SendGridRepositoryListener, ClickFreeNotificationListener {

    public static final String EMAIL_SENT_SUCCESSFULLY = "EMAIL_SENT_SUCCESSFULLY";
    public static final String EMAIL_SENT_FAIL = "EMAIL_SENT_FAIL";
    private final String CONTACT_LIST_ID = "9931a43a-0254-46a3-8379-365ad338b4eb";
    private EditText mEmailEditText;
    private TextView mEmailHint;
    private Button mSubmitButton;
    private ClickFreeNotification mClickFreeNotification;
    private FragmentManager mSupportFragmentManager;
    private ProgressBar mCircularProgress;
    private View mUnderline;

    private FirstRunScreensListener mFirstRunScreensListener;

    EmailFragment(FirstRunScreensListener mFirstRunScreensListener) {
        this.mFirstRunScreensListener = mFirstRunScreensListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.email_fragment_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEmailEditText = view.findViewById(R.id.email_edit_text);
        mEmailHint = view.findViewById(R.id.email_hint);
        mSubmitButton = view.findViewById(R.id.submit_button);
        mCircularProgress = view.findViewById(R.id.progress_circular);
        mUnderline = view.findViewById(R.id.edit_text_underline);
        view.findViewById(R.id.next_button).setOnClickListener(nextButtonView -> mFirstRunScreensListener.onNextClicked());
        view.findViewById(R.id.skip_button).setOnClickListener(nextButtonView -> mFirstRunScreensListener.onSkipClicked());

        final FragmentActivity activity = getActivity();

        if (activity != null) {
            mSupportFragmentManager = activity.getSupportFragmentManager();
        }

        mSubmitButton.setOnClickListener(submitButtonView -> {
            mCircularProgress.setVisibility(View.VISIBLE);
            final String email = mEmailEditText.getText().toString();
            final ContactBody contactBody = new ContactBody(
                    Collections.singletonList(CONTACT_LIST_ID), Collections.singletonList(new Contact(email))
            );
            mFirstRunScreensListener.onSendEmailButtonClicked(contactBody, this);
            mEmailEditText.setText("");
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mFirstRunScreensListener = null;
    }

    @Override
    public void emailSendingSuccess() {
        mCircularProgress.setVisibility(View.GONE);
        mEmailEditText.setVisibility(View.GONE);
        mEmailHint.setVisibility(View.GONE);
        mSubmitButton.setVisibility(View.GONE);
        mUnderline.setVisibility(View.GONE);

        showResultNotification(true);
    }

    @Override
    public void emailSendingFail() {
        mCircularProgress.setVisibility(View.GONE);
        showResultNotification(false);
    }

    private void showResultNotification(final boolean isSent) {
        if (isSent && mSupportFragmentManager != null) {
            mClickFreeNotification = new ClickFreeNotification(this, EMAIL_SENT_SUCCESSFULLY);
            mClickFreeNotification.show(mSupportFragmentManager, EMAIL_SENT_SUCCESSFULLY);
        } else if (!isSent && mSupportFragmentManager != null) {
            mClickFreeNotification = new ClickFreeNotification(this, EMAIL_SENT_FAIL);
            mClickFreeNotification.show(mSupportFragmentManager, EMAIL_SENT_SUCCESSFULLY);
        }
    }

    @Override
    public void onActionButtonClicked() {
        mClickFreeNotification.dismiss();
    }
}
