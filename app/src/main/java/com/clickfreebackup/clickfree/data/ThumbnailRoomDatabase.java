package com.clickfreebackup.clickfree.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.clickfreebackup.clickfree.model.BitmapThumbnail;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {BitmapThumbnail.class}, version = 1, exportSchema = false)
public abstract class ThumbnailRoomDatabase extends RoomDatabase {
    private static volatile ThumbnailRoomDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 5;

    public abstract BitmapDao bitmapDao();

    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static ThumbnailRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (ThumbnailRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            ThumbnailRoomDatabase.class, "thumbnail_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
