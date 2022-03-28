package com.clickfreebackup.clickfree.view;

import com.clickfreebackup.clickfree.model.ContactBody;
import com.clickfreebackup.clickfree.repository.SendGridRepositoryListener;

public interface FirstRunFragmentListener {

    void onInstructionsPassed();

    void onSendEmailButtonClicked(final ContactBody contactBody, final SendGridRepositoryListener sendGridRepositoryListener);

}
