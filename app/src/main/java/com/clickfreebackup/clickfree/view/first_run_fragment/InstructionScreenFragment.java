package com.clickfreebackup.clickfree.view.first_run_fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.clickfreebackup.clickfree.R;

public class InstructionScreenFragment extends Fragment {

    private FirstRunScreensListener mFirstRunScreensListener;

    InstructionScreenFragment(FirstRunScreensListener firstRunScreensListener) {
        mFirstRunScreensListener = firstRunScreensListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.instruction_screen_fragment_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.skip_button).setOnClickListener(nextButtonView -> mFirstRunScreensListener.onSkipClicked());
        view.findViewById(R.id.next_button).setOnClickListener(nextButtonView -> mFirstRunScreensListener.onNextClicked());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mFirstRunScreensListener = null;
    }
}
