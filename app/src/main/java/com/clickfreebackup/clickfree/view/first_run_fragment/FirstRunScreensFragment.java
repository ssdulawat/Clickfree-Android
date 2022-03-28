package com.clickfreebackup.clickfree.view.first_run_fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.clickfreebackup.clickfree.MainActivity;
import com.clickfreebackup.clickfree.R;
import com.clickfreebackup.clickfree.model.ContactBody;
import com.clickfreebackup.clickfree.repository.SendGridRepositoryListener;
import com.clickfreebackup.clickfree.view.FirstRunFragmentListener;

public class FirstRunScreensFragment extends Fragment implements FirstRunScreensListener {

    private ViewPager mPager;
    private ScreensPagerAdapter mScreensPagerAdapter;
    private FirstRunFragmentListener mFirstRunFragmentListener;
    private boolean isLastInstructionScreen = false;

    public FirstRunScreensFragment(FirstRunFragmentListener firstRunFragmentListener) {
        mFirstRunFragmentListener = firstRunFragmentListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.first_run_screens_fragment_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainActivity activity = (MainActivity) getContext();
        mPager = view.findViewById(R.id.screens_pager);
        if (activity != null) {
            mScreensPagerAdapter = new ScreensPagerAdapter(activity.getSupportFragmentManager(), 1, this);
            mPager.setAdapter(mScreensPagerAdapter);
            mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    if (position == 3) {
                        offInstructionsIfLastScreen();
                        isLastInstructionScreen = true;
                    } else {
                        isLastInstructionScreen = false;
                    }
                }

                @Override
                public void onPageSelected(int position) {
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                }
            });
        }
    }

    @Override
    public void onSkipClicked() {
        isLastInstructionScreen = true;
        mPager.setCurrentItem(mScreensPagerAdapter.getCount() - 1);
    }

    @Override
    public void onNextClicked() {
        final int currentItem = mPager.getCurrentItem();
        if (currentItem != mScreensPagerAdapter.getCount() - 2) {
            mPager.setCurrentItem(mPager.getCurrentItem() + 1);
        } else {
            isLastInstructionScreen = true;
            mPager.setCurrentItem(mPager.getCurrentItem() + 1);
        }
    }

    @Override
    public void onSmoothFragment() {
        offInstructionsIfLastScreen();
    }

    @Override
    public void onSendEmailButtonClicked(final ContactBody contactBody, final SendGridRepositoryListener sendGridRepositoryListener) {
        mFirstRunFragmentListener.onSendEmailButtonClicked(contactBody, sendGridRepositoryListener);
    }

    private void offInstructionsIfLastScreen() {
        if (isLastInstructionScreen) {
            onInstructionsPassed();
        }
    }

    private void onInstructionsPassed() {
        mFirstRunFragmentListener.onInstructionsPassed();
        mPager.removeAllViews();
        mPager.setVisibility(View.GONE);
    }
}
