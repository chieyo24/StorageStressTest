package com.d170.storagestresstest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import br.com.thinkti.android.filechooser.FileChooser;
import com.d170.storagestresstest.R;

public class SDStressTest extends Activity {

    private TextView mSrcFileEdTt;
    private TextView mDestFileEdTt;
    private EditText mExecutionCountEdTt;
    private ProgressBar mStatusPgBr;

    private ProgressDialog mProgressDialog;
    private Toast mToast;
    private LocalBroadcastManager mLocalBroadcastManager;

    /**
     * show user a warning Toast message
     * 
     * @param msg
     *            message
     */
    private void showToast(String msg) {
        mToast.setText(msg);
        mToast.setDuration(Toast.LENGTH_SHORT);
        mToast.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sdstress_test);

        mSrcFileEdTt = (TextView) findViewById(R.id.srcFileEdTt);
        mDestFileEdTt = (TextView) findViewById(R.id.destFileEdTt);
        mExecutionCountEdTt = (EditText) findViewById(R.id.executionCountEdTt);
        mStatusPgBr = (ProgressBar) findViewById(R.id.statusPgBr);
        mStatusPgBr.setMax(100);

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        createProgressDialog("");

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_COPY_STATUS_REPORT);
        filter.addAction(Constants.ACTION_COPY_FINISH_REPORT);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long start = System.nanoTime();

                if (intent.getAction().equals(Constants.ACTION_COPY_STATUS_REPORT)) {
                    int fileIndex = intent.getIntExtra(Constants.STATUS_COPY_INDEX, 1);
                    int progress = intent.getIntExtra(Constants.STATUS_COPY_PROGRESS, 0);
                    String speed = intent.getStringExtra(Constants.STATUS_COPY_SPEED);
                    String avgSpeed = intent.getStringExtra(Constants.STATUS_COPY_AVGSPEED);

                    // if (mProgressDialog == null) {
                    // createProgressDialog("copy " + fileIndex + "-th \n" +
                    // "Speed : " + speed + "(kb/s) Avg-Speed : "
                    // + avgSpeed + "(kb/s)");
                    // }
                    mProgressDialog.setMessage("copy " + fileIndex + "-th \n" + "Speed : " + speed
                            + "(kb/s) \n Avg-Speed : " + avgSpeed + "(kb/s)");
                    mProgressDialog.setProgress(progress);

                    if (!mProgressDialog.isShowing()) {
                        mProgressDialog.show();
                    }

                    mStatusPgBr.setProgress(
                            (fileIndex * 100) / Integer.parseInt(mExecutionCountEdTt.getText().toString()));

                } else if (intent.getAction().equals(Constants.ACTION_COPY_FINISH_REPORT)) {
                    if (mProgressDialog != null) {
                        if (mProgressDialog.isShowing()) {
                            try {
                                mProgressDialog.dismiss();
                            } catch (Exception e) {
                                //doNotthing
                            }
                        }
                    }
                }
