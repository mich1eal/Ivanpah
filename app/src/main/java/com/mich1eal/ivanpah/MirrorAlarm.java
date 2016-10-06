package com.mich1eal.ivanpah;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;
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
    private static Ringtone ringtone;

    // requires context
    private MirrorAlarm(){};

    public MirrorAlarm(Context context)
    {
        this.context = context;
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null)
        {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        ringtone = RingtoneManager.getRingtone(context, alarmUri);
        if (ringtone == null) throw new AssertionError("Ringtone is null!");
    }

    public void setAlarmListener(AlarmListener listener)
    {
        this.listener = listener;
    }

    public void setAlarm(Date alarmTime)
    {
        Log.d(TAG, "Before: date = " + alarmTime.toString());
        Date now = new Date(System.currentTimeMillis());
        alarmTime.setDate(now.getDate());
        alarmTime.setYear(now.getYear());

        // if alarmTime occurs before now, set its day to tomorrow
        if (now.compareTo(alarmTime) > 0)
        {
            Calendar c = Calendar.getInstance();
            c.setTime(alarmTime);
            c.add(Calendar.DATE, 1);
            alarmTime = c.getTime();
        }

        Log.d(TAG, "After: date = " + alarmTime.toString());

        cancel();
        timer = new Timer();
        timer.schedule(getAlarmTask(), alarmTime);
        if (listener!= null) listener.onAlarmSet(alarmTime);
    }

    public void cancel()
    {
        if (timer != null) timer.cancel();
        if (ringtone.isPlaying()) ringtone.stop();
        timer = null;
        if (listener != null) listener.onCancel();
    }

    public void ring()
    {
        ringtone.play();
        if (listener != null) listener.onRing();
    }

    // Gets a simple TimerTask that calls ring() when completed
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
        public void onAlarmSet(Date time);
        public void onCancel();
        public void onRing();
    }
}
