package com.apical.gpslogger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.util.Log;

import java.io.*;

public class GpsService extends Service
{
    private static final String TAG = "GpsService";
    private GpsBinder       mBinder     = null;
    private Handler         mHandler    = null;
    private LocationManager mLocManager = null;
    private String          mLogFile    = null;
    private PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        mBinder     = new GpsBinder();
        mLocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
        mWakeLock.setReferenceCounted(false);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopGpsLog();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    public class GpsBinder extends Binder {
        public GpsService getService(Handler h) {
            mHandler = h;
            return GpsService.this;
        }
    }

    public void startGpsLog(String file) {
        mWakeLock.acquire();
        mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, mLocationListener);
        mLocManager.addNmeaListener(mNmeaListener);
        mLogFile = file;
        showNotification(this, true, getString(R.string.nmea_is_recording));
    }

    public void stopGpsLog() {
        showNotification(this, false, null);
        mLocManager.removeUpdates(mLocationListener);
        mLocManager.removeNmeaListener(mNmeaListener);
        mLogFile = null;
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    public void resetGps() {
        SystemProperties.set("sys.gps.start", "3");
    }

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
        }
        @Override
        public void onProviderDisabled(String provider) {
        }
        @Override
        public void onProviderEnabled(String provider) {
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
            case LocationProvider.AVAILABLE:
                break;
            case LocationProvider.OUT_OF_SERVICE:
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                break;
            }
        }
    };

    GpsStatus.NmeaListener mNmeaListener = new GpsStatus.NmeaListener() {
        @Override
        public void onNmeaReceived(long timestamp, String nmea) {
//          Log.d(TAG, nmea);
            if (mLogFile != null) {
                writeFile(mLogFile, nmea);
                Message msg = new Message();
                msg.what = GpsNmeaLoggerActivity.MSG_NMEA_STRING;
                msg.obj  = new String(nmea);
                mHandler.sendMessage(msg);
            }
        }
    };

    private static void writeFile(String file, String text) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(file, true);
            writer.write(text);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private static final int NOTIFICATION_ID = 1;
    private static Notification        mNotification = new Notification();
    private static NotificationManager mNotifyManager= null;
    private static void showNotification(Context context, boolean show, String msg) {
        if (mNotifyManager == null) mNotifyManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (show) {
            PendingIntent pi    = PendingIntent.getActivity(context, 0, new Intent(context, GpsNmeaLoggerActivity.class), 0);
            mNotification.flags = Notification.FLAG_ONGOING_EVENT;
            mNotification.icon  = R.drawable.ic_launcher;
            mNotification.setLatestEventInfo(context, context.getResources().getString(R.string.app_name), msg, pi);
            mNotifyManager.notify(NOTIFICATION_ID, mNotification);
        } else {
            mNotifyManager.cancel(NOTIFICATION_ID);
        }
    }
}


