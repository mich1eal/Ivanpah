package com.mich1eal.ivanpah;

import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;

/**
 * Created by Michael on 9/1/2016.
 */
public class AlarmManager
{
    private static String TAG = AlarmManager.class.getSimpleName();

    public AlarmManager()
    {

    }

    public void setAlarm(int hour, int min, Context context)
    {
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
        intent.putExtra(AlarmClock.EXTRA_HOUR, hour);
        intent.putExtra(AlarmClock.EXTRA_MINUTES, min);
        intent.putExtra(AlarmClock.EXTRA_MESSAGE, "Ivanpah Alarm");
        intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        context.startActivity(intent);
    }


    public void cancelAlarm()
    {

    }

}
