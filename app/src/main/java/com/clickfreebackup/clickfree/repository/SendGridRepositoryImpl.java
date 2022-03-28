package com.clickfreebackup.clickfree.repository;

import com.clickfreebackup.clickfree.model.ContactBody;
import com.clickfreebackup.clickfree.network.SendGridService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class SendGridRepositoryImpl implements SendGridRepository {

    private SendGridService mSendGridService;

    public SendGridRepositoryImpl() {
        mSendGridService = SendGridService.getInstance();
    }

    @Override
    public void sendEmail(final ContactBody contactBody, final SendGridRepositoryListener sendGridRepositoryListener) {
        mSendGridService.getSendGridApi().saveEmail(contactBody).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                sendGridRepositoryListener.emailSendingSuccess();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Timber.e(t);
                sendGridRepositoryListener.emailSendingFail();
            }
        });
    }
}
