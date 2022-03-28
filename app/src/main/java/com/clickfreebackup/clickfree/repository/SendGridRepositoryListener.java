package com.clickfreebackup.clickfree.repository;

public interface SendGridRepositoryListener {

    void emailSendingSuccess();

    void emailSendingFail();
}
