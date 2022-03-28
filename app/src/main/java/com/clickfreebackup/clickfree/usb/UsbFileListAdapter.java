/*
 * (C) Copyright 2014 mjahnen <jahnen@in.tum.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.clickfreebackup.clickfree.usb;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.clickfreebackup.clickfree.R;
import com.github.mjdev.libaums.fs.UsbFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * List adapter to represent the contents of an {@link UsbFile} directory.
 *
 * @author mjahnen
 */
public class UsbFileListAdapter extends ArrayAdapter<UsbFile> {
    private String[] systemDirs = {"Android", "LOST.DIR", "System Volume Information"};
    private List<String> systemDirsList = Arrays.asList(systemDirs);
    private byte[] imageBuffer = new byte[10240];

    /**
     * Class to compare {@link UsbFile}s. If the {@link UsbFile} is an directory
     * it is rated lower than an file, ie. directories come first when sorting.
     */
    private Comparator<UsbFile> comparator = (lhs, rhs) -> {

        if (lhs.isDirectory() && !rhs.isDirectory()) {
            return -1;
        }

        if (rhs.isDirectory() && !lhs.isDirectory()) {
            return 1;
        }

        return lhs.getName().compareToIgnoreCase(rhs.getName());
    };

    private List<UsbFile> files;
    private UsbFile currentDir;
    private LayoutInflater inflater;

    /**
     * Constructs a new List Adapter to show {@link UsbFile}s.
     *
     * @param context The context.
     * @param dir     The directory which shall be shown.
     * @throws IOException If reading fails.
     */
    public UsbFileListAdapter(Context context, UsbFile dir) throws IOException {
        super(context, R.layout.list_item);
        currentDir = dir;
        files = new ArrayList<>();
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        refresh();
    }

    /**
     * Reads the contents of the directory and notifies that the View shall be
     * updated.
     *
     * @throws IOException If reading contents of a directory fails.
     */
    public void refresh() throws IOException {
        files = new ArrayList<>(Arrays.asList(currentDir.listFiles()));

        // Don't show system directories
        if (currentDir.isRoot()) {
            Iterator<UsbFile> iterator = files.iterator();
            while (iterator.hasNext()) {
                UsbFile file = iterator.next();
                if (file.isDirectory() && systemDirsList.contains(file.getName())) iterator.remove();
            }
        }

        Collections.sort(files, comparator);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = inflater.inflate(R.layout.list_item, parent, false);
        }

        TextView typeText = view.findViewById(R.id.type_text_view);
        TextView nameText = view.findViewById(R.id.name_text_view);
        ImageView imageView = view.findViewById(R.id.icon);
        UsbFile file = files.get(position);

        if (file.isDirectory()) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.folder));
            typeText.setText(R.string.directory);
        } else {
            imageView.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.file));
            typeText.setText(R.string.file);
        }

        nameText.setText(file.getName());

        return view;
    }

    @Override
    public UsbFile getItem(int position) {
        return files.get(position);
    }

    /**
     * @return the directory which is currently be shown.
     */
    public UsbFile getCurrentDir() {
        return currentDir;
    }

}