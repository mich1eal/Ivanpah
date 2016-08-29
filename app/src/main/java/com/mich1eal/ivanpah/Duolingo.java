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

    public void getStatus()
    {
        super.getJSON(URL);

    }

    @Override
    protected void onComplete(JSONObject json)
    {
        int streak = -1;
        try
        {
            streak = json.getInt("site_streak");
        }
        catch (Exception e)
        {
            Log.e(TAG, "Error reading duolingo json");
            e.printStackTrace();
                    }
        Log.d(TAG, "Streak = " + streak);
    }
}
