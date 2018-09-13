package com.mich1eal.ivanpah;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

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

    public void setAlarm(int hour, int minute)
    {
        Calendar nextAlarm = Calendar.getInstance();
        nextAlarm.set(Calendar.HOUR_OF_DAY, hour);
        nextAlarm.set(Calendar.MINUTE, minute);
        setAlarm(nextAlarm);
    }


    private void setAlarm(Calendar setTime)
    {

        //Clone since calendars are mutable and we need to modify it
        Calendar nextAlarm = (Calendar) setTime.clone();
        Calendar now = Calendar.getInstance();

        //round off time to 0 seconds, so rings right when changes
        nextAlarm.set(Calendar.SECOND, 0);

        // if alarmTime occurs before now, set its day to tomorrow
        if (nextAlarm.before(now))
        {
            // if the alarm time already happened, make it go off tomorrow instead
            nextAlarm.add(Calendar.DATE, 1);
        }

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

    public void ring()
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

    public void snooze()
    {
        Calendar snooze = Calendar.getInstance();
        snooze.add(Calendar.MINUTE, SNOOZE_MINS);
        setAlarm(snooze);
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
}