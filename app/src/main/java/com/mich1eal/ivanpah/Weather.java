package com.mich1eal.ivanpah;

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Michael on 10/14/2016.
 */
public class Weather
        extends JSONGetter
{
    private static final String TAG = "WEATHER";
    private static final String APIKey = "310a6b004645167adf0d695576d45819";
    private LocationManager lm = null;
    private WeatherUpdateListener listener;

    private Location lastLoc = null;
    private JSONObject lastJSON = null;

    private boolean hasValues = false;
    private int temp;
    private int min;
    private int max;
    private String cond;
    private double precipChance;
    private String precipType;
    private boolean isPrecip;

    private Weather(){}

    public Weather(LocationManager manager, WeatherUpdateListener listener)
    {
        lm = manager;
        this.listener = listener;
    }

    public int getTemp(){return temp;}
    public int getMin(){return min;}
    public int getMax(){return max;}
    public String getCond(){return cond;}
    public double getPrecipChance(){return precipChance;}
    public String getPrecipType(){return precipType;}
    public boolean hasValues(){return hasValues;}
    public boolean isPrecip(){return isPrecip;}

    @Override
    protected void onComplete(JSONObject json)
    {
        lastJSON = json;
        updateData();
        listener.onWeatherDataChange();
    }

    @Override
    protected void getNewData()
    {
        Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (loc != null)
        {
            Log.d(TAG, "Location = " + loc.toString());
            lastLoc = loc;
        }

        if (lastLoc == null)
        {
            Log.e(TAG, "No location available");
            return;
        }

        String apiurl = "https://api.forecast.io/forecast/" +
                APIKey + '/' +
                lastLoc.getLatitude() + ',' +
                lastLoc.getLongitude();

        super.getJSON(apiurl);
    }

    private String getCondString(String in)
    {
        StringBuilder out = new StringBuilder("wi_");
        String[] strs = in.split("-");

        int n = strs.length;
        if (n == 1)
        {
            out.append(strs[0]);
        }
        else if (n > 1)
        {
            // First say if it's day or night
            out.append(strs[n - 1]);
            // Then the conditions
            for (int i = 0; i < n - 1; i++)
            {
                out.append('_');
                out.append(strs[i]);
            }
        }
        String outStr = out.toString();

        if (outStr.equals("wi_day_partly_cloudy")) outStr = "wi_day_cloudy";
        if (outStr.equals("wi_day_clear")) outStr = "wi_day_sunny";


        Log.d(TAG, "Condition icon: " + outStr);

        return outStr;
    }

    private void updateData()
    {
        if (lastJSON == null)
        {
            hasValues = false;
            return;
        }
        hasValues = true;

        try
        {
            // look at the API guide https://developer.forecast.io/docs/v2
            JSONObject now = lastJSON.getJSONObject("currently");
            temp = now.getInt("temperature");

            JSONArray days = lastJSON.getJSONObject("daily").getJSONArray("data");

            String iconStr = now.getString("icon");
            cond = getCondString(iconStr);
            isPrecip = iconStr.equals("rain") || iconStr.equals("sleet") || iconStr.equals("snow");

            // JSON > daily > data > 0 (today's datapoint)
            JSONObject today = lastJSON.getJSONObject("daily").getJSONArray("data").getJSONObject(0);
            min = today.getInt("temperatureMin");
            max = today.getInt("temperatureMax");
            precipType = "wi_" + today.getString("precipType");
            precipChance = (float) today.getDouble("precipProbability");
        }
        catch (JSONException e)
        {
            Log.e(TAG, e.getMessage());
        }
    }

    public interface WeatherUpdateListener
    {
        public void onWeatherDataChange();
    }
}
