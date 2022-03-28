package com.clickfreebackup.clickfree.view.first_run_fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.clickfreebackup.clickfree.R;

public class SmoothFragment extends Fragment {

    private FirstRunScreensListener mFirstRunScreensListener;

    SmoothFragment(FirstRunScreensListener firstRunScreensListener) {
        this.mFirstRunScreensListener = firstRunScreensListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.smooth_fragment_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mFirstRunScreensListener.onSmoothFragment();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFirstRunScreensListener = null;
    }
}
