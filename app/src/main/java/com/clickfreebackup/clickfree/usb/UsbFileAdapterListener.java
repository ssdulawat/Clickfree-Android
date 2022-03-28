package com.clickfreebackup.clickfree.usb;

import android.widget.ImageView;

import com.github.mjdev.libaums.fs.UsbFile;

public interface UsbFileAdapterListener {
  void onDirectorySelected(final UsbFile file);

  void onFileSelected(final UsbFile file);

  boolean setState(final boolean isIdleState);

  void loadBitmap(final UsbFile file, final ImageView imageView);
}
