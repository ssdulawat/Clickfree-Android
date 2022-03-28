package com.clickfreebackup.clickfree.usb;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clickfreebackup.clickfree.ContentType;
import com.clickfreebackup.clickfree.R;
import com.github.mjdev.libaums.fs.UsbFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class UsbFileRecyclerAdapter extends RecyclerView.Adapter<UsbFileRecyclerAdapter.ViewHolder> {
  private final String[] systemDirs = {"Android", "LOST.DIR", "System Volume Information"};
  private final String[] imageTypes = {"jpg", "jpeg", "png"};
  private final List<String> systemDirsList = Arrays.asList(systemDirs);
  private final List<String> imageTypesList = Arrays.asList(imageTypes);

  private final Context context;
  private final List<UsbFile> incomeUsbFiles;
  private final List<UsbFile> usbFiles = new ArrayList<>();
  private final UsbFileAdapterListener listener;
  private final LinearLayoutManager layoutManager;

  private final Comparator<UsbFile> comparator = (lhs, rhs) -> {

    if (lhs.isDirectory() && !rhs.isDirectory()) {
      return -1;
    }

    if (rhs.isDirectory() && !lhs.isDirectory()) {
      return 1;
    }

    return lhs.getName().compareToIgnoreCase(rhs.getName());
  };

  public UsbFileRecyclerAdapter(final Context context, final UsbFileAdapterListener listener,
                                final LinearLayoutManager layoutManager, final List<UsbFile> devs) {
    this.context = context;
    this.incomeUsbFiles = devs;
    this.listener = listener;
    this.layoutManager = layoutManager;
    refresh();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent,
            false));
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.setContent(usbFiles.get(position));
  }

  @Override
  public int getItemCount() {
    return usbFiles.size();
  }

  public void setState(boolean isIdleState) {
    if (listener.setState(isIdleState)) {
      handleState(isIdleState);
    }
  }

  private void refresh() {
    usbFiles.clear();

    for (UsbFile file : incomeUsbFiles) {
      if (file.isDirectory() && !systemDirsList.contains(file.getName())) {
        usbFiles.add(file);
      } else if (!file.isDirectory()) {
        usbFiles.add(file);
      }
    }

    Collections.sort(usbFiles, comparator);
    notifyDataSetChanged();
  }

  public class ViewHolder extends RecyclerView.ViewHolder {
    final TextView typeText, nameText;
    final ImageView imageView;
    UsbFile usbFile;

    public ViewHolder(@NonNull View itemView) {
      super(itemView);

      typeText = itemView.findViewById(R.id.type_text_view);
      nameText = itemView.findViewById(R.id.name_text_view);
      imageView = itemView.findViewById(R.id.icon);

      itemView.setOnClickListener(v -> {
        if (usbFile.isDirectory()) {
          listener.onDirectorySelected(usbFile);
        } else {
          listener.onFileSelected(usbFile);
        }
      });
    }

    public void setContent(UsbFile dev) {
      this.usbFile = dev;

      if (usbFile.isDirectory()) {
        imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.folder));
        typeText.setText(R.string.directory);
      } else if (usbFile.getAbsolutePath().contains(ContentType.CONTACTS.dirName)) {
        imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.file));
        typeText.setText(R.string.file);
      } else {
        setDefaultImageIcon(imageView, getAdapterPosition());
        loadBitmap(usbFile, imageView);
        typeText.setText(R.string.file);
      }

      nameText.setText(usbFile.getName());
    }

    private void setDefaultImageIcon(final ImageView imageView, final int position) {
      final String[] nameArray = usbFiles.get(position).getName().split("\\.");
      if (nameArray.length > 1 && !imageTypesList.contains(nameArray[1])) {
        imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_default_movie));
      } else {
        imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_default_image));
      }
    }
  }

  private void handleState(final boolean isIdleState) {
    if (isIdleState) {
      onIdleStateEvent();
    }
  }

  private void onIdleStateEvent() {
    final int itemsCount =
            layoutManager.findLastVisibleItemPosition() - layoutManager.findFirstVisibleItemPosition() + 1;
    for (int i = 0; i < itemsCount; i++) {
      final View galeryItem = layoutManager.getChildAt(i);
      fillGalleryItem(galeryItem, i);
    }
  }

  private void fillGalleryItem(final View galeryItem, final int incrementValue) {
    if (galeryItem != null) {
      final ImageView imageView = galeryItem.findViewById(R.id.icon);
      final UsbFile file = usbFiles.get(layoutManager.findFirstVisibleItemPosition() + incrementValue);
      loadBitmap(file, imageView);
    }
  }

  private void loadBitmap(final UsbFile file, final ImageView imageView) {
    listener.loadBitmap(file, imageView);
  }
}