package com.clickfreebackup.clickfree.view.frequent_questions_fragment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.bignerdranch.expandablerecyclerview.ExpandableRecyclerAdapter;
import com.clickfreebackup.clickfree.R;
import com.clickfreebackup.clickfree.model.FrequentQuestion;
import com.clickfreebackup.clickfree.model.FrequentQuestionDescription;

import java.util.List;

public class FrequentQuestionRecyclerAdapter extends ExpandableRecyclerAdapter<FrequentQuestion, FrequentQuestionDescription, QuestionViewHolder, QuestionChildViewHolder> {

    private LayoutInflater mLayoutInflater;

    FrequentQuestionRecyclerAdapter(@NonNull List<FrequentQuestion> parentList, Context context) {
        super(parentList);
        this.mLayoutInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public QuestionViewHolder onCreateParentViewHolder(@NonNull ViewGroup parentViewGroup, int viewType) {
        return new QuestionViewHolder(mLayoutInflater.inflate(R.layout.list_item_frequent_question, parentViewGroup, false));
    }

    @NonNull
    @Override
    public QuestionChildViewHolder onCreateChildViewHolder(@NonNull ViewGroup childViewGroup, int viewType) {
        return new QuestionChildViewHolder(mLayoutInflater.inflate(R.layout.list_item_frequent_question_child, childViewGroup, false));
    }

    @Override
    public void onBindParentViewHolder(@NonNull QuestionViewHolder parentViewHolder, int parentPosition, @NonNull FrequentQuestion frequentQuestion) {
        parentViewHolder.mQuestionTitle.setText(frequentQuestion.getTitle());
    }

    @Override
    public void onBindChildViewHolder(@NonNull QuestionChildViewHolder childViewHolder, int parentPosition, int childPosition, @NonNull FrequentQuestionDescription frequentQuestionDescription) {
        childViewHolder.mQuestionDescription.setText(frequentQuestionDescription.getDescription());
    }
}
