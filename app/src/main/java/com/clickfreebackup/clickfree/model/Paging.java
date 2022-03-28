package com.clickfreebackup.clickfree.model;

import com.google.gson.annotations.SerializedName;

public class Paging {
    @SerializedName("cursors")
    private Cursors cursors;
    @SerializedName("next")
    private String next;

    public Cursors getCursors() {
        return cursors;
    }

    public void setCursors(Cursors cursors) {
        this.cursors = cursors;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }
}
