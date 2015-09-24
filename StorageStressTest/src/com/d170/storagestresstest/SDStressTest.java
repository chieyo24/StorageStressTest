package com.d170.storagestresstest;

import java.io.File;

import br.com.thinkti.android.filechooser.FileChooser;
import com.d170.storagestresstest.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SDStressTest extends Activity {

    private TextView mSrcFileEdTt;
    private TextView mDestFileEdTt;
    private EditText mExecutionCountEdTt;
    private Button mStartBt;
    private Button mStopBt;
    private ProgressBar mStatusPgBr;
    private Switch mProgressDialogSW;
    private TextView mStatusMesasgeTV;
    private ImageButton mSrcChooserBt;
    private ImageButton mDestChooserBt;

    private ProgressDialog mProgressDialog;
    private Toast mToast;
    private LocalBroadcastManager mLocalBroadcastManager;
    private SharedPreferences mAppSettings;
    private String mUIMode;

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
        mSrcChooserBt = (ImageButton) findViewById(R.id.srcChooserBt);
        mDestChooserBt = (ImageButton) findViewById(R.id.destChooserBt);
        mExecutionCountEdTt = (EditText) findViewById(R.id.executionCountEdTt);
        mStartBt = (Button) findViewById(R.id.startBt);
        mStopBt = (Button) findViewById(R.id.stopBt);
        mProgressDialogSW = (Switch) findViewById(R.id.progressDialogSW);
        mStatusMesasgeTV = (TextView) findViewById(R.id.statusMesasgeTV);
        mStatusPgBr = (ProgressBar) findViewById(R.id.statusPgBr);
        mStatusPgBr.setMax(100);

        // portrait & landscape orientation should be initial
        createProgressDialog();
        mAppSettings = getSharedPreferences(Constants.COPY_SERVICE, 0);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_COPY_STATUS_REPORT);
        filter.addAction(Constants.ACTION_COPY_FINISH_REPORT);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int fileIndex = intent.getIntExtra(Constants.STATUS_COPY_INDEX, 1);
                int progress = intent.getIntExtra(Constants.STATUS_COPY_PROGRESS, 0);
                int speed = Integer.valueOf(intent.getStringExtra(Constants.STATUS_COPY_SPEED));
                int avgSpeed = Integer.valueOf(intent.getStringExtra(Constants.STATUS_COPY_AVGSPEED));
                long costTime = Long.valueOf(intent.getStringExtra(Constants.STATUS_COPY_COSTTIME));

                if (intent.getAction().equals(Constants.ACTION_COPY_STATUS_REPORT)) {
                    if (mProgressDialogSW.isChecked()) {
                        mProgressDialog.setMessage("copy " + fileIndex + "-th \n" // fileIndex
                                + "Speed : " + speed + "(kb/s)"); // speed
                        Log.d("onReceive", "Copy progress (" + progress + ")");
                        mProgressDialog.setProgress(progress);
                        if (!mProgressDialog.isShowing()) {
                            mProgressDialog.show();
                        }
                    }
                    int totalFileCount = Integer.parseInt(mExecutionCountEdTt.getText().toString());
                    mStatusPgBr.setProgress((fileIndex * 100) / totalFileCount);
                    if (fileIndex > 1) {// avg speed generated after first file
                                        // copy finish.
                        long remainingSeconds = costTime * (totalFileCount - fileIndex + 1);
                        String remainingTime = "";
                        remainingTime = " ; Remaining time : " + parserSecond2Time(remainingSeconds);
                        mStatusMesasgeTV.setText("Avg-Speed : " + avgSpeed + "(kb/s)" + remainingTime);
                    }
                } else if (intent.getAction().equals(Constants.ACTION_COPY_FINISH_REPORT)) {
                    if (mProgressDialog != null) {
                        if (mProgressDialog.isShowing()) {
                            mProgressDialog.hide();
                        }
                    }
                    mStatusMesasgeTV.setText(
                            "Avg-Speed : " + avgSpeed + "(kb/s) ; Total Cost Time : " + parserSecond2Time(costTime));
                    updateComponentEnable(Constants.UI_INITIAL);
                    // send mail
                    Intent mailIntent = new Intent(Intent.ACTION_SENDTO);
                    mailIntent.putExtra(Intent.EXTRA_EMAIL, Constants.MAIL_TO.split(";"));
                    mailIntent.putExtra(Intent.EXTRA_CC, Constants.MAIL_CC.split(";"));
                    mailIntent.putExtra(Intent.EXTRA_SUBJECT, "SD Strss Test Report");
                    mailIntent.putExtra(Intent.EXTRA_TEXT, "Success!!");
                    if (mailIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mailIntent);
                    }
                }
            }
        };
        mLocalBroadcastManager.registerReceiver(receiver, filter);
    }

    private void createProgressDialog() {
        mProgressDialog = new ProgressDialog(SDStressTest.this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setMax(100);
    }

    private String parserSecond2Time(long seconds) {
        String[] timeArray = new String[3];
        timeArray[2] = String.valueOf(seconds % 60); // second
        timeArray[1] = String.valueOf((seconds / 60) % 60); // minute
        timeArray[0] = String.valueOf(seconds / 60 / 60); // hour
        return timeArray[0] + " hour " + timeArray[1] + " minute " + timeArray[2] + " second.";
    }

    @Override
    protected void onDestroy() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        super.onDestroy();
    }

    /**
     * Progress Dialog Switch
     */
    public void switchProgressDialogEnable(View v) {
        if (mProgressDialogSW.isChecked()) {
            if (!mProgressDialog.isShowing()) {
                mProgressDialog.show();
            }
        } else {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.hide();
            }
        }
    }

    /**
     * Start Button Click
     */
    public void doStartProcess(View v) {
        if (mStartBt.getText().equals(Constants.UI_STARTBUTTON_PAUSE)) {
            mAppSettings.edit().putBoolean(Constants.PREFENCES_IS_PAUSE, true).commit();
            updateComponentEnable(Constants.UI_STARTBUTTON_PAUSE);
            showToast("When current file is copyied finish,copy task will be pause.");
            return;
        } else if (mStartBt.getText().equals(Constants.UI_STARTBUTTON_RESTART)) {
            mAppSettings.edit().putBoolean(Constants.PREFENCES_IS_PAUSE, false).commit();
            updateComponentEnable(Constants.UI_STARTBUTTON_RESTART);
            return;
        }
        File srcFile = new File(mSrcFileEdTt.getText().toString());
        File dstFile = new File(mDestFileEdTt.getText().toString());
        String executeCount = mExecutionCountEdTt.getText().toString();
        if (!srcFile.isFile()) {
            showToast("Please choose a file for source of copy task !");
        } else if (dstFile.isDirectory()) {
            showToast("Please choose a file for destination of copy task !");
        } else if (executeCount.length() == 0) {
            showToast("Please input the number of execute count !");
        } else if (Integer.parseInt(executeCount) % 2 != 0) {
            showToast("The number of execution count shoule be even number,or the source file will be deleted.");
        } else {
            // Each start should initial progress dialog, or progress invalid.
            createProgressDialog();
            updateComponentEnable(Constants.UI_STARTBUTTON_START);
            Intent mServiceIntent = new Intent(this, CopyService.class);
            mServiceIntent.putExtra(Constants.INITIAL_SOURCE_FILE, srcFile);
            mServiceIntent.putExtra(Constants.INITIAL_DESTINATION_FILE, dstFile);
            mServiceIntent.putExtra(Constants.INITIAL_COPY_COUNT,
                    Integer.parseInt(mExecutionCountEdTt.getText().toString()));
            mServiceIntent.putExtra(Constants.INITIAL_PROGRESS_DIALOG_EABLE, mProgressDialogSW.isChecked());

            mAppSettings.edit().putBoolean(Constants.PREFENCES_IS_PAUSE, false)
                    .putBoolean(Constants.PREFENCES_IS_STOP, false).commit();
            startService(mServiceIntent);
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current UI mode
        savedInstanceState.putString(Constants.UI_MODE, mUIMode);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
     // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);
        updateComponentEnable(savedInstanceState.getString(Constants.UI_MODE));
    }
    
    private void updateComponentEnable(String mode) {
        boolean enabled = false;
        mUIMode=mode;
        if (mode.equals(Constants.UI_STARTBUTTON_START)) {
            mStartBt.setText(Constants.UI_STARTBUTTON_PAUSE);
        } else if (mode.equals(Constants.UI_STARTBUTTON_PAUSE)) {
            mStartBt.setText(Constants.UI_STARTBUTTON_RESTART);
        } else if (mode.equals(Constants.UI_STARTBUTTON_RESTART)) {
            mStartBt.setText(Constants.UI_STARTBUTTON_PAUSE);
        } else if (mode.equals(Constants.UI_STOPBUTTON_STOP) || mode.equals(Constants.UI_INITIAL)) {
            enabled = true;
            mStartBt.setText(Constants.UI_STARTBUTTON_START);
        }
        mSrcFileEdTt.setEnabled(enabled);
        mDestFileEdTt.setEnabled(enabled);
        mSrcChooserBt.setEnabled(enabled);
        mDestChooserBt.setEnabled(enabled);
        mExecutionCountEdTt.setEnabled(enabled);
        mProgressDialogSW.setEnabled(enabled);
        mStopBt.setEnabled(!enabled);
    }

    /**
     * Stop Button Click
     */
    public void doStopProcess(View v) {
        showToast("When the even count of file is copyied finish,copy task will be stop.");
        if (mProgressDialog != null) {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.hide();
            }
        }
        updateComponentEnable(Constants.UI_STOPBUTTON_STOP);
        mAppSettings.edit().putBoolean(Constants.PREFENCES_IS_PAUSE, false)
                .putBoolean(Constants.PREFENCES_IS_STOP, true).commit();
    }

    /**
     * chooser Button Click
     */
    public void doFileChooser(View v) {
        ImageButton bt = (ImageButton) v;
        Intent intent = new Intent(this, FileChooser.class);
        String folderPath = "";
        switch (bt.getId()) {
        case R.id.srcChooserBt:
            folderPath = mSrcFileEdTt.getText().toString();
            break;
        case R.id.destChooserBt:
            folderPath = mDestFileEdTt.getText().toString();
            break;
        default:
            // doNotThing
            break;
        }
        if (folderPath.lastIndexOf("/") != -1) {
            folderPath = folderPath.substring(0, folderPath.lastIndexOf("/"));
            File dir = new File(folderPath);
            if (dir.isDirectory()) {
                intent.putExtra("dirPath", folderPath);
            }
        }
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
}
