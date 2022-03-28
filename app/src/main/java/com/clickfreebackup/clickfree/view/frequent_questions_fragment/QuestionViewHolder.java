package com.clickfreebackup.clickfree.view.frequent_questions_fragment;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.ParentViewHolder;
import com.clickfreebackup.clickfree.R;

public class QuestionViewHolder extends ParentViewHolder {

    final TextView mQuestionTitle;
    private final ImageView mArrow;

    QuestionViewHolder(View itemView) {
        super(itemView);
        mQuestionTitle = itemView.findViewById(R.id.question_title);
        mArrow = itemView.findViewById(R.id.arrow);
        itemView.setOnClickListener(v -> {
            if (isExpanded()) {
                collapseView();
                mArrow.setScaleY(1f);
            } else {
                mArrow.setScaleY(-1f);
                expandView();
            }
        });
    }

    @Override
    public boolean shouldItemViewClickToggleExpansion() {
        return false;
    }
}
