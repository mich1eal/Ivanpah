package com.mich1eal.ivanpah;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Michael on 8/14/2016.
 */
public class JSONGetter extends AsyncTask<String, String, JSONObject>
{
    private static final String TAG = "AsyncHttp";

    private static CompletionListener listener;

    private JSONGetter(){} // Don't instantitate w/o listener

    public JSONGetter(CompletionListener listener)
    {
        this.listener = listener;
    }



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
            while((line = reader.readLine()) != null)
            {
                json.append(line).append('\n');
            }
            reader.close();

            return new JSONObject(json.toString());
        }
        catch(Exception e){
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
        if (json == null) Log.e(TAG, "Json is null");
        else listener.onComplete(json);
    }

    public interface CompletionListener
    {
        public void onComplete(JSONObject json);
    }
}
