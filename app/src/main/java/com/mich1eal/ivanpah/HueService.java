package com.mich1eal.ivanpah;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.mich1eal.ivanpah.activities.Mirror;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Michael on 2/28/2019.
 */

public class HueService extends Service
{
    private static final String TAG = HueService.class.getSimpleName();

    public static final String EXTRA_RING_TIME = "com.mich1eal.ivanpah.extra.RING_TIME";
    private static final long SUNRISE_FREQ_MILLI = 10000; //update hue every 10 secs

    private PowerManager.WakeLock wl;
    private TimerTask task;
    private Timer timer;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        wl.acquire();

        Calendar now = Calendar.getInstance();

        long ringTime = -1;
        if (intent != null)
        {
            ringTime = intent.getLongExtra(EXTRA_RING_TIME, -1);
        }

        Calendar ringCal = Calendar.getInstance();
        if (ringTime > 0)
        {
            ringCal.setTimeInMillis(ringTime);
        }
        else
        {
            ringCal.add(Calendar.MINUTE, Mirror.defaultMinsToHue);
        }

        setSunriseTime(now, ringCal);
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG, "onDestroy()");
        if (task != null) task.cancel();
        if (timer != null) timer.cancel();

        super.onDestroy();
    }

    private void setSunriseTime(final Calendar sunriseTime, final Calendar alarmTime)
    {
        Log.d(TAG, "Setting sunrise to start at: " + sunriseTime.getTime().toString());
        //milliseconds sunrise should be active
        final long sunriseTimeWindow = alarmTime.getTimeInMillis() - sunriseTime.getTimeInMillis();

        timer = new Timer();
        task = new TimerTask()
        {
            //Each time run() is called, hue is set to correct brightness, th
            public void run() {
                Calendar now = Calendar.getInstance();

                // If alarm is set, and it hasn't rung yet
                if (alarmTime.after(now)) {
                    long timeLeft = alarmTime.getTimeInMillis() - now.getTimeInMillis();
                    updateHue(timeLeft, sunriseTimeWindow);
                } else {
                    Log.d(TAG, "Sunrise cancelled");
                    cancel();
                }
            }
        };

        timer.schedule(task, sunriseTime.getTime(), SUNRISE_FREQ_MILLI);
    }

    private void updateHue(long timeLeft, long timeWindow)
    {
        if (timeLeft > 0)
        {
            double bright = ((double) timeWindow - timeLeft)  / timeWindow;
            Log.d(TAG, (timeLeft / 1000) + " secs to ring, setting bright to " + bright);

            MirrorAlarm.setHue(bright);
        }
    }
}