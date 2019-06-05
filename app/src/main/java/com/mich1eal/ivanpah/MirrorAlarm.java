package com.mich1eal.ivanpah;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

/**
 * Created by Michael on 10/5/2016.
 */
public class MirrorAlarm
{
    private static final String TAG = MirrorAlarm.class.getSimpleName();
    private static final int SNOOZE_MINS = 10;

    private static final String HUE_URL_BASE = "http://";
    private static String HUE_IP;
    private static final String HUE_URL_END1 = "/api/qsrRaSO7t7sb6MyAa8saqzgCZMCelVPNUIY1qJWL/lights/3/state";
    private static final String HUE_URL_END2 = "/api/qsrRaSO7t7sb6MyAa8saqzgCZMCelVPNUIY1qJWL/lights/2/state";

    private static boolean isHueOn = false;

    private Context context;
    private Calendar lastAlarm;

    public MirrorAlarm(Context context)
    {
        this.context = context;
    }

    public int getLastAlarmHour()
    {
        //Lazy constructor for lastAlarm
        if (lastAlarm == null) lastAlarm = Calendar.getInstance();
        return lastAlarm.get(Calendar.HOUR_OF_DAY);
    }

    public int getLastAlarmMinute()
    {
        //Lazy constructor for lastAlarm
        if (lastAlarm == null) lastAlarm = Calendar.getInstance();
        return lastAlarm.get(Calendar.MINUTE);
    }

    // Sets alarm to ring at the given calendar date. Overwrites any alarms currently in progress
    public void setAlarm(int hour, int minute, int minutesToHue, String hueIP)
    {
        Calendar now = Calendar.getInstance();

        //New calendar since calendars are mutable
        Calendar nextAlarm = Calendar.getInstance();
        nextAlarm.set(Calendar.HOUR_OF_DAY, hour);
        nextAlarm.set(Calendar.MINUTE, minute);

        //round off time to 0 seconds, so rings right when changes
        nextAlarm.set(Calendar.SECOND, 0);

        // if alarmTime occurs before now, set its day to tomorrow
        if (nextAlarm.before(now))
        {
            // if the alarm time already happened, make it go off tomorrow instead
            nextAlarm.add(Calendar.DATE, 1);
        }

        if(hueIP != null)
        {
            this.HUE_IP = hueIP;
        }

        //Set alarm
        setAlarmService(nextAlarm);

        //Set hue fade in
        if (minutesToHue > 0)
        {
            Calendar hueAlarm = (Calendar) nextAlarm.clone(); //Clone since calendars are mutable
            hueAlarm.add(Calendar.MINUTE, 0 - minutesToHue); //Subtract mins for hue

            // hueAlarm should not be in the past
            if(hueAlarm.before(Calendar.getInstance())) hueAlarm = Calendar.getInstance();

            setHueService(hueAlarm, (Calendar) nextAlarm.clone());
        }
    }

    private void setAlarmService(Calendar lastAlarm)
    {
        cancel();
        long alarmTime = lastAlarm.getTimeInMillis();

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmService.class);

        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

        //setExact method introduced in API 19, before it was always exact
        if (Build.VERSION.SDK_INT > 19) {
            am.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
        }
    }

    private void setHueService(Calendar hueAlarm, Calendar lastAlarm)
    {
        long alarmTime = lastAlarm.getTimeInMillis();
        long hueTime = hueAlarm.getTimeInMillis();

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, HueService.class);
        intent.putExtra(HueService.EXTRA_RING_TIME, alarmTime);

        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

        //setExact method introduced in API 19, before it was always exact
        if (Build.VERSION.SDK_INT > 19) {
            am.setExact(AlarmManager.RTC_WAKEUP, hueTime, pendingIntent);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, hueTime, pendingIntent);
        }
    }

    public boolean isSet()
    {
        Intent intent = new Intent(context, AlarmService.class);
        PendingIntent sender = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);

        return sender != null;
    }

    public boolean isRinging()
    {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (AlarmService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void cancel()
    {
        Log.d(TAG, "onCancel called");

        //Cancel ringing
        Intent intent = new Intent(context, AlarmService.class);
        context.stopService(intent);

        //Cancel ring service
        PendingIntent sender = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);

        //Cancel ring pending intent and alarmmanager
        if (sender == null) Log.d(TAG, "No alarm found to cancel");
        else
        {
            Log.d(TAG, "Alarm found, proceding with cancelling");
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(sender);
            sender.cancel();
        }

        //Cancel hue
        intent = new Intent(context, HueService.class);
        context.stopService(intent);

        //Cancel hue service
        sender = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);

        //Cancel hue pending intent and alarmmanager
        if (sender == null) Log.d(TAG, "No hue found to cancel");
        else
        {
            Log.d(TAG, "Hue found, proceding with cancelling");
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(sender);
            sender.cancel();
        }
    }

    public static void toggleHue(double bright)
    {
        if (isHueOn)
        {
            setHue(false);
        }
        else
        {
            setHue(bright);
        }
    }

    public static void setHue(double brightness)
    {
        if (brightness > 0)
        {
            int intBright = (int) (brightness * 255);
            isHueOn = true;
            new URLCaller().execute(HUE_URL_BASE + HUE_IP + HUE_URL_END1, "{\"on\":true, \"bri\":" + intBright + "}");
            new URLCaller().execute(HUE_URL_BASE + HUE_IP + HUE_URL_END2, "{\"on\":true, \"bri\":" + intBright + "}");
        }
        else
        {
            setHue(false);
        }
    }

    public static void setHue(boolean on)
    {
        if (on)
        {
            isHueOn = true;
            new URLCaller().execute(HUE_URL_BASE + HUE_IP + HUE_URL_END1, "{\"on\":true}");
            new URLCaller().execute(HUE_URL_BASE + HUE_IP + HUE_URL_END2, "{\"on\":true}");
        }
        else
        {
            isHueOn = false;
            new URLCaller().execute(HUE_URL_BASE + HUE_IP + HUE_URL_END1, "{\"on\":false}");
            new URLCaller().execute(HUE_URL_BASE + HUE_IP + HUE_URL_END2, "{\"on\":false}");
        }
    }

    public Calendar snooze()
    {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, SNOOZE_MINS);
        setAlarm(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), -1, null); //no hue for snoozing
        return now;
    }

    private static class URLCaller extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            HttpURLConnection con = null;
            URL url = null;
            OutputStreamWriter out = null;

            try {
                url = new URL(params[0]);

                con = (HttpURLConnection) url.openConnection();
                con.setDoOutput(true);
                con.setRequestMethod("PUT");
                out = new OutputStreamWriter(con.getOutputStream());
                out.write(params[1]);
                out.close();

                con.getResponseMessage();
                // Log.d(TAG, "Response code from hue = " + con.getResponseCode());
                // Log.d(TAG, "Response message from hue = " + con.getResponseMessage());
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }

            return null;
        }
    }
}