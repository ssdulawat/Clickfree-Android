package com.clickfreebackup.clickfree.usb;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.clickfreebackup.clickfree.data.BitmapDao;
import com.clickfreebackup.clickfree.data.ThumbnailRoomDatabase;
import com.clickfreebackup.clickfree.model.BitmapThumbnail;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileStreamFactory;

import org.reactivestreams.Publisher;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;

import io.reactivex.BackpressureOverflowStrategy;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class BitmapHandler {
    private final BitmapDao bitmapDao;
    private final LruCache<String, Bitmap> memoryCache;
    private CompositeDisposable compositeDisposable;
    private final FileSystem currentFs;
    private boolean idleScrollState = true;

    public BitmapHandler(final Context context, final FileSystem currentFs) {
        this.currentFs = currentFs;
        bitmapDao = ThumbnailRoomDatabase.getDatabase(context).bitmapDao();
        compositeDisposable = new CompositeDisposable();
        final int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 1024) / 8;
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    private void saveBitmap(final String imageName, final Bitmap bitmap, final Context context) {
        final String THUMBNAIL_PATH = "thumbnails";
        final String thumbnailFolderPath = context.getFilesDir().toString() + "/" + THUMBNAIL_PATH;
        createThumbnailFolder(thumbnailFolderPath);

        final File bitmapThumbnailFile = new File(thumbnailFolderPath + "/", imageName);
        if (bitmapThumbnailFile.exists()) {
            return;
        }

        final File writtenBitmapThumbnail = writeBitmapThumbnailToFile(bitmap, new File(thumbnailFolderPath + "/", imageName));

        saveThumbnailDataToDB(imageName, writtenBitmapThumbnail.getAbsolutePath());
    }

    private void saveThumbnailDataToDB(final String imageName, final String absolutePath) {
        ThumbnailRoomDatabase.databaseWriteExecutor.execute(() -> bitmapDao.insert(new BitmapThumbnail(imageName, absolutePath)));
    }

    private File writeBitmapThumbnailToFile(final Bitmap bitmap, final File bitmapFile) {

        try {
            final FileOutputStream out = new FileOutputStream(bitmapFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);

            out.flush();
            out.close();
        } catch (Exception e) {
            Timber.e(e);
        }
        return bitmapFile;
    }

    private void createThumbnailFolder(final String thumbnailFolderPath) {
        final File thumbnailFolder = new File(thumbnailFolderPath);
        if (!thumbnailFolder.exists()) {
            boolean mkdir = thumbnailFolder.mkdir();
            Timber.d("Thumbnail folder creating - %s", mkdir);
        }
    }

    private Single<BitmapThumbnail> getBitmapThumbnail(final String imageName) {
        return bitmapDao.findBitmapThumbnailByName(imageName);
    }


    public void loadBitmap(final Context context, final UsbFile file, final ImageView imageView) {
        final Bitmap bitmap = getBitmapFromMemCache(file.getName());
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            getBitmapFromDB(context, file, imageView);
        }
    }

    private Bitmap getBitmapFromMemCache(final String key) {
        return memoryCache.get(key);
    }

    private void getBitmapFromDB(final Context context, final UsbFile file, final ImageView imageView) {
        if (idleScrollState) {

            final Disposable disposable = getBitmapThumbnail(file.getName())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            bitmapThumbnail ->
                                    handleBitmapFromDBResponse(context, file, bitmapThumbnail, imageView),
                            throwable -> {
                                Timber.e(throwable);
                                bitmapFromUsbFile(context, file, imageView);
                            });

            handleDisposible(disposable);
        }
    }

    private void handleBitmapFromDBResponse(final Context context, final UsbFile file, final BitmapThumbnail bitmapThumbnail, final ImageView imageView) {
        if (bitmapThumbnail != null) {
            Glide.with(context)
                    .load(bitmapThumbnail.imageUrl)
                    .into(imageView);
        } else {
            bitmapFromUsbFile(context, file, imageView);
        }
    }

    private void bitmapFromUsbFile(final Context context, final UsbFile file, final ImageView imageView) {
        final Disposable disposable = createFlowable(file)
                .onBackpressureBuffer(127, () -> {
                }, BackpressureOverflowStrategy.DROP_LATEST)

                .flatMap((Function<UsbFile, Publisher<Bitmap>>) usbFile -> sendRequest(context, usbFile))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bitmap -> {
                            if (bitmap != null) {
                                imageView.setImageBitmap(bitmap);
                                addBitmapToMemoryCache(file.getName(), bitmap);
                            }
                        }
                        , Timber::e);

        handleDisposible(disposable);
    }

    private void handleDisposible(final Disposable disposable) {
        compositeDisposable.add(disposable);
    }

    private synchronized Flowable<UsbFile> createFlowable(final UsbFile usbFile) {
        return Flowable.create(emitter -> {
            if (emitter != null && usbFile != null) {
                emitter.onNext(usbFile);
            }
        }, BackpressureStrategy.BUFFER);
    }

    private Publisher<Bitmap> sendRequest(final Context context, final UsbFile usbFile) {
        return s -> s.onNext(getBitmap(context, usbFile));
    }

    private void addBitmapToMemoryCache(final String key, final Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            memoryCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmap(final Context context, final UsbFile usbFile) {
        final BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inJustDecodeBounds = true;
        bitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;

        BitmapFactory.decodeStream(UsbFileStreamFactory.createBufferedInputStream(usbFile, currentFs), null, bitmapOptions);

        final int REQUIRED_WIDTH = 12;
        final int REQUIRED_HIGHT = 12;
        int scale = 1;
        while (bitmapOptions.outWidth / scale / 2 >= REQUIRED_WIDTH && bitmapOptions.outHeight / scale / 2 >= REQUIRED_HIGHT)
            scale *= 2;

        final BitmapFactory.Options finalBitmapOptions = new BitmapFactory.Options();
        finalBitmapOptions.inSampleSize = scale;
        final BufferedInputStream bufferedInputStream = UsbFileStreamFactory.createBufferedInputStream(usbFile, currentFs);
        final Bitmap bitmap = BitmapFactory.decodeStream(bufferedInputStream, null, finalBitmapOptions);

        if (bitmap != null) {
            saveBitmap(usbFile.getName(), bitmap, context);
            return bitmap;
        } else {
            Timber.e("Bitmap Null pointer exception");
            return null;
        }
    }

    public void setScrollState(boolean scrollState) {
        idleScrollState = scrollState;
        if (scrollState) {
            compositeDisposable.clear();
        }
    }

    public void onDestroyActivity() {
        compositeDisposable.clear();
        compositeDisposable.dispose();
        compositeDisposable = null;
    }
}
