package com.clickfreebackup.clickfree.view.first_run_fragment;

import com.clickfreebackup.clickfree.model.ContactBody;
import com.clickfreebackup.clickfree.repository.SendGridRepositoryListener;

public interface FirstRunScreensListener {

    void onSkipClicked();

    void onNextClicked();

    void onSmoothFragment();

    void onSendEmailButtonClicked(final ContactBody contactBody, final SendGridRepositoryListener sendGridRepositoryListener);

}
