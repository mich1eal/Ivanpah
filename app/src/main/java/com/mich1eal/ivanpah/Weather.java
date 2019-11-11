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
    private WeatherUpdateListener listener;

    private Location location = null;
    private JSONObject lastJSON = null;

    private boolean hasValues = false;
    private int temp;
    private int min, tomorrowMin;
    private int max, tomorrowMax;
    private String cond, tomorrowCond;
    private double precipChance, tomorrowPrecipChance;
    private String precipType, tomorrowPrecipType;
    private long sunRise = -1;
    private long sunSet = -1;

    private Weather(){}

    public Weather(WeatherUpdateListener listener)
    {
        this.listener = listener;
        if(location == null)
        {
            Log.e(TAG, "Location is null");
        }
    }

    public void setLocation(Location location)
    {
        if(location == null) Log.e(TAG, "Location is null");
        this.location = location;
        getNewData();
    }

    public int getTemp(){return temp;}
    public int getMin(boolean today)
    {
        if (today) return min;
        else return tomorrowMin;
    }
    public int getMax(boolean today)
    {
        if (today) return max;
        else return tomorrowMax;
    }
    public String getCond(boolean today)
    {
        if (today) return cond;
        else return tomorrowCond;
    }
    public double getPrecipChance(boolean today)
    {
        if (today) return precipChance;
        else return tomorrowPrecipChance;
    }
    public String getPrecipType(boolean today)
    {
        if (today) return precipType;
        else return tomorrowPrecipType;
    }
    public boolean hasValues(){return hasValues;}
    public long getSunrise(){return sunRise;}
    public long getSunSet(){return sunSet;}

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
        if (location == null) return;

        String apiurl = "https://api.darksky.net/forecast/" +
                APIKey + '/' +
                location.getLatitude() + ',' +
                location.getLongitude();
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

            // JSON > daily > data > 0 (today's datapoint)
            JSONObject today = lastJSON.getJSONObject("daily").getJSONArray("data").getJSONObject(0);
            min = today.getInt("temperatureLow");
            max = today.getInt("temperatureHigh");
            sunRise = today.getLong("sunriseTime");
            sunSet = today.getLong("sunsetTime");

            // JSON > daily > data > 1 (tomorrows's datapoint)
            JSONObject tomorrow = lastJSON.getJSONObject("daily").getJSONArray("data").getJSONObject(1);
            tomorrowMin = tomorrow.getInt("temperatureLow");
            tomorrowMax = tomorrow.getInt("temperatureHigh");
            String tomorrowIcon = tomorrow.getString("icon");
            tomorrowCond = getCondString(tomorrowIcon);


        }
        catch (JSONException e)
        {
            Log.e(TAG, e.getMessage());
        }

        // Rain data isn't always available so fetch it seperately
        precipChance = 0;

        try
        {
            JSONObject today = lastJSON.getJSONObject("daily").getJSONArray("data").getJSONObject(0);
            precipType = "wi_" + today.getString("precipType");
            precipChance = (float) today.getDouble("precipProbability");
        }
        catch (JSONException e)
        {
            Log.e(TAG, e.getMessage());
        }

        // And tomorrow's predcip change
        tomorrowPrecipChance = 0;
        try
        {
            JSONObject tomorrow = lastJSON.getJSONObject("daily").getJSONArray("data").getJSONObject(1);
            tomorrowPrecipType = "wi_" + tomorrow.getString("precipType");
            tomorrowPrecipChance = (float) tomorrow.getDouble("precipProbability");
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
