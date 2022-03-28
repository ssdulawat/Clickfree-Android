package com.clickfreebackup.clickfree.repository;

import com.clickfreebackup.clickfree.model.ContactBody;

public interface SendGridRepository {
    void sendEmail(final ContactBody contactBody, final SendGridRepositoryListener sendGridRepositoryListener);
}
