package com.d170.storagestresstest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public class CopyFileByChannel extends AsyncTask<File,Long,Boolean> {

    private ProgressDialog mProgressDialog;
    private Context ctx;

    @Override
    protected void onPreExecute() {
        mProgressDialog = ProgressDialog.show(ctx, "Copying...", "Copying...", true);
    }

    @Override
    protected Boolean doInBackground(File... files) {
        try {
            copyFileUsingFileChannels(files[0], files[1]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        mProgressDialog.dismiss();
        // Show dialog with result
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        mProgressDialog.setMessage("Transferred " + values[0] + " bytes");
    }

    private void copyFileUsingFileChannels(File source, File dest) throws IOException {
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            inputChannel.close();
            outputChannel.close();
        }
    }

}
