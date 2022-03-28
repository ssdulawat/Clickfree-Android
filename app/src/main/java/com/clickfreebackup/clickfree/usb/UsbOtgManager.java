package com.clickfreebackup.clickfree.usb;

import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.clickfreebackup.clickfree.App;
import com.clickfreebackup.clickfree.ContentType;
import com.clickfreebackup.clickfree.ProgressListener;
import com.clickfreebackup.clickfree.R;
import com.clickfreebackup.clickfree.util.RxErrorUtils;
import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileStreamFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import timber.log.Timber;

public class UsbOtgManager {
  private static UsbOtgManager instance = null;
  private boolean isWriting;
  private FileSystem currentFs;
  private UsbMassStorageDevice device;
  private long size;
  private boolean stopWrite;

  private UsbOtgManager() {
  }

  public static UsbOtgManager getInstance() {
    if (instance == null) {
      synchronized (UsbOtgManager.class) {
        if (instance == null) {
          instance = new UsbOtgManager();
        }
      }
    }
    return instance;
  }

  public void stopWrite() {
    if (isWriting) {
      stopWrite = true;
    }
  }

  public boolean isBusy() {
    return isWriting;
  }

  public FileSystem getCurrentFs() {
    return currentFs;
  }

  public UsbFile[] getRootFiles() throws IOException {
    if (isConnected()) {
      return currentFs.getRootDirectory().listFiles();
    } else return null;
  }

  public boolean isConnected() {
    return currentFs != null && currentFs.getRootDirectory() != null;
  }

  public Flowable<Boolean> setupDevice() {
    Timber.d("UsbOtgManager.setupDevice()");
    return Flowable.fromPublisher(publisher -> {
      try {
        final UsbMassStorageDevice[] massStorageDevices =
                UsbMassStorageDevice.getMassStorageDevices(App.getContext());
        if (massStorageDevices.length > 0 && massStorageDevices[0].getPartitions().size() == 0) {
          device = massStorageDevices[0];
          device.init();
          {
            Timber.d("Vendor ID: %s", device.getUsbDevice().getVendorId());
            Timber.d("Product ID: %s", device.getUsbDevice().getProductId());

            // we always use the first partition of the device
            currentFs = device.getPartitions().get(0).getFileSystem();
            Timber.d("CurrentFs initialized");

            Timber.d("Capacity: %s", currentFs.getCapacity());
            Timber.d("Occupied Space: %s", currentFs.getOccupiedSpace());
            Timber.d("Free Space: %s", currentFs.getFreeSpace());
            Timber.d("Chunk size: %s", currentFs.getChunkSize());

            publisher.onNext(true);
          }
        }
      } catch (Throwable e) {
        Toast.makeText(App.getContext(), App.getContext().getString(R.string.error_setting_up), Toast.LENGTH_SHORT).show();
        Timber.e(e, "Error setting up device");
        publisher.onNext(false);
      }
    });
  }

  public Single<Boolean> writeToUsb(UsbFileParams fileParams, ProgressListener progressListener) {
    stopWrite = false;
    isWriting = true;

    return Single.fromCallable(() -> {
      Uri uri = fileParams.getUri();
      long startTime = System.currentTimeMillis();

      size = getFileSize(uri);

      String name = uri.getLastPathSegment();

      Timber.d("Copying file to USB storage: %s", name);


      UsbFile testDirectory;

      String directoryName = fileParams.getContentType().dirName;
      testDirectory = currentFs.getRootDirectory().search(directoryName);
      if (testDirectory == null) {
        testDirectory = currentFs.getRootDirectory().createDirectory(directoryName);
      }

      // Check whether the file is already backed up.
      UsbFile file = testDirectory.search(name);
      if (file == null) {
        file = testDirectory.createFile(name);
      } else return true;

      if (size > 0) {
        file.setLength(size);
      }

      InputStream inputStream = new BufferedInputStream(App.getContext().getContentResolver().openInputStream(uri));
      OutputStream outputStream = UsbFileStreamFactory.createBufferedOutputStream(file, currentFs);

      try {
        byte[] bytes = new byte[currentFs.getChunkSize()];
        int count;
        long bytesWritten = 0;

        while ((count = inputStream.read(bytes)) != -1) {

          if (stopWrite) {
            Timber.d("Copying interrupted!");
            file.delete();
            break;
          }

          outputStream.write(bytes, 0, count);

          bytesWritten += count;
          int progress = (int) (((double) bytesWritten / (double) size) * 100.0);
          if (progressListener != null) progressListener.onProgress(progress, 100);
        }

        outputStream.close();
        inputStream.close();
        Timber.d("Closed streams()");
      } catch (IOException e) {
        Timber.e(e, "Error copying!");
        return false;
      } finally {
        isWriting = false;
      }
      Timber.d("copy time: %s", (System.currentTimeMillis() - startTime));

      // return task result as "false" if the task was interrupted by 'stopWrite'
      return !stopWrite;
    })
            .retry(1)
            .doOnSubscribe((param) -> isWriting = true)
            .doOnDispose(() -> Timber.d("doOnDispose()"))
            .doFinally(() -> {
                      Timber.d("doFinally()");
                      isWriting = false;
                    }
            );
  }

