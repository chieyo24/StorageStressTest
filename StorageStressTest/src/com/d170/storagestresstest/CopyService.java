package com.d170.storagestresstest;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CopyService extends IntentService {

    // private SharedPreferences mAppSettings;
    private int fileIndex = 0;
    private int progress = 0;
    private String speed = "0";
    private String avgSpeed = "0";

    public CopyService() {
        super(Constants.COPY_SERVICE);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        // String action = workIntent.getAction();
        // if (Constants.WIFI_MANUAL_SWITCH_ACTION.equals(action))
        File src = (File) workIntent.getSerializableExtra(Constants.INITIAL_SOURCE_FILE);
        File dest = (File) workIntent.getSerializableExtra(Constants.INITIAL_DESTINATION_FILE);
        int count = workIntent.getIntExtra(Constants.INITIAL_COPY_COUNT, 0);

        for (fileIndex = 1; fileIndex < count + 1; fileIndex++) {
            try {
                long start = System.nanoTime();
                if (fileIndex % 2 == 1) {
                    copyFile(src, dest);
                } else {
                    copyFile(dest, src);
                }
                Log.d("Copy a file cost ", "((((( copyFile Cost "
                        + (double) ((System.nanoTime() - start) / 1000000000.0) + " second. )))))");
            } catch (IOException e) {
                e.printStackTrace();
            }
            int v_avgSpeed = (Integer.valueOf(avgSpeed) * (fileIndex - 1) + Integer.valueOf(speed)) / fileIndex;
//            avgSpeed = String.format("%.2f", v_avgSpeed);
            avgSpeed = String.format("%d", v_avgSpeed);
            ;
        }
        Intent finishIntent = new Intent(Constants.ACTION_COPY_FINISH_REPORT);
        LocalBroadcastManager.getInstance(this).sendBroadcast(finishIntent);
    }

    private void BroadcastUpdateCopyStatus() {
        Intent copyStatusReportIntent = new Intent(Constants.ACTION_COPY_STATUS_REPORT);
        copyStatusReportIntent.putExtra(Constants.STATUS_COPY_INDEX, fileIndex);
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
            long lenghtOfFile = source.length();
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

                if (index % reportFrequency == reportFrequency - 1) {
                    end = System.nanoTime();
                    speed = String.format("%d", (1000000000 / (end - start) * (byfByte / 1024) * reportFrequency));
                    start = System.nanoTime();
                    progress = (int) (total * 100 / lenghtOfFile);
                    BroadcastUpdateCopyStatus();
                    // Log.d("BroadcastUpdateCopyStatus", "((((( update Status
                    // Cost "
                    // + (double) ((System.nanoTime() - end) / 1000000000.0) + "
                    // second. )))))");
                }
                index++;
            }
            // Log.d("BroadcastUpdateCopyStatus", "((((( total count : " + index
            // + ")))))");
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
