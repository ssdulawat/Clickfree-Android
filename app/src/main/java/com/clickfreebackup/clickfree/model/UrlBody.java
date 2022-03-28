package com.clickfreebackup.clickfree.model;

public class UrlBody {
    private String url;
    private boolean isSelected = false;

    public UrlBody(String url, boolean isSelected) {
        this.url = url;
        this.isSelected = isSelected;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
