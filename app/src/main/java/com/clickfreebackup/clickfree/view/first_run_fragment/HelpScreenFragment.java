package com.clickfreebackup.clickfree.view.first_run_fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.clickfreebackup.clickfree.R;
import com.clickfreebackup.clickfree.util.SenderUtil;

public class HelpScreenFragment extends Fragment {

    private FirstRunScreensListener mFirstRunScreensListener;

    HelpScreenFragment(FirstRunScreensListener mFirstRunScreensListener) {
        this.mFirstRunScreensListener = mFirstRunScreensListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.help_screen_fragment_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Context context = getContext();

        view.findViewById(R.id.next_button).setOnClickListener(nextButtonView -> mFirstRunScreensListener.onNextClicked());
        view.findViewById(R.id.skip_button).setOnClickListener(nextButtonView -> mFirstRunScreensListener.onSkipClicked());
        view.findViewById(R.id.contact_form_button).setOnClickListener(nextButtonView -> {
            if (context != null) {
                SenderUtil.sendMessageToBackup(context);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mFirstRunScreensListener = null;
    }
}
