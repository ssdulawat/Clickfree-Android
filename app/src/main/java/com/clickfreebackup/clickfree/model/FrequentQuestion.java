package com.clickfreebackup.clickfree.model;

import com.bignerdranch.expandablerecyclerview.model.Parent;

import java.util.List;

public class FrequentQuestion implements Parent<FrequentQuestionDescription> {

    private List<FrequentQuestionDescription> mDescriptionList;
    private String mTitle;

    public FrequentQuestion(List<FrequentQuestionDescription> mDescriptionList, String mTitle) {
        this.mDescriptionList = mDescriptionList;
        this.mTitle = mTitle;
    }

    public String getTitle() {
        return mTitle;
    }

    @Override
    public List<FrequentQuestionDescription> getChildList() {
        return mDescriptionList;
    }

    @Override
    public boolean isInitiallyExpanded() {
        return false;
    }
}
