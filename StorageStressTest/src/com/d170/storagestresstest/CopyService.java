package com.d170.storagestresstest;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CopyService extends IntentService {

    private SharedPreferences mAppSettings;
    private int fileIndex = 0;
    private int progress = 0;
    private String speed = "0";
    private String avgSpeed = "0";
    private String fileSpeed = "0";
    private boolean isShowProgressDialog;
    private long lenghtOfFile;

    public CopyService() {
        super(Constants.COPY_SERVICE);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        mAppSettings = getSharedPreferences(Constants.COPY_SERVICE, 0);
        // String action = workIntent.getAction();
        // if (Constants.WIFI_MANUAL_SWITCH_ACTION.equals(action))
        File src = (File) workIntent.getSerializableExtra(Constants.INITIAL_SOURCE_FILE);
        File dest = (File) workIntent.getSerializableExtra(Constants.INITIAL_DESTINATION_FILE);
        int count = workIntent.getIntExtra(Constants.INITIAL_COPY_COUNT, 0);
        lenghtOfFile = src.length();
                
        for (fileIndex = 1; fileIndex < count + 1; fileIndex++) {
            while (mAppSettings.getBoolean(Constants.PREFENCES_IS_PAUSE, false)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (mAppSettings.getBoolean(Constants.PREFENCES_IS_STOP, false)) {
                BroadcastUpdateCopyStatus(Constants.ACTION_COPY_FINISH_REPORT);
            }
            isShowProgressDialog = mAppSettings.getBoolean(Constants.PREFENCES_IS_SHOW_PROGRESSDIALOG, true);
            long start = System.nanoTime();
            try {
                if (fileIndex % 2 == 1) {
                    copyFile(src, dest);
                } else {
                    copyFile(dest, src);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            float v_fileSpeed=(float) ((System.nanoTime() - start) / 1000000000);
            fileSpeed = String.valueOf(v_fileSpeed);
//            Log.d("Copy a file cost ", "((((( copyFile Cost " + fileSpeed + " second. )))))");
            speed = String.format("%d", (int)(lenghtOfFile/1024/v_fileSpeed));
            int v_avgSpeed = (Integer.valueOf(avgSpeed) * (fileIndex - 1) + Integer.valueOf(speed)) / fileIndex;
            avgSpeed = String.format("%d", v_avgSpeed);
            progress=100;
            BroadcastUpdateCopyStatus(Constants.ACTION_COPY_STATUS_REPORT);
        }
        BroadcastUpdateCopyStatus(Constants.ACTION_COPY_FINISH_REPORT);
    }

    private void BroadcastUpdateCopyStatus(String action) {
        Intent copyStatusReportIntent = new Intent(action);
        copyStatusReportIntent.putExtra(Constants.STATUS_COPY_INDEX, fileIndex);
        copyStatusReportIntent.putExtra(Constants.STATUS_COPY_FILESPEED, fileSpeed);
        copyStatusReportIntent.putExtra(Constants.STATUS_COPY_PROGRESS, progress);
        copyStatusReportIntent.putExtra(Constants.STATUS_COPY_SPEED, speed);
        copyStatusReportIntent.putExtra(Constants.STATUS_COPY_AVGSPEED, avgSpeed);
        LocalBroadcastManager.getInstance(this).sendBroadcast(copyStatusReportIntent);
    }

    private void copyFile(File source, File dest) throws IOException {
        if (!dest.exists()) {
            // dest.delete();
            dest.createNewFile();
        }

        InputStream in = null;
        OutputStream out = null;

        try {
            in = new FileInputStream(source);
            out = new FileOutputStream(dest);

            int index = 1;
            int byfByte;
            long reportFrequency;
            long fileKbs = lenghtOfFile / 1024;
            if (fileKbs <= 100) {// ~100K
                byfByte = 1024;// 1kb
                reportFrequency = 1;
            } else if (fileKbs > 100 && fileKbs <= 1024) {// 100kb~1mb
                byfByte = 1024;// 1kb
                reportFrequency = fileKbs / 100;
            } else if (fileKbs > 1024 && fileKbs <= 1024 * 100) {// 1mb~100mb
                byfByte = 1024 * 10;// 10kb
                reportFrequency = fileKbs / 100 / 10;
            } else if (fileKbs > 1024 * 100 && fileKbs <= 1024 * 1024) {// 100~1000mb
                byfByte = 1024 * 512;// 512kb
                reportFrequency = fileKbs / 100 / 512;
            } else { // 1000mb~
                byfByte = 1024 * 1024;// 1mb
                reportFrequency = fileKbs / 100 / 1024;
            }

            int len = 0;
            long total = 0;
            byte[] buf = new byte[byfByte];
            long start = System.nanoTime();
            long end;
            while ((len = in.read(buf)) > 0) {
                total += len;
                out.write(buf, 0, len);
                if (isShowProgressDialog && index % reportFrequency == reportFrequency - 1) {
                    end = System.nanoTime();
                    speed = String.format("%d", (1000000000 / (end - start) * (byfByte / 1024) * reportFrequency));
                    start = System.nanoTime();
                    progress = (int) (total * 100 / lenghtOfFile);
                    BroadcastUpdateCopyStatus(Constants.ACTION_COPY_STATUS_REPORT);
                }
                index++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }
}
