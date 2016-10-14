package com.mich1eal.ivanpah;

import android.util.Log;

import org.json.JSONObject;

/**
 * Created by Michael on 8/14/2016.
 */
public class Duolingo
    extends JSONGetter
{
    private static final String TAG = "DUOLINGO";
    private static final String URL = "https://www.duolingo.com/users/mich1eal";
    private static OnNewStreakListener listener;
    private static int lastStreak = Integer.MAX_VALUE;


    public void getStatus()
    {
        super.getJSON(URL);
    }

    public void setOnStreakListener(OnNewStreakListener listener)
    {
        this.listener = listener;
    }

    @Override
    protected void onComplete(JSONObject json)
    {
        // Look at current streak. If last recorded streak is smaller by 1, a lesson has been completed
        int streak = -1;
        try
        {
            streak = json.getInt("site_streak");
        }
        catch (Exception e)
        {
            Log.e(TAG, "Error reading duolingo json");
            e.printStackTrace();
            if (listener != null) listener.onNewStreak();
        }
        if (streak == lastStreak + 1 && listener != null) listener.onNewStreak();
        lastStreak = streak;

        Log.d(TAG, "Streak = " + streak);
    }

    @Override
    protected void getNewData()
    {
        super.getJSON(URL);
    }

    public interface OnNewStreakListener
    {
        public void onNewStreak();
    }

}
