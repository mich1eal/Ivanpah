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

    // Strings for use in API
    private static final String APIKey = "d2779b6f3abf700f12c9171e9db1bc99";
    private static final String excludeString = "minutely,hourly,alerts";
    private static final String unitString = "imperial";

    private WeatherUpdateListener listener;

    private Location location = null;
    private JSONObject lastJSON = null;

    private boolean hasValues = false;
    private int temp;
    private int min;
    private int max;
    private int weatherCode = -1;
    private double precipChance;
    private boolean isPrecip;
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
    public int getMin(){return min;}
    public int getMax(){return max;}
    public double getPrecipChance(){return precipChance;}
    public boolean hasValues(){return hasValues;}
    public boolean isPrecip(){return isPrecip;}
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

        String apiurl = "https://api.openweathermap.org/data/3.0/onecall?lat=" + location.getLatitude() +
                "&lon=" + location.getLongitude() +
                "&exclude=" + excludeString +
                "&appid=" + APIKey +
                "&units=" + unitString;

        super.getJSON(apiurl);
    }
    public int getConditionIconId(long now){
        // now needs to be in seconds
        // Returns R.string id for the icon to display
        // if not set, use day icons

        boolean isDay = true;
        // we will use day unless we have sunset/sunrise data
        if (sunRise > 0 && sunSet > 0) {
            isDay = now > sunRise && now < sunSet;
        }
        return getConditionIconId(weatherCode, isDay);
    }

    private int getConditionIconId(int code, boolean isDay)
    {
        // Map weather codes to icon strings according to doc/weather_icon_map.xlsx
        if (code < 300) {
            //Thunderstorm
            if (code == 200 || code == 210) {
                return isDay ? R.string.wi_day_storm_showers : R.string.wi_night_alt_storm_showers;
            }
            else {
                return isDay ? R.string.wi_day_thunderstorm : R.string.wi_night_alt_thunderstorm;
            }
        }
        else if (code < 400) {
            //Drizzle
            if (code == 310 || code == 311 || code == 312) {
                return isDay ? R.string.wi_day_rain : R.string.wi_night_alt_rain;
            }
            else if (code >= 313 && code < 330) {
                return isDay ? R.string.wi_day_showers : R.string.wi_night_alt_showers;
            }
            else {
                return isDay ? R.string.wi_day_fog : R.string.wi_night_fog;
            }
        }
        else if (code >= 500 && code < 600) {
            if (code < 510) {
                return isDay ? R.string.wi_day_rain : R.string.wi_night_alt_rain;
            }
            else if (code < 520) {
                return isDay ? R.string.wi_day_rain_mix : R.string.wi_night_alt_rain_mix;
            }
            else if (code < 540) {
                return isDay ? R.string.wi_day_showers : R.string.wi_night_alt_showers;
            }
            else {
                return isDay ? R.string.wi_day_rain : R.string.wi_night_alt_rain;
            }
        }
        else if (code < 700) {
            if (code < 610) {
                return isDay ? R.string.wi_day_snow : R.string.wi_night_alt_snow;
            }
            else if (code == 611) {
                return isDay ? R.string.wi_day_hail : R.string.wi_night_alt_hail;
            }
            else if (code == 612 || code == 613) {
                return isDay ? R.string.wi_day_sleet : R.string.wi_night_alt_sleet;
            }
            else if (code < 620) {
                return isDay ? R.string.wi_day_rain_mix : R.string.wi_night_alt_rain_mix;
            }
            else {
                return isDay ? R.string.wi_day_snow : R.string.wi_night_alt_snow;
            }
        }
        else if (code < 800) {
            if (code < 710) {
                return isDay ? R.string.wi_day_fog : R.string.wi_night_fog;
            }
            else if (code < 720) {
                return R.string.wi_smoke;
            }
            else if (code < 730) {
                return isDay ? R.string.wi_day_haze : R.string.wi_dust;
            }
            else if (code < 740) {
                return R.string.wi_dust;
            }
            else if (code < 750) {
                return isDay ? R.string.wi_day_fog : R.string.wi_night_fog;
            }
            else if (code < 760) {
                return R.string.wi_sandstorm;
            }
            else if (code < 770) {
                return R.string.wi_dust;
            }
            else if (code < 780) {
                return isDay ? R.string.wi_day_thunderstorm : R.string.wi_night_alt_thunderstorm;
            }
            else if (code < 790) {
                return R.string.wi_tornado;
            }
            else {
                return R.string.wi_dust;
            }
        }
        else if (code < 810) {
            if (code == 800) {
                return isDay ? R.string.wi_day_sunny : R.string.wi_night_clear;
            }
            else if (code == 801 || code == 802) {
                return isDay ? R.string.wi_day_sunny_overcast : R.string.wi_night_alt_partly_cloudy;
            }
            else if (code < 810) {
                return isDay ? R.string.wi_day_cloudy : R.string.wi_night_alt_cloudy;
            }
        }

        // else
        Log.e(TAG, "Unknown weather code received");
        return R.string.wi_thermometer_exterior;
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
            // look at the API guide https://openweathermap.org/api/one-call-3#data
            JSONObject now = lastJSON.getJSONObject("current");
            temp = now.getInt("temp");

            weatherCode = now.getJSONArray("weather").getJSONObject(0).getInt("id");
            // precipitation codes representing active precipitation
            // see https://openweathermap.org/weather-conditions#Weather-Condition-Codes-2
            final int firstDigit = weatherCode/100;
            isPrecip = firstDigit == 2 || firstDigit == 5 || firstDigit == 6;

            // JSON > daily > data > 0 (today's datapoint)
            JSONObject today = lastJSON.getJSONArray("daily").getJSONObject(0);
            min = (int) today.getJSONObject("temp").getDouble("min");
            max = (int) today.getJSONObject("temp").getDouble("max");
            sunRise = today.getLong("sunrise");
            sunSet = today.getLong("sunset");
        }
        catch (JSONException e)
        {
            Log.e(TAG, e.getMessage());
        }

        // Rain data isn't always available so fetch it separately
        precipChance = 0;
        try
        {
            JSONObject today = lastJSON.getJSONArray("daily").getJSONObject(0);
            precipChance = (float) today.getDouble("pop");
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
