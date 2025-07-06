package com.github.bytesculptor07.quillo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class InternalFileDownloader {

    // Callback interface to notify when the file download is complete
    public interface DownloadCallback {
        void onDownloadComplete(File downloadedFile);
        void onDownloadFailed(String error);
    }

    public static void downloadFileToInternal(Context context, String fileUrl, String infoUrl, String fileName, ProgressBar progressBar, TextView resultText, DownloadCallback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            float totalSize = -1;
            try {
                // Open connection to the info URL to get the file size
                URL url2 = new URL(infoUrl);
                HttpURLConnection connection2 = (HttpURLConnection) url2.openConnection();
    
                connection2.setRequestMethod("GET");
                connection2.connect();
    
                int responseCode = connection2.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection2.getInputStream()));
                    String line;
                    long size = -1;
    
                    // Parse the pointer file
                    while ((line = reader.readLine()) != null) {
                            final String lllln = line;
                        new Handler(Looper.getMainLooper()).post(() -> resultText.setText(lllln)); /////
                        if (line.startsWith("size")) {
                            size = Long.parseLong(line.split(" ")[1]);
                            break;
                        }
                    }
                    reader.close();
                    connection2.disconnect();
    
                    if (size != -1) {
                        System.out.println("File size from LFS metadata: " + size + " bytes");
                            final String rrrTx = "File size from LFS metadata: " + size + " bytes"; /////
                            new Handler(Looper.getMainLooper()).post(() -> resultText.setText(rrrTx)); /////
                        totalSize = (float) size;
                    } else {
                        String errorMessage = "Failed to get file size: Size information not found in the LFS pointer file.";
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (callback != null) {
                                callback.onDownloadFailed(errorMessage);
                            }
                        });
                    }
                } else {
                    String errorMessage = "Failed to get filesize: " + responseCode;
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (callback != null) {
                                callback.onDownloadFailed(errorMessage);
                            }
                        });
                }
                // Open connection to the file URL
                URL url = new URL(fileUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                // Check for success response
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    // Get total file size
                    //int totalSize = connection.getContentLength();
                    int downloadedSize = 0;

                    // Get input stream
                    InputStream inputStream = new BufferedInputStream(connection.getInputStream());

                    // Create file in internal storage
                    File outputFile = new File(context.getFilesDir(), fileName);
                    FileOutputStream outputStream = new FileOutputStream(outputFile);

                    // Buffer to hold data during download
                    byte[] buffer = new byte[8192];
                    int bytesRead;

                    // Write data to the file and track progress
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        downloadedSize += bytesRead;

                        // Update progress on the main thread
                        final int progress = (int) ((downloadedSize / totalSize) * 100);
                        new Handler(Looper.getMainLooper()).post(() -> progressBar.setProgress(progress));
                        final String resText = String.valueOf(downloadedSize) + " / " + String.valueOf(totalSize);
                        new Handler(Looper.getMainLooper()).post(() -> resultText.setText(resText));
                    }

                    // Close streams
                    outputStream.close();
                    inputStream.close();

                    // Notify the callback about the download completion
                    new Handler(Looper.getMainLooper()).post(() -> {
                        progressBar.setProgress(100);  // Complete progress
                        if (callback != null) {
                            callback.onDownloadComplete(outputFile);
                        }
                    });
                } else {
                    String errorMessage = "Failed to download file: HTTP " + connection.getResponseCode();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (callback != null) {
                            callback.onDownloadFailed(errorMessage);
                        }
                    });
                }
            } catch (Exception e) {
                String errorMessage = "Download failed: " + e.getMessage();
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) {
                        callback.onDownloadFailed(errorMessage);
                    }
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }
}