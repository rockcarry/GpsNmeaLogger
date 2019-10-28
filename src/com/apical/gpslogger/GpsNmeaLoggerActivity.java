package com.apical.gpslogger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GpsNmeaLoggerActivity extends Activity
    implements View.OnClickListener
{
    private static final String TAG = "NmeaLoggerActivity";
    private static final String NMEA_LOG_DIR = Environment.getExternalStorageDirectory() + File.separator + "nmea";
    private Button     mBtnStartLog;
    private Button     mBtnStopLog;
    private Button     mBtnGpsReset;
    private EditText   mEditGpsNmea;
    private GpsService mGpsServ = null;

    private ServiceConnection mGpsServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder serv) {
            mGpsServ = ((GpsService.GpsBinder)serv).getService(mHandler);;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mGpsServ = null;
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mBtnStartLog = (Button)findViewById(R.id.btn_start_log);
        mBtnStopLog  = (Button)findViewById(R.id.btn_stop_log );
        mBtnGpsReset = (Button)findViewById(R.id.btn_gps_reset);
        mBtnStartLog.setOnClickListener(this);
        mBtnStopLog .setOnClickListener(this);
        mBtnGpsReset.setOnClickListener(this);

        mEditGpsNmea = (EditText)findViewById(R.id.txt_gps_nmea );
        mEditGpsNmea.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);

        // start record service
        Intent i = new Intent(GpsNmeaLoggerActivity.this, GpsService.class);
        startService(i);

        // bind record service
        bindService(i, mGpsServiceConn, Context.BIND_AUTO_CREATE);

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                  getApplicationInfo().packageName, PackageManager.GET_PERMISSIONS);

            if (packageInfo.requestedPermissions != null) {
                for (String permission : packageInfo.requestedPermissions) {
                    Log.v(TAG, "Checking permissions for: " + permission);
                    if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(packageInfo.requestedPermissions, 1);
                        return;
                    }
                }
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Unable to load package's permissions", e);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        // unbind record service
        unbindService(mGpsServiceConn);

        // stop record service
        Intent i = new Intent(GpsNmeaLoggerActivity.this, GpsService.class);
        stopService(i);

        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.btn_start_log:
            {
                AlertDialog.Builder dialog = new AlertDialog.Builder(GpsNmeaLoggerActivity.this);
                dialog.setTitle(R.string.start_gps_log_title);
                dialog.setMessage(R.string.start_gps_log_message);
                dialog.setCancelable(false);
                dialog.setPositiveButton(R.string.btn_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mEditGpsNmea.setText("");
                        File dir = new File(NMEA_LOG_DIR);
                        if (!dir.exists()) dir.mkdir();
                        Date date = new Date(System.currentTimeMillis());
                        SimpleDateFormat df = new SimpleDateFormat("'nmea'_yyyyMMdd_HHmmss");
                        String file = NMEA_LOG_DIR + File.separator + df.format(date) + ".log";
                        mGpsServ.startGpsLog(file);
                        Toast.makeText(GpsNmeaLoggerActivity.this, file, Toast.LENGTH_LONG).show();
                    }
                });
                dialog.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                dialog.show();
            }
            break;
        case R.id.btn_stop_log:
            mGpsServ.stopGpsLog();
            Toast.makeText(GpsNmeaLoggerActivity.this, R.string.toast_stop_catch_log, Toast.LENGTH_LONG).show();
            break;
        case R.id.btn_gps_reset:
            mGpsServ.resetGps();
            break;
        }
    }

    public static final int MSG_NMEA_STRING = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_NMEA_STRING:
                {
                    String str = (String)msg.obj + mEditGpsNmea.getText().toString();
                    if (str.length() > 2 * 1024) str = str.substring(0, 1 * 1024);
                    mEditGpsNmea.setText(str);
                }
                break;
            }
        }
    };
}

