package com.clickfreebackup.clickfree.view.first_run_fragment;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import timber.log.Timber;

public class ScreensPagerAdapter extends FragmentStatePagerAdapter {
    private FirstRunScreensListener mFirstRunScreensListener;

    ScreensPagerAdapter(@NonNull FragmentManager fm, int behavior, FirstRunScreensListener firstRunScreensListener) {
        super(fm, behavior);
        mFirstRunScreensListener = firstRunScreensListener;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new InstructionScreenFragment(mFirstRunScreensListener);
            case 1:
                return new EmailFragment(mFirstRunScreensListener);
            case 2:
                return new HelpScreenFragment(mFirstRunScreensListener);
            case 3:
                return new SmoothFragment(mFirstRunScreensListener);
            default:
                Timber.d("Last instruction fragment has passed");
                return new SmoothFragment(mFirstRunScreensListener);
        }
    }

    @Override
    public int getCount() {
        return 4;
    }
}
