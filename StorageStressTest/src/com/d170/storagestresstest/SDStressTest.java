package com.d170.storagestresstest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        mProgressDialog = new ProgressDialog(SDStressTest.this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        // mProgressDialog.setTitle("Copy File ...");
        mProgressDialog.setMessage("Transferred 0 bytes");
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMax(100);
        
        mStatusPgBr.setMax(100);
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
            CopyFileByChannelTask task = new CopyFileByChannelTask();
            task.execute(srcFile, dstFile);
        }
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

    private class CopyFileByChannelTask extends AsyncTask<File, Long, Boolean> {

        private float speed;
        private long total;
        private int progressStatus;

        @Override
        protected void onPreExecute() {
            mProgressDialog.setProgress(0);
            mProgressDialog.show();
            mStatusPgBr.setProgress(0);
        }
        
        @Override
        protected void onProgressUpdate(Long... values) {
            if (mProgressDialog!=null) {
                mProgressDialog.setProgress(progressStatus);
            }
//             mProgressDialog.setMessage("Transferred " + total + "bytes.(" +String.format("%.2f", speed) + " kb/s)");
            // long value = (long) values[0];
        }

        @Override
        protected Boolean doInBackground(File... files) {
            try {
                int count = Integer.parseInt(mExecutionCountEdTt.getText().toString());
                for (int i = 1; i < count + 1; i++) {
//                     mProgressDialog.setMessage("Copy " + i + "-th File (total"+ count + ")");
                    if (i % 2 == 1) {
                        copyFile(files[0], files[1]);
                    } else {
                        copyFile(files[1], files[0]);
                    }
                    if(mProgressDialog!=null){
                        mProgressDialog.setProgress(0);
                    }
                    
                    if(mStatusPgBr!=null){
                        mStatusPgBr.setProgress((i * 100 / count));
                    }
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
            long lenghtOfFile = source.length();

            try {
                in = new FileInputStream(source);
                out = new FileOutputStream(dest);

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len = 0;
                total = 0;
                start = System.nanoTime();
                while ((len = in.read(buf)) > 0) {
                    total += len;
                    out.write(buf, 0, len);
                    end = System.nanoTime();
                    speed = 1000000000 / (end - start);
                    start = System.nanoTime();

                    // publishProgress((total * 100 / lenghtOfFile));
                    progressStatus = (int) (total * 100 / lenghtOfFile);
                    publishProgress();
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
