package com.clickfreebackup.clickfree.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ContactBody {
    @SerializedName("list_ids")
    private List<String> listIds = null;
    @SerializedName("contacts")
    private List<Contact> contacts = null;

    public ContactBody(List<String> listIds, List<Contact> contacts) {
        this.listIds = listIds;
        this.contacts = contacts;
    }

    @SerializedName("list_ids")
    public List<String> getListIds() {
        return listIds;
    }

    @SerializedName("list_ids")
    public void setListIds(List<String> listIds) {
        this.listIds = listIds;
    }

    @SerializedName("contacts")
    public List<Contact> getContacts() {
        return contacts;
    }

    @SerializedName("contacts")
    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }
}
