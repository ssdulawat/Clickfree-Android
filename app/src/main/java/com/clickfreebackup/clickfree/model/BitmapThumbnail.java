package com.clickfreebackup.clickfree.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class BitmapThumbnail {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "image_name")
    public String imageName;

    @ColumnInfo(name = "image_url")
    public String imageUrl;

    public BitmapThumbnail(String imageName, String imageUrl) {
        this.imageName = imageName;
        this.imageUrl = imageUrl;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
