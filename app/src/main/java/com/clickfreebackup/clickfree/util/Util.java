package com.clickfreebackup.clickfree.util;

import android.app.ActivityManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import com.clickfreebackup.clickfree.App;
import com.clickfreebackup.clickfree.ContentType;
import com.clickfreebackup.clickfree.usb.UsbFileParams;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import timber.log.Timber;

public abstract class Util {

    public static List<UsbFileParams> getAllImagePaths(Context context) {
        List<UsbFileParams> allPath = new ArrayList<>();

        // The list of columns we're interested in:
        String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_ADDED};

        final Cursor cursor = context.getContentResolver().
                query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // Specify the provider
                        columns, // The columns we're interested in
                        null, // A WHERE-filter query
                        null, // The arguments for the filter-query
                        MediaStore.Images.Media.DATE_ADDED + " DESC" // Order the results, newest first
                );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                final int imagePathIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                do {
                    String filePath = cursor.getString(imagePathIndex);
                    Uri fileUri = Uri.fromFile(new File(filePath));
                    UsbFileParams fileParams = new UsbFileParams(fileUri, ContentType.PHOTO_VIDEO);
                    allPath.add(fileParams);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return allPath;
    }

    public static List<UsbFileParams> getPhotos(HashMap<String, String> urlList, ContentType contentType) {
        final List<UsbFileParams> allPath = new ArrayList<>();
        for (String key : urlList.keySet()) {
            allPath.add(new UsbFileParams(key, Uri.parse(urlList.get(key)), contentType));
        }
        return allPath;
    }

    public static List<UsbFileParams> getFacebookPhotos(HashMap<String, HashSet<String>> mediaUrlMap, ContentType contentType) {
        final List<UsbFileParams> allPath = new ArrayList<>();
        for (String albumMapKey : mediaUrlMap.keySet()) {
            allPath.addAll(parseAlbumUrlMap(albumMapKey, mediaUrlMap.get(albumMapKey), contentType));
        }
        return allPath;
    }

    private static List<UsbFileParams> parseAlbumUrlMap(final String albumMapKey, final HashSet<String> photoUrlSet,
                                                        final ContentType contentType) {
        final List<UsbFileParams> allPath = new ArrayList<>();
        if (photoUrlSet != null) {
            for (String url : photoUrlSet) {
                allPath.add(new UsbFileParams(albumMapKey, getIdFromUrl(url), Uri.parse(url), contentType));
            }
        }
        return allPath;
    }

    public static String getIdFromUrl(final String url) {
        final String[] parts = url.split("/");
        return parts[parts.length - 1].split("\\?")[0];
    }

    public static List<UsbFileParams> getAllVideosPaths(Context context) {
        List<UsbFileParams> allPath = new ArrayList<>();

        // The list of columns we're interested in:
        String[] columns = {MediaStore.Video.Media.DATA, MediaStore.Video.Media.DATE_ADDED};

        final Cursor cursor = context.getContentResolver().
                query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, // Specify the provider
                        columns, // The columns we're interested in
                        null, // A WHERE-filter query
                        null, // The arguments for the filter-query
                        MediaStore.Video.Media.DATE_ADDED + " DESC" // Order the results, newest first
                );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                final int imagePathIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                do {
                    String filePath = cursor.getString(imagePathIndex);
                    Uri fileUri = Uri.fromFile(new File(filePath));
                    UsbFileParams fileParams = new UsbFileParams(fileUri, ContentType.PHOTO_VIDEO);
                    allPath.add(fileParams);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return allPath;
    }

    public static Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Image", null);
        return Uri.parse(path);
    }

    public static String readFisToString(FileInputStream stream) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            Timber.d("Error reading FileInputString to string.");
            return null;
        }
    }

    public static boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) App.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