  private Single<Boolean> writeFacebookPhotoToUsb(UsbFileParams fileParams) {
    stopWrite = false;
    isWriting = true;

    RxErrorUtils.setErrorHandler();

    return Single.fromCallable(() -> {
      final Uri uri = fileParams.getUri();
      final long startTime = System.currentTimeMillis();

      size = getFileSize(uri);

      final String name = getFileName(fileParams);

      Timber.d("Copying file to USB storage: %s", name);

      final UsbFile testDirectory = getTestDirectory(fileParams);

      // Check whether the file is already backed up.
      if (testDirectory != null) {
        UsbFile file = testDirectory.search(name);
        if (file == null) {
          file = testDirectory.createFile(name);
        } else return true;

        if (size > 0) {
          file.setLength(size);
        }

        final InputStream inputStream = new BufferedInputStream(new URL(uri.toString()).openStream());
        final OutputStream outputStream = UsbFileStreamFactory.createBufferedOutputStream(file, currentFs);

        try {
          byte[] bytes = new byte[currentFs.getChunkSize()];
          int count;
          long bytesWritten = 0;

          while ((count = inputStream.read(bytes)) != -1) {

            if (stopWrite) {
              Timber.d("Copying interrupted!");
              file.delete();
              break;
            }

            outputStream.write(bytes, 0, count);

            bytesWritten += count;
            int progress = (int) (((double) bytesWritten / (double) size) * 100.0);
          }

          outputStream.close();
          inputStream.close();
          Timber.d("Closed streams()");
        } catch (IOException e) {
          Timber.e(e, "Error copying!");
          return false;
        } finally {
          isWriting = false;
        }
        Timber.d("copy time: %s", (System.currentTimeMillis() - startTime));
      }
      // return task result as "false" if the task was interrupted by 'stopWrite'
      return !stopWrite;
    })
            .retry(1)
            .doOnSubscribe((param) -> isWriting = true)
            .doOnDispose(() -> Timber.d("doOnDispose()"))
            .doFinally(() -> {
                      Timber.d("doFinally()");
                      isWriting = false;
                    }
            );
  }

  @Nullable
  private UsbFile getTestDirectory(UsbFileParams fileParams) {
    final ContentType contentType = fileParams.getContentType();
    final String dirName = contentType.dirName;
    final UsbFile rootDirectory = currentFs.getRootDirectory();
    UsbFile testDirectory;
    try {
      if (contentType.equals(ContentType.INSTAGRAM_PHOTO) || contentType.equals(ContentType.SELECTED_INSTAGRAM_PHOTO) || contentType.equals(ContentType.SELECTED_FACEBOOK_PHOTO)) {
        testDirectory = rootDirectory.search(dirName);
        if (testDirectory == null) {
          testDirectory = rootDirectory.createDirectory(dirName);
        }

      } else {
        UsbFile facebookDirectory = rootDirectory.search(dirName);
        if (facebookDirectory == null) {
          facebookDirectory = rootDirectory.createDirectory(dirName);
        }

        UsbFile facebookSubdirectory = facebookDirectory.search(fileParams.getFolder());
        if (facebookSubdirectory == null) {
          facebookSubdirectory = facebookDirectory.createDirectory(fileParams.getFolder());
        }

        testDirectory = facebookSubdirectory;
      }
    } catch (IOException e) {
      Timber.e(e, "Error copying!");
      testDirectory = null;
    }
    return testDirectory;
  }

  private String getFileName(UsbFileParams fileParams) {
    if (fileParams.getContentType().equals(ContentType.INSTAGRAM_PHOTO)
            || fileParams.getContentType().equals(ContentType.SELECTED_INSTAGRAM_PHOTO)) {
      return fileParams.getUri().getLastPathSegment();
    } else {
      return fileParams.getId().split("\\?")[0];
    }
  }

  public Single<Boolean> writeAllToUsb(List<UsbFileParams> fileParamsList, ProgressListener progressListener) {
    stopWrite = false;
    AtomicInteger counter = new AtomicInteger();

    return Observable.fromIterable(fileParamsList)
            .flatMap(usbFileParams -> writeToUsb(usbFileParams, null).toObservable())
            .takeWhile((param) -> !stopWrite)
            .doOnNext(writeResult -> {
              counter.getAndIncrement();
              progressListener.onProgress(counter.get(), fileParamsList.size());

              Timber.d("File written: " + counter.get() + " of " + fileParamsList.size());

              if (!writeResult) {
                throw new IllegalStateException("USB write failed.");
              }
            })
            .lastOrError()
            .map(result -> {
              // return task result as "false" if the task was interrupted by 'stopWrite'
              if (stopWrite) return false;
              else return result;
            });
  }

  public Single<Boolean> writeFacebookPhotoToUsb(List<UsbFileParams> fileParamsList,
                                                 ProgressListener progressListener) {
    stopWrite = false;
    final AtomicInteger counter = new AtomicInteger();

    return Observable.fromIterable(fileParamsList)
            .flatMap(usbFileParams -> writeFacebookPhotoToUsb(usbFileParams).toObservable())
            .takeWhile((param) -> !stopWrite)
            .doOnNext(writeResult -> {
              counter.getAndIncrement();
              progressListener.onProgress(counter.get(), fileParamsList.size());

              Timber.d("File written: " + counter.get() + " of " + fileParamsList.size());

              if (!writeResult) {
                throw new IllegalStateException("USB write failed.");
              }
            })
            .lastOrError()
            .map(result -> {
              // return task result as "false" if the task was interrupted by 'stopWrite'
              if (stopWrite) return false;
              else return result;
            });
  }

  private long getFileSize(Uri uri) {
    File file = new File(uri.getPath());
    return file.length();
  }

  public void disconnect() {
    Timber.d("UsbOtgManager.disconnect()");
    if (device != null) {
      device.close();
      device = null;
    }
    currentFs = null;
  }
}
