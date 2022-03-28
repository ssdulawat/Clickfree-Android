package com.clickfreebackup.clickfree;

import android.content.Context;
import android.widget.ImageView;

import com.clickfreebackup.clickfree.usb.BitmapHandler;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;

public class FileListPresenter {
    private BitmapHandler bitmapHandler;

    FileListPresenter() {
    }

    void initBitmapHandler(final Context context, final FileSystem currentFs) {
        bitmapHandler = new BitmapHandler(context, currentFs);
    }

    public void loadBitmap(final Context context, final UsbFile file, final ImageView imageView) {
        if (bitmapHandler != null) {
            bitmapHandler.loadBitmap(context, file, imageView);
        }
    }

    public void setScrollState(boolean scrollState) {
        if (bitmapHandler != null) {
            bitmapHandler.setScrollState(scrollState);
        }
    }

    void onDestroyActivity() {
        if (bitmapHandler != null) {
            bitmapHandler.onDestroyActivity();
        }
    }
}
