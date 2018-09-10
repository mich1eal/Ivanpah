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
    private Context context;
    private AlarmListener listener;
    private Timer timer;
    private Calendar alarmTime;
    private static MediaPlayer mediaPlayer;

    public MirrorAlarm(Context context)
    {
        this.context = context;

        mediaPlayer = new MediaPlayer();

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

    public void setAlarmListener(AlarmListener listener)
    {
        this.listener = listener;
    }

    // Sets alarm to ring at the given calendar date. Overwrites any alarms currently in progress
    public void setAlarm(Calendar alarmTime, boolean hueEnabled)
    {
        Calendar now = Calendar.getInstance();
        this.alarmTime = alarmTime;

        // if alarmTime occurs before now, set its day to tomorrow
        if (alarmTime.before(now))
        {
            // if the alarm time already happened, make it go off tomorrow instead
            alarmTime.add(Calendar.DATE, 1);
        }

        Log.d(TAG, "Alarm set for: " + alarmTime.getTime().toString());

        cancel();
        timer = new Timer();
        timer.schedule(getAlarmTask(), alarmTime.getTime());
        if (listener!= null) listener.onAlarmSet(alarmTime, hueEnabled);
    }

    public void cancel()
    {
        if (timer != null) timer.cancel();
        timer = null;
        if (mediaPlayer.isPlaying())
        {
            mediaPlayer.stop();
        }
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

    public long timeToAlarm()
    {
        if (timer != null)
        {
            return alarmTime.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
        }
        return -1;
    }


    public interface AlarmListener
    {
        public void onAlarmSet(Calendar time, boolean hueEnabled);
        public void onCancel();
        public void onRing();
    }
}