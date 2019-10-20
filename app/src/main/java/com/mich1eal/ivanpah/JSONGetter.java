package com.mich1eal.ivanpah;

import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Michael on 8/14/2016.
 */
public abstract class JSONGetter
{
    private static final String TAG = "AsyncHttp";

    private Timer timer;
    private boolean cancelled = false;
    protected abstract void onComplete(JSONObject json);
    protected abstract void getNewData();

    protected void getJSON(String url)
    {
        new AsyncHttp().execute(url);
    }

    // Sets up JSON getter to automatically refresh data
    public void setAutoUpdate(final Handler handler, long repeatTime)
    {
        Log.d(TAG, "Setting autoUpdate for weather");
        getNewData();
        TimerTask timerTask = getTimerTask(handler);
        timer = new Timer();
        timer.schedule(timerTask, 0, repeatTime);
    }

    public void cancelAutoUpdate()
    {
        cancelled = true;
        if (timer != null)
        {
            timer.cancel();
        }
    }

    private TimerTask getTimerTask (final Handler handler)
    {
        return new TimerTask()
        {
            @Override
            public void run()
            {
                handler.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.d(TAG, "Getting new data");
                        getNewData();
                    }
                });
            }
        };
    }


    private class AsyncHttp extends AsyncTask<String, String, JSONObject>
    {
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        @Override
        protected JSONObject doInBackground(String... params)
        {
            try
            {
                URL url = new URL(params[0]);
                Log.d(TAG, url.toString());
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                Log.d(TAG, "Response code = " + con.getResponseCode());

                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuffer json = new StringBuffer();

                String line = null;
                while ((line = reader.readLine()) != null)
                {
                    json.append(line).append('\n');
                }
                reader.close();

                return new JSONObject(json.toString());
            } catch (Exception e)
            {
                Log.e(TAG, "An error occured getting json");
                if (e != null) e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... str)
        {
            super.onProgressUpdate();
        }

        @Override
        protected void onPostExecute(JSONObject json)
        {
            super.onPostExecute(json);
            if (json == null) Log.e(TAG, "Json is null");
            else onComplete(json);
        }

    }
}
