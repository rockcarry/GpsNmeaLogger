package com.apical.gpslogger;

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
        mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, mLocationListener);
        mLocManager.addNmeaListener(mNmeaListener);
        mLogFile = file;
        mWakeLock.acquire();
    }

    public void stopGpsLog() {
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
            Log.d(TAG, nmea);
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
}


