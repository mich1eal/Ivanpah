package com.mich1eal.ivanpah;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Michael on 10/5/2016.
 */
public class MirrorAlarm
{
    private static final String TAG = MirrorAlarm.class.getSimpleName();
    private static final int SNOOZE_MINS = 10;

    private static final String HUE_URL_BASE = "http://";
    private static final String HUE_URL_END1 = "/api/qsrRaSO7t7sb6MyAa8saqzgCZMCelVPNUIY1qJWL/lights/3/state";
    private static final String HUE_IP = "192.168.0.10";

    private static final long SUNRISE_FREQ_MILLI = 10000; //update hue every 10 secs

    private static boolean isHueOn = false;

    private Context context;
    private AlarmListener listener;
    private Timer timer;
    private Calendar lastAlarm;
    //private Calendar alarmTime;
    private boolean alarmIsSet = false;
    private static MediaPlayer mediaPlayer;

    public MirrorAlarm(Context context)
    {
        this.context = context;

        mediaPlayer = new MediaPlayer();

        lastAlarm = Calendar.getInstance();

/*
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null)
        {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        Log.d(TAG, "alarmUri null? :" + Boolean.toString(alarmUri == null));

        mediaPlayer = MediaPlayer.create(context.getApplicationContext(), alarmUri);
        mediaPlayer.setLooping(true);*/
    }

    public int getLastAlarmHour()
    {
        return lastAlarm.get(Calendar.HOUR_OF_DAY);
    }

    public int getLastAlarmMinute()
    {
        return lastAlarm.get(Calendar.MINUTE);
    }

    public boolean isAlarmSet()
    {
        return alarmIsSet;
    }

    public void setAlarmListener(AlarmListener listener)
    {
        this.listener = listener;
    }

    // Sets alarm to ring at the given calendar date. Overwrites any alarms currently in progress

    public void setAlarm(int hour, int minute, int minutesToHue)
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

        //Set alarm
        setAlarm(nextAlarm);

        //Set hue fade in
        if (minutesToHue > 0)
        {
            Calendar hueAlarm = (Calendar) nextAlarm.clone(); //Clone since calendars are mutable
            hueAlarm.add(Calendar.MINUTE, 0 - minutesToHue); //Subtract mins for hue
            setSunriseTime(hueAlarm, (Calendar) nextAlarm.clone());
        }

    }

    private void setAlarm(Calendar nextAlarm)
    {

        lastAlarm = nextAlarm;

        Log.d(TAG, "Alarm set for: " + nextAlarm.getTime().toString());

        cancel();
        timer = new Timer();
        timer.schedule(getAlarmTask(), nextAlarm.getTime());

        alarmIsSet = true;
        if (listener!= null) listener.onAlarmSet(nextAlarm);
    }

    public void cancel()
    {
        if (timer != null) timer.cancel();

        timer = null;
        if (mediaPlayer.isPlaying())
        {
            mediaPlayer.stop();
        }

        alarmIsSet = false;
        if (listener != null) listener.onCancel();
    }

    private void ring()
    {
        if (mediaPlayer.isPlaying())
        {
            mediaPlayer.stop();
        }


        try {
            mediaPlayer.reset();
            AssetFileDescriptor afd = context.getAssets().openFd("river.mp3");
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaPlayer.setLooping(true);

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


        // Mediaplayer has to be prepared before it can start (took 2 ms when I timed it)
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
        {
            @Override
            public void onPrepared(MediaPlayer mp)
            {
                mediaPlayer.start();
                if (listener != null) listener.onRing();
            }
        });

        try
        {
            mediaPlayer.prepareAsync();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    private void setSunriseTime(final Calendar sunriseTime, final Calendar alarmTime)
    {
        Log.d(TAG, "Setting sunrise to start at: " + sunriseTime.getTime().toString());
        //milliseconds sunrise should be active
        final long sunriseTimeWindow = alarmTime.getTimeInMillis() - sunriseTime.getTimeInMillis();

        Timer timer = new Timer();
        TimerTask task = new TimerTask()
        {
            //Each time run() is called, hue is set to correct brightness, th
            public void run() {
                Calendar now = Calendar.getInstance();

                // If alarm is set, and it hasn't rung yet
                if (isAlarmSet() && alarmTime.after(now)) {
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

            setHue(bright);
        }

    }

    public void toggleHue(double bright)
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


    public void setHue(double brightness)
    {
        if (brightness > 0)
        {
            int intBright = (int) (brightness * 255);
            isHueOn = true;
            new URLCaller().execute(HUE_URL_BASE + HUE_IP + HUE_URL_END1, "{\"on\":true, \"bri\":" + intBright + "}");
        }
        else
        {
            setHue(false);
        }

    }

    public void setHue(boolean on)
    {
        if (on)
        {
            isHueOn = true;
            new URLCaller().execute(HUE_URL_BASE + HUE_IP + HUE_URL_END1, "{\"on\":true}");
        }
        else
        {
            isHueOn = false;
            new URLCaller().execute(HUE_URL_BASE + HUE_IP + HUE_URL_END1, "{\"on\":false}");
        }

    }





    public void snooze()
    {
        Calendar now = Calendar.getInstance();
        setAlarm(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE) + SNOOZE_MINS, -1); //no hue for snoozing
    }


    // Method for neatness, returns a timertask that calls ring when completed
    private TimerTask getAlarmTask ()
    {
        return new TimerTask()
        {
            final Handler handler = new Handler();
            @Override
            public void run()
            {
                handler.post(
                        new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                ring();
                            }
                        }
                );
            }
        };
    }

    public interface AlarmListener
    {
        public void onAlarmSet(Calendar time);
        public void onCancel();
        public void onRing();
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
                Log.d(TAG, "Writing output: " + params[1]);
                out.write(params[1]);
                out.close();

                con.getResponseMessage(); //This is necessary to actually make the call I guess?
                Log.d(TAG, "Response code from hue = " + con.getResponseCode());
                Log.d(TAG, "Response message from hue = " + con.getResponseMessage());
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }

            return null;
        }
    }
}