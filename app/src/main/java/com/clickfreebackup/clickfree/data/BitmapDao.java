package com.clickfreebackup.clickfree.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.clickfreebackup.clickfree.model.BitmapThumbnail;

import io.reactivex.Single;

@Dao
public interface BitmapDao {

    @Query("SELECT * FROM bitmapthumbnail WHERE image_name LIKE :thumbnailName")
    Single<BitmapThumbnail> findBitmapThumbnailByName(String thumbnailName);

    @Insert
    void insert(BitmapThumbnail bitmapThumbnail);

}
