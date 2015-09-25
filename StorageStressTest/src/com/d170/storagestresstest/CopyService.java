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
    private double speed = 0;
    private double avgSpeed = 0;
    private double costTime_second = 0;
    private long lengthOfFile = 0;
    private boolean isShowProgress = true;

    public CopyService() {
        super(Constants.COPY_SERVICE);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        mAppSettings = getSharedPreferences(Constants.COPY_SERVICE, 0);
        File src = (File) workIntent.getSerializableExtra(Constants.INITIAL_SOURCE_FILE);
        File dest = (File) workIntent.getSerializableExtra(Constants.INITIAL_DESTINATION_FILE);
        int count = workIntent.getIntExtra(Constants.INITIAL_COPY_COUNT, 0);
        isShowProgress = mAppSettings.getBoolean(Constants.INITIAL_PROGRESS_DIALOG_EABLE, true);
        lengthOfFile = src.length();
        long start = System.nanoTime();
        for (fileIndex = 1; fileIndex < count + 1; fileIndex++) {
            while (mAppSettings.getBoolean(Constants.PREFENCES_IS_PAUSE, false)) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (fileIndex % 2 == 0 && mAppSettings.getBoolean(Constants.PREFENCES_IS_STOP, false)) {
                break;
            }
            long start_file = System.nanoTime();
            try {
                if (fileIndex % 2 == 1) {
                    copyFile(src, dest);
                } else {
                    copyFile(dest, src);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            costTime_second = (double) (System.nanoTime() - start_file) / Constants.CONSTANTS_SECONDTONANOTIME;
            speed = (double) ((lengthOfFile / 1024.0) / costTime_second);
            avgSpeed = (avgSpeed * (fileIndex - 1) + speed) / fileIndex;
            progress = 100;
            BroadcastUpdateCopyStatus(Constants.ACTION_COPY_STATUS_REPORT);
        }
        costTime_second = (double) (System.nanoTime() - start) / Constants.CONSTANTS_SECONDTONANOTIME;
        BroadcastUpdateCopyStatus(Constants.ACTION_COPY_FINISH_REPORT);
    }

    private void BroadcastUpdateCopyStatus(String action) {
        Intent copyStatusReportIntent = new Intent(action);
        copyStatusReportIntent.putExtra(Constants.STATUS_COPY_INDEX, fileIndex);
        copyStatusReportIntent.putExtra(Constants.STATUS_COPY_PROGRESS, ++progress);
        copyStatusReportIntent.putExtra(Constants.STATUS_COPY_SPEED, String.format("%.0f", speed));
        copyStatusReportIntent.putExtra(Constants.STATUS_COPY_AVGSPEED, String.format("%.0f", avgSpeed));
        copyStatusReportIntent.putExtra(Constants.STATUS_COPY_COSTTIME, String.format("%.0f", costTime_second));
        LocalBroadcastManager.getInstance(this).sendBroadcast(copyStatusReportIntent);
    }

    private void copyFile(File source, File dest) throws IOException {
        if (!dest.exists()) {
            dest.delete();
            dest.createNewFile();
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(source);
            out = new FileOutputStream(dest);
            int bufByte;
            long reportFrequency;
            long fileKbs = lengthOfFile / 1024;
            if (fileKbs <= 100) {// ~100K (no File Progress Bar)
                bufByte = 1024;// 1kb
                reportFrequency = 1 * 100; // 100 progress report
            } else if (fileKbs > 100 && fileKbs <= 1024) {// 100kb~1mb
                bufByte = 1024;// 1kb
                reportFrequency = fileKbs / 100 / 1 * 20; // 20 progress report
            } else if (fileKbs > 1024 && fileKbs <= 1024 * 100) {// 1mb~100mb
                bufByte = 1024 * 10;// 10kb
                reportFrequency = fileKbs / 100 / 10 * 10; // 10 progress report
            } else if (fileKbs > 1024 * 100 && fileKbs <= 1024 * 1024) {// 100mb~1000mb
                bufByte = 1024 * 512;// 512kb
                reportFrequency = fileKbs / 100 / 512 * 5; // 5 progress report
            } else { // 1000mb~
                bufByte = 1024 * 1024;// 1mb
                reportFrequency = fileKbs / 100 / 1024;
            }
            int len = 0;
            long total = 0;
            byte[] buf = new byte[bufByte];
            int index = 1;
            long start = System.nanoTime();
            while ((len = in.read(buf)) > 0) {
                total += len;
                out.write(buf, 0, len);
                if (isShowProgress && index % reportFrequency == 0) {
                    double costTime_second = (double) (System.nanoTime() - start)
                            / Constants.CONSTANTS_SECONDTONANOTIME;
                    speed = (len / 1024.0) / costTime_second;
                    progress = (int) (total * 100 / lengthOfFile);
                    BroadcastUpdateCopyStatus(Constants.ACTION_COPY_STATUS_REPORT);
                }
                start = System.nanoTime();
                index++;
            }
            // after copy check the file length.
            if (source.length() != dest.length()) {
                throw new Exception("Copy Fail! (The length of destination file is different with source file.)");
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
