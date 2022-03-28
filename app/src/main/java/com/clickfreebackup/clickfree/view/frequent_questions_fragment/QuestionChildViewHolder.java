package com.clickfreebackup.clickfree.view.frequent_questions_fragment;

import android.view.View;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.ChildViewHolder;
import com.clickfreebackup.clickfree.R;

class QuestionChildViewHolder extends ChildViewHolder {

    final TextView mQuestionDescription;

    QuestionChildViewHolder(View itemView) {
        super(itemView);

        mQuestionDescription = itemView.findViewById(R.id.question_description);
    }
}
