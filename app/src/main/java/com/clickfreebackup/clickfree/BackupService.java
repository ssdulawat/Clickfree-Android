package com.clickfreebackup.clickfree;

import static com.clickfreebackup.clickfree.BaseActivity.USB_RETRY_DELAY;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.ContactsContract;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.clickfreebackup.clickfree.usb.UsbFileParams;
import com.clickfreebackup.clickfree.usb.UsbOtgManager;
import com.clickfreebackup.clickfree.util.Util;
import com.github.mjdev.libaums.fs.UsbFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Foreground service where the backup/clear tasks are running.
 */
public class BackupService extends Service {
    private boolean isRunning;
    public static final String PARAM_MESSAGE = "Message";
    public static final String PARAM_TITLE = "Title";
    public static final String PARAM_TEXT = "Text";
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    public static final String ACTION_REQUEST_PROGRESS = "ACTION_REQUEST_PROGRESS";
    private NotificationCompat.Builder notificationBuilder;
    private UsbOtgManager usbOtgManager = UsbOtgManager.getInstance();
    private final int BACKUP_NOTIFICATION_ID = 11;
    private NotificationManager notificationManager;
    private Intent intent;
    private ProgressParams progress;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    public static final String CHANNEL_ID = "anawenSilentNotifications";
    public static final String PARAM_PROGRESS = "CurrentProgress";
    public static final String PARAM_SELECTED_FILES = "SelectedFiles";
    public static final String PARAM_CAMERA_PHOTOS = "CameraPhotos";
    public static final String TASK = "Task";
    public static final String TASK_BACKUP_CONTACTS = "BackupContacts";
    public static final String TASK_BACKUP_EVERYTHING = "BackupEverything";
    public static final String TASK_BACKUP_PHOTOVIDEO = "BackupAllPhotoVideo";
    public static final String TASK_BACKUP_SELECTED = "BackupSelected";
    public static final String TASK_CLEAR_STORAGE = "ClearStorage";
    public static final String TASK_BACKUP_CAMERA_PHOTOS = "BackupCameraPhotos";
    public static final String BROADCAST_ACTION_PROGRESS = "com.clickfreebackup.clickfree.BackupProgress";
    public static final String BROADCAST_ACTION_MESSAGE = "com.clickfreebackup.clickfree.ShowMessage";
    public static final String BROADCAST_ACTION_POPUP = "com.clickfreebackup.clickfree.ShowPopup";
    public static final String TASK_BACKUP_INSTAGRAM_PHOTO = "TASK_BACKUP_INSTAGRAM_PHOTO";
    public static final String TASK_BACKUP_SELECTED_INSTAGRAM_PHOTO = "TASK_BACKUP_SELECTED_INSTAGRAM_PHOTO";
    public static final String TASK_BACKUP_FACEBOOK_PHOTO = "TASK_BACKUP_FACEBOOK_PHOTO";
    public static final String TASK_BACKUP_SELECTED_FACEBOOK_PHOTO = "TASK_BACKUP_SELECTED_FACEBOOK_PHOTO";
    public static final String IMAGE_URL_MAP = "IMAGE_URL_MAP";
    private HashMap<String, String> urlHashMap;
    private HashMap<String, HashSet<String>> facebookUrlHashMap;
    private boolean isRetryInProgress = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("BackupService onCreate().");
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        initChannels();
    }

    public void initChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "ClickFree Notifications", NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.intent = intent;
        if (intent != null) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_START_FOREGROUND_SERVICE:
                    isRunning = true;
                    String task = intent.getStringExtra(TASK);
                    startForegroundService(task);
                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    if (!UsbOtgManager.getInstance().isBusy()) {
                        isRunning = false;
                        stopForeground(true);
                        stopSelf();
                    } else {
                        stopBackup();
                    }
                    break;
                case ACTION_REQUEST_PROGRESS:
                    if (progress != null) broadcastProgress(progress);
                    break;
            }
        }

        return START_NOT_STICKY;
    }

    /* Used to build and start foreground service. */
    private void startForegroundService(String task) {
        Timber.d("Starting BackupService");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent stopIntent = new Intent(this, BackupService.class);
        stopIntent.setAction(BackupService.ACTION_STOP_FOREGROUND_SERVICE);
        PendingIntent stopServiceIntent = PendingIntent.getService(this,
                (int) System.currentTimeMillis(), stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder =
                new NotificationCompat.Builder(this.getApplicationContext(), CHANNEL_ID)
                        .setContentText(getString(R.string.download_in_progress))
                        .setOnlyAlertOnce(true)
                        .setSmallIcon(R.drawable.ic_upload_notification)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setProgress(100, 0, true)
                        .setContentIntent(pendingIntent)
                        .setLocalOnly(true)
                        .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.stop), stopServiceIntent);

        startForeground(BACKUP_NOTIFICATION_ID, notificationBuilder.build());

        switch (task) {
            case TASK_BACKUP_CONTACTS:
                onBackupContactsAction();
                break;
            case TASK_BACKUP_EVERYTHING:
                onBackupEverything();
                break;
            case TASK_CLEAR_STORAGE:
                onClearStorageTask();
                break;
            case TASK_BACKUP_PHOTOVIDEO:
                onBackupAllPhotoVideoTask();
                break;
            case TASK_BACKUP_SELECTED:
                List<UsbFileParams> files = intent.getParcelableArrayListExtra(PARAM_SELECTED_FILES);
                onBackupSelectedFilesTask(files, 2);
                break;
            case TASK_BACKUP_INSTAGRAM_PHOTO:
                urlHashMap = (HashMap<String, String>) intent.getSerializableExtra(IMAGE_URL_MAP);
                onBackupFacebookPhoto(ContentType.INSTAGRAM_PHOTO);
                break;
            case TASK_BACKUP_SELECTED_INSTAGRAM_PHOTO:
                urlHashMap = (HashMap<String, String>) intent.getSerializableExtra(IMAGE_URL_MAP);
                onBackupFacebookPhoto(ContentType.SELECTED_INSTAGRAM_PHOTO);
                break;
            case TASK_BACKUP_FACEBOOK_PHOTO:
                facebookUrlHashMap = (HashMap<String, HashSet<String>>) intent.getSerializableExtra(IMAGE_URL_MAP);
                onBackupFacebookPhoto(ContentType.FACEBOOK_PHOTO);
                break;
            case TASK_BACKUP_SELECTED_FACEBOOK_PHOTO:
                urlHashMap = (HashMap<String, String>) intent.getSerializableExtra(IMAGE_URL_MAP);
                onBackupFacebookPhoto(ContentType.SELECTED_FACEBOOK_PHOTO);
                break;
            case TASK_BACKUP_CAMERA_PHOTOS:
                final List<UsbFileParams> cameraPhotosParams = intent.getParcelableArrayListExtra(PARAM_CAMERA_PHOTOS);
                onBackupSelectedFilesTask(cameraPhotosParams, 2);
                break;
        }
    }

    private void onBackupFacebookPhoto(ContentType contentType) {
        if (!verifyConnection()) return;

        final List<UsbFileParams> allMediaFiles;

        if (contentType.equals(ContentType.FACEBOOK_PHOTO)) {
            allMediaFiles = new ArrayList<>(Util.getFacebookPhotos(facebookUrlHashMap, contentType));
        } else if (contentType.equals(ContentType.SELECTED_FACEBOOK_PHOTO)) {
            allMediaFiles = new ArrayList<>(Util.getPhotos(urlHashMap, contentType));
        } else {
            allMediaFiles = new ArrayList<>(Util.getPhotos(urlHashMap, contentType));
        }

        final ProgressListener progressListener = (current, max) -> {
            broadcastProgress(new ProgressParams(
                    App.getContext().getString(R.string.photo_video_backup),
                    getString(R.string.saving_photos_and_videos_with_progress, current, max),
                    max,
                    current,
                    true,
                    true
            ));
        };

        compositeDisposable.add(
                usbOtgManager.writeFacebookPhotoToUsb(allMediaFiles, progressListener)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(param -> broadcastProgress(ProgressParams.setText(App.getContext().getString(R.string.photo_video_backup), App.getContext().getString(R.string.saving_photos_and_videos))))
                        .doFinally(() -> {
                            stopForeground(true);
                            stopSelf();
                        })
                        .subscribe(
                                (copySuccessful) -> {
                                    if (copySuccessful)
                                        broadcastPopup(App.getContext().getString(R.string.photos_saved), App.getContext().getString(R.string.photos_saved_desc));
                                    else
                                        broadcastMessage(App.getContext().getString(R.string.error_backup_failed));
                                },
                                (error) -> {
                                    Timber.e(error, "Backup failed.");
                                    broadcastMessage(App.getContext().getString(R.string.error_backup_failed));
                                })
        );
    }

    /**
     * Check whether USB storage is connected and showing alert message if it isn't.
     */
    private boolean verifyConnection() {
        boolean isConnected = usbOtgManager.isConnected();

        if (!isConnected) {
            broadcastMessage(App.getContext().getString(R.string.error_usb_connect));
        }

        return isConnected;
    }

    private void stopBackup() {
        broadcastProgress(ProgressParams.stopping());
        UsbOtgManager.getInstance().stopWrite();
    }

    private void onBackupContactsAction() {
        if (!verifyConnection()) {
            stopSelf();
            return;
        }

        ProgressListener vcfProgressListener = (current, max) -> {
            Timber.d("Progress: %s", current);
            broadcastProgress(new ProgressParams(
                    App.getContext().getString(R.string.saving_contacts),
                    App.getContext().getString(R.string.saving_contacts_file),
                    max,
                    current,
                    true,
                    true
            ));
        };

        ProgressListener writeUsbProgressListener = (progress, total) -> {
            double part = (double) progress / (double) total;
            int currentProgress = (int) (40 * part);
            // view.setCurrentProgress(60 + currentProgress);
            broadcastProgress(new ProgressParams(
                    App.getContext().getString(R.string.saving_contacts),
                    App.getContext().getString(R.string.writing_contacts),
                    total,
                    60 + currentProgress,
                    true,
                    true
            ));
        };

        compositeDisposable.add(
                backupContacts(vcfProgressListener, writeUsbProgressListener)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(param -> broadcastProgress(ProgressParams.setText(App.getContext().getString(R.string.saving_contacts), App.getContext().getString(R.string.saving_contacts_file))))
                        .doFinally(() -> {
                            stopForeground(true);
                            stopSelf();
                        })
                        .subscribe(
                                (copySuccessful) -> {
                                    if (copySuccessful)
                                        broadcastPopup(App.getContext().getString(R.string.contacts_copied_success), App.getContext().getString(R.string.contacts_saved_desc));
                                    else broadcastMessage("Contacts backup failed.");
                                },
                                (error) -> {
                                    Timber.e(error, "Contacts backup failed.");
                                    broadcastMessage("Contacts backup failed.");
                                })
        );
    }

    /**
     * Backup everything: contacts and photo/video files.
     */
    private void onBackupEverything() {
        if (!verifyConnection()) return;

        List<UsbFileParams> imageFiles = Util.getAllImagePaths(App.getContext());
        List<UsbFileParams> videoFiles = Util.getAllVideosPaths(App.getContext());

        List<UsbFileParams> allMediaFiles = new ArrayList<>();
        allMediaFiles.addAll(imageFiles);
        allMediaFiles.addAll(videoFiles);

        broadcastProgress(new ProgressParams(
                App.getContext().getString(R.string.saving_everything),
                App.getContext().getString(R.string.saving_contacts_file),
                100,
                0,
                true,
                true));

        ProgressListener vcfProgressListener = (current, max) -> {
            int contactsMaxProgress = 10;
            double part = (double) current / max;
            int progress = (int) (part * contactsMaxProgress);

            broadcastProgress(new ProgressParams(
                    App.getContext().getString(R.string.saving_everything),
                    App.getContext().getString(R.string.saving_contacts_file),
                    100,
                    progress,
                    true,
                    true));
        };

        ProgressListener writeContactsProgressListener = (current, max) -> {
            int contactsMaxProgress = 5;
            double part = (double) current / max;
            int progress = (int) (part * contactsMaxProgress + 10);

            broadcastProgress(new ProgressParams(
                    App.getContext().getString(R.string.saving_everything),
                    App.getContext().getString(R.string.writing_contacts),
                    100,
                    progress,
                    true,
                    true));
        };

        ProgressListener photoVideoProgressListener = (current, max) -> {
            int photoVideoMaxProgress = 85;
            double part = (double) current / max;
            int progress = (int) (part * photoVideoMaxProgress + 15);

            broadcastProgress(new ProgressParams(
                    App.getContext().getString(R.string.saving_everything),
                    App.getContext().getString(R.string.saving_photos_and_videos_with_progress, current, max),
                    100,
                    progress,
                    true,
                    true
            ));
        };

        compositeDisposable.add(
                backupContacts(vcfProgressListener, writeContactsProgressListener)
                        .doOnSuccess(params -> {
                            broadcastProgress(ProgressParams.setText(null, App.getContext().getString(R.string.saving_photos_and_videos)));
                        })
                        .flatMap(result -> usbOtgManager.writeAllToUsb(allMediaFiles, photoVideoProgressListener))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally(() -> {
                            stopForeground(true);
                            stopSelf();
                        })
                        .subscribe(
                                (copySuccessful) -> {
                                    if (copySuccessful)
                                        broadcastPopup(App.getContext().getString(R.string.content_saved), App.getContext().getString(R.string.content_saved_desc));
                                    else broadcastMessage("Backup failed.");
                                },
                                (error) -> {
                                    Timber.e(error, "Backup failed.");
                                    broadcastMessage("Backup failed.");
                                })
        );
    }

    private Single<Boolean> backupContacts(ProgressListener vcfProgressListener, ProgressListener writeUsbProgressListener) {
        Cursor contactsCursor = App.getContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

        return Single.fromCallable(() -> createVCF(contactsCursor, vcfProgressListener)) // First, create vcf strings for every contact.
                .doOnSuccess(params -> ProgressParams.setText(null, App.getContext().getString(R.string.writing_contacts)))
                .flatMap(params -> usbOtgManager.writeToUsb(params, writeUsbProgressListener)); // Then, write the resulting file to the USB storage.
    }

    /**
     * Create a *.vcf file containing all contacts data.
     */
    private UsbFileParams createVCF(Cursor contactsCursor, ProgressListener progressListener) {
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault()).format(new Date());
        String vfile = "Contacts" + "_" + timeStamp + ".vcf";

        int contactsCount = contactsCursor.getCount();

        double singleContactProgress = (double) 60 / contactsCount;
        int currentProgress;

        int lookupKeyIndex = contactsCursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
        String path = null;
        contactsCursor.moveToFirst();
        for (int i = 0; i < contactsCursor.getCount(); i++) {
            String lookupKey = contactsCursor.getString(lookupKeyIndex);
            Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
            AssetFileDescriptor fd;
            try {
                fd = App.getContext().getContentResolver().openAssetFileDescriptor(uri, "r");
                FileInputStream fis = fd.createInputStream();

                String vCard;
                long declaredLength = fd.getDeclaredLength();
                if (declaredLength != -1) {
                    byte[] buf = new byte[(int) declaredLength];
                    fis.read(buf);
                    vCard = new String(buf);
                } else {
                    vCard = Util.readFisToString(fis);
                }

                path = Environment.getExternalStorageDirectory().toString() + File.separator + vfile;
                FileOutputStream mFileOutputStream = new FileOutputStream(path, true);
                mFileOutputStream.write(vCard.getBytes());
                contactsCursor.moveToNext();
                //Timber.d("Vcard generated: #" + i);
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            currentProgress = (int) ((i + 1) * singleContactProgress);
            progressListener.onProgress(currentProgress, 100);
        }

        Uri contactURI = Uri.fromFile(new File(path));
        UsbFileParams params = new UsbFileParams(contactURI, ContentType.CONTACTS);

        return params;
    }

    /**
     * Remove all files from USB storage.
     */
    private void onClearStorageTask() {
        compositeDisposable.add(
                Single.fromCallable(() -> {
                    List<UsbFile> usbFiles;
                    usbFiles = Arrays.asList(UsbOtgManager.getInstance().getRootFiles());
                    for (int i = 0; i < usbFiles.size(); i++) {
                        Timber.e("DELETING : %s", usbFiles.get(i).getName());
                        usbFiles.get(i).delete();
                    }
                    return true;
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe((param) -> {
                            ProgressParams progressParams = ProgressParams.setText(App.getContext().getString(R.string.clearing_storage), null);
                            progressParams.setAllowCancel(false);
                            broadcastProgress(progressParams);
                        })
                        .doFinally(() -> {
                            stopForeground(true);
                            stopSelf();
                        })
                        .subscribe(
                                (clearedSuccessfully) -> {
                                    if (clearedSuccessfully)
                                        broadcastPopup(App.getContext().getString(R.string.storage_cleared), App.getContext().getString(R.string.storage_cleared_desc));
                                    else broadcastMessage("USB storage clearing failed.");
                                },
                                (error) -> {
                                    Timber.e(error, "USB storage clearing failed.");
                                    broadcastMessage(getString(R.string.storage_clearing_failed));
                                }));
    }

    /**
     * Save all photo and video files from the Android device to USB storage.
     */
    private void onBackupAllPhotoVideoTask() {
        if (!verifyConnection()) return;

        List<UsbFileParams> imageFiles = Util.getAllImagePaths(App.getContext());
        List<UsbFileParams> videoFiles = Util.getAllVideosPaths(App.getContext());

        List<UsbFileParams> allMediaFiles = new ArrayList<>();
        allMediaFiles.addAll(imageFiles);
        allMediaFiles.addAll(videoFiles);

        ProgressListener progressListener = (current, max) -> {
            broadcastProgress(new ProgressParams(
                    App.getContext().getString(R.string.photo_video_backup),
                    getString(R.string.saving_photos_and_videos_with_progress, current, max),
                    max,
                    current,
                    true,
                    true
            ));
        };

        compositeDisposable.add(
                usbOtgManager.writeAllToUsb(allMediaFiles, progressListener)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(param -> broadcastProgress(ProgressParams.setText(App.getContext().getString(R.string.photo_video_backup), App.getContext().getString(R.string.saving_photos_and_videos))))
                        .doFinally(() -> {
                            stopForeground(true);
                            stopSelf();
                        })
                        .subscribe(
                                (copySuccessful) -> {
                                    if (copySuccessful)
                                        broadcastPopup(App.getContext().getString(R.string.photos_saved), App.getContext().getString(R.string.photos_saved_desc));
                                    else
                                        broadcastMessage(App.getContext().getString(R.string.error_backup_failed));
                                },
                                (error) -> {
                                    Timber.e(error, "Backup failed.");
                                    broadcastMessage(App.getContext().getString(R.string.error_backup_failed));
                                })
        );
    }

    /**
     * Save a list of selected files to the USB storage.
     */
    private void onBackupSelectedFilesTask(final List<UsbFileParams> files, final int retryCount) {
        ProgressListener progressListener = (current, max) ->
                broadcastProgress(new ProgressParams(
                        App.getContext().getString(R.string.photo_video_backup),
                        null,
                        max,
                        current,
                        true,
                        true
                ));

        compositeDisposable.add(
                usbOtgManager.writeAllToUsb(files, progressListener)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .delaySubscription(1000, TimeUnit.MILLISECONDS) // FIXME: Delay as a workaround to let the app connect to USB storage before saving the image. Is there a better way?
                        .doOnSubscribe(param -> broadcastProgress(
                                new ProgressParams(
                                        App.getContext().getString(R.string.photo_video_backup),
                                        null,
                                        files.size(),
                                        0,
                                        true,
                                        true
                                )))
                        .doOnError(throwable -> {
                            if (retryCount > 0) {
                              isRetryInProgress = true;
                              reconnectAndWriteToUsb(files, retryCount);
                            } else {
                              isRetryInProgress = false;
                            }
                        })
                        .doFinally(() -> {
                            if (!isRetryInProgress) {
                                stopForeground(true);
                                stopSelf();
                            }
                        })
                        .subscribe(
                                (copySuccessful) -> {
                                    if (copySuccessful)
                                        broadcastPopup(App.getContext().getString(R.string.photos_saved), App.getContext().getString(R.string.photos_saved_desc));
                                    else
                                        broadcastMessage(App.getContext().getString(R.string.error_backup_failed));
                                },
                                (error) -> {
                                    Timber.e(error, "Backup failed.");
                                    broadcastMessage(App.getContext().getString(R.string.error_backup_failed));
                                })
        );
    }

    private void reconnectAndWriteToUsb(final List<UsbFileParams> fileParamsList, final int retryCount) {
        compositeDisposable.add(usbOtgManager.setupDevice()
                .delaySubscription(USB_RETRY_DELAY, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(isReady -> {
                            if (isReady) {
                                onBackupSelectedFilesTask(fileParamsList, retryCount - 1);
                            }
                        },
                        Timber::e));
    }

    /**
     * Sends a Broadcast containing info regarding the progress of the current backup operation.
     */
    private void broadcastProgress(ProgressParams progressParams) {
        // Save current progress
        progress = progressParams;

        // Don't update progress if the service is being stopped (except when we're hiding the progress dialog)
        if (!isRunning && progressParams.isShowDialog()) return;

        if (progressParams.isShowDialog()) {
            if (progressParams.getMaxProgress() > 0) {
                notificationBuilder.setProgress(progressParams.getMaxProgress(), progressParams.getCurrentProgress(), false);
            }
            if (progressParams.getTitle() != null)
                notificationBuilder.setContentTitle(progressParams.getTitle());
            if (progressParams.getText() != null)
                notificationBuilder.setContentText(progressParams.getText());
            //Timber.d("Updating notification: %s", progressParams);
            notificationManager.notify(BACKUP_NOTIFICATION_ID, notificationBuilder.build());
        }

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(BROADCAST_ACTION_PROGRESS);
        broadcastIntent.putExtra(PARAM_PROGRESS, progressParams);

        sendBroadcast(broadcastIntent);
    }

    /**
     * Sends a broadcast containing data to be shown in an alert popup dialog.
     */
    private void broadcastPopup(String title, String text) {
        if (!isRunning) return;

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(BROADCAST_ACTION_POPUP);
        broadcastIntent.putExtra(PARAM_TITLE, title);
        broadcastIntent.putExtra(PARAM_TEXT, text);

        sendBroadcast(broadcastIntent);
    }

    /**
     * Sends a broadcast containing a message to be shown e.g. in a toast or a snackbar.
     */
    private void broadcastMessage(String message) {
        if (!isRunning) return;

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(BROADCAST_ACTION_MESSAGE);
        broadcastIntent.putExtra(PARAM_MESSAGE, message);

        sendBroadcast(broadcastIntent);
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        broadcastProgress(ProgressParams.hide());
        compositeDisposable.dispose();
        Timber.d("BackupService onDestroy()");

        super.onDestroy();
    }
}
