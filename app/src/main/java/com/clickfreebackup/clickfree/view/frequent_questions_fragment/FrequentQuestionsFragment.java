package com.clickfreebackup.clickfree.view.frequent_questions_fragment;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bignerdranch.expandablerecyclerview.ExpandableRecyclerAdapter;
import com.clickfreebackup.clickfree.R;
import com.clickfreebackup.clickfree.model.FrequentQuestion;
import com.clickfreebackup.clickfree.model.FrequentQuestionDescription;
import com.clickfreebackup.clickfree.util.SenderUtil;

import java.util.ArrayList;
import java.util.List;

public class FrequentQuestionsFragment extends Fragment {
    private Resources mResources;
    private FrequentQuestionListener mFrequentQuestionListener;

    public FrequentQuestionsFragment(final FrequentQuestionListener frequentQuestionListener) {
        mFrequentQuestionListener = frequentQuestionListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frequent_question_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Context context = getContext();
        if (context != null) {
            mResources = context.getResources();
        }

        view.findViewById(R.id.contact_us_button).setOnClickListener(contactButton -> {
            if (context != null) {
                SenderUtil.sendMessageToBackup(context);
            }
        });
        view.findViewById(R.id.back_arrow_image).setOnClickListener(contactButton -> mFrequentQuestionListener.onBackPressedFQ());

        final RecyclerView frequentQuestionRecycler = view.findViewById(R.id.frequent_questions_recycler);

        final List<FrequentQuestion> questionList = getQuestionList();

        final FrequentQuestionRecyclerAdapter frequentQuestionRecyclerAdapter =
                new FrequentQuestionRecyclerAdapter(questionList, getContext());

        frequentQuestionRecyclerAdapter.setExpandCollapseListener(new ExpandableRecyclerAdapter.ExpandCollapseListener() {
            @Override
            public void onParentExpanded(int parentPosition) {
            }

            @Override
            public void onParentCollapsed(int parentPosition) {
            }
        });

        frequentQuestionRecycler.setAdapter(frequentQuestionRecyclerAdapter);
        frequentQuestionRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private List<FrequentQuestion> getQuestionList() {

        if (mResources != null) {
            return getFrequentQuestionList(mResources);
        } else {
            return new ArrayList<>();
        }
    }

    private List<FrequentQuestion> getFrequentQuestionList(Resources resources) {
        final List<FrequentQuestion> frequentQuestions = new ArrayList<>();
        final List<String> titles = new ArrayList<>();
        final List<String> descriptions = new ArrayList<>();

        final String firstTitle = resources.getString(R.string.frequent_question_first_title);
        final String secondTitle = resources.getString(R.string.frequent_question_second_title);
        final String thirdTitle = resources.getString(R.string.frequent_question_third_title);

        final String firstDescription = resources.getString(R.string.frequent_question_first_description);
        final String secondDescription = resources.getString(R.string.frequent_question_second_description);
        final String thirdDescription = resources.getString(R.string.frequent_question_third_description);

        titles.add(firstTitle);
        titles.add(secondTitle);
        titles.add(thirdTitle);

        descriptions.add(firstDescription);
        descriptions.add(secondDescription);
        descriptions.add(thirdDescription);

        for (int i = 0; i < titles.size(); i++) {
            final List<FrequentQuestionDescription> frequentQuestionDescriptions = new ArrayList<>();
            frequentQuestionDescriptions.add(new FrequentQuestionDescription(descriptions.get(i)));
            frequentQuestions.add(new FrequentQuestion(frequentQuestionDescriptions, titles.get(i)));
        }

        return frequentQuestions;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mFrequentQuestionListener = null;
    }
}