//                Log.d("BroadcastUpdateCopyStatus", "((((( onReceive Cost "
//                        + (double) ((System.nanoTime() - start) / 1000000000.0) + " second. )))))");
            }
        };
        mLocalBroadcastManager.registerReceiver(receiver, filter);
    }

    private void createProgressDialog(String msg) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }

        mProgressDialog = new ProgressDialog(SDStressTest.this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        if (msg.length() > 0) {
            mProgressDialog.setMessage(msg);
        }
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMax(100);
    }

    @Override
    protected void onDestroy() {
        if (mProgressDialog != null) {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
        }
        super.onDestroy();
    }

    /**
     * Start Button Click
     */
    public void doStartProcess(View v) {
        File srcFile = new File(mSrcFileEdTt.getText().toString());
        File dstFile = new File(mDestFileEdTt.getText().toString());

        if (!srcFile.isFile()) {
            showToast("Please Choose a file for copy Source !");
        } else if (dstFile.isDirectory()) {
            showToast("Please Choose a file for copy Destination !");
            // }else if(mExecutionCountEdTt.getText()){
        } else {
            // CopyFileTask task = new CopyFileTask();
            // task.execute(srcFile, dstFile);
            CopyFileService(srcFile, dstFile);

        }
    }

    private void CopyFileService(File src, File dest) {
        Intent mServiceIntent = new Intent(this, CopyService.class);
        mServiceIntent.putExtra(Constants.INITIAL_SOURCE_FILE, src);
        mServiceIntent.putExtra(Constants.INITIAL_DESTINATION_FILE, dest);
        mServiceIntent.putExtra(Constants.INITIAL_COPY_COUNT,
                Integer.parseInt(mExecutionCountEdTt.getText().toString()));
        startService(mServiceIntent);
    }

    /**
     * Stop Button Click
     */
    public void doStopProcess(View v) {
        if (mProgressDialog != null) {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
        }
    }

    /**
     * chooser Button Click
     */
    public void doFileChooser(View v) {
        ImageButton bt = (ImageButton) v;
        Intent intent = new Intent(this, FileChooser.class);
        startActivityForResult(intent, bt.getId());

    }

    /**
     * chooser Button Click
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String fileSelected = data.getStringExtra("fileSelected");
            switch (requestCode) {
            case R.id.srcChooserBt:
                mSrcFileEdTt.setText(fileSelected);
                break;
            case R.id.destChooserBt:
                mDestFileEdTt.setText(fileSelected);
                break;
            default:
                // doNotThing
                break;
            }
        }
    }

    private class CopyFileTask extends AsyncTask<File, Integer, Boolean> {

        private float speed;
        // private int progressStatus;
        private int copyCount;
        private int fileIndex;

        @Override
        protected void onPreExecute() {
            copyCount = Integer.parseInt(mExecutionCountEdTt.getText().toString());

            mProgressDialog.setProgress(0);
            mProgressDialog.show();
            mStatusPgBr.setProgress(0);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

            if (values[0] == 0) {
                // createProgressDialog("Copy " + fileIndex + "-th File (Speed :
                // " + String.format("%.2f", speed) +
                // "; total :"+ copyCount + ")");
                // mProgressDialog.setProgress(values[0]);
                // mProgressDialog.show();

                if (mStatusPgBr != null) {
                    mStatusPgBr.setProgress((fileIndex * 100 / copyCount));
                }
            }
            mProgressDialog.setProgress(values[0]);

            // if (mProgressDialog != null) {
            // mProgressDialog.setProgress(progressStatus);
            // } else {
            // mProgressDialog.setProgress(progressStatus);
            // mProgressDialog.show();
            // }
        }

        @Override
        protected Boolean doInBackground(File... files) {
            try {
                for (fileIndex = 1; fileIndex < copyCount + 1; fileIndex++) {
                    long start = System.nanoTime();
                    if (fileIndex % 2 == 1) {
                        copyFile(files[0], files[1]);
                    } else {
                        copyFile(files[1], files[0]);
                    }
                    publishProgress(mProgressDialog.getMax());
                    Log.d("Copy a file cost ", "((((( copyFile Cost "
                            + (double) ((System.nanoTime() - start) / 1000000000.0) + " second. )))))");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mProgressDialog.dismiss();
        }

        private void copyFile(File source, File dest) throws IOException {
            if (!dest.exists()) {
                // dest.delete();
                dest.createNewFile();
            }
            InputStream in = null;
            OutputStream out = null;
            long start;
            long end;
            long total;
            long lenghtOfFile = source.length();
            try {
                in = new FileInputStream(source);
                out = new FileOutputStream(dest);
                byte[] buf = new byte[1024];
                int len = 0;
                total = 0;
                start = System.nanoTime();
                int index = 0;
                while ((len = in.read(buf)) > 0) {
                    total += len;
                    out.write(buf, 0, len);
                    if (index == 0 || index % 512 == 0) {
                        end = System.nanoTime();
                        speed = 1000000000 / (end - start);
                        start = System.nanoTime();
                        if (index == 0) {
                            publishProgress(0);
                        } else {
                            publishProgress((int) (total * 100 / lenghtOfFile));
                        }
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
}
