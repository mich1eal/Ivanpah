package com.mich1eal.ivanpah.activities;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mich1eal.ivanpah.R;
import com.mich1eal.ivanpah.Weather;

import java.util.Calendar;


public class Mirror extends Activity
    implements Weather.WeatherUpdateListener
{
    private static final String TAG = "MIRROR";

    private final static long weatherDelay = 15 * 60 * 1000; //Time between weather updates in millis
    private final static double minRainDisplay = .1; //Minimum threshold for displaying rain prob

    private static TextView temp, max, min, icon, precipType, precipPercent, messageDisplay;
    private static LinearLayout precipTile, allWeather;

    private static Typeface weatherFont, iconFont, defaultFont;
    private static Weather weather;
    private static boolean hasLightSensor;

    private static boolean dimEnabled = true;

    private static int brightness;

    private static int fullDarkLevel = 100;
    private static int fullBrightlevel = 255;

    // Offsets between sunsets and sunrise for dimming purposes, in seconds
    private static long sunriseOffset = 60 * 60;
    private static long sunsetOffset = 120 * 60;


    @Override
    public void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mirror);

        // Initialize views that will need to be updated
        temp = (TextView) findViewById(R.id.temp);
        max = (TextView) findViewById(R.id.max);
        min = (TextView) findViewById(R.id.min);
        icon = (TextView) findViewById(R.id.icon);
        precipTile = (LinearLayout) findViewById(R.id.precipTile);
        precipType = (TextView) findViewById(R.id.precipType);
        precipPercent = (TextView) findViewById(R.id.precipPercent);
        messageDisplay = (TextView) findViewById(R.id.messageView);
        allWeather = (LinearLayout) findViewById(R.id.allWeather);


        // Stuff for handling screen brightness
        Settings.System.putInt(getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        hasLightSensor = lightSensor != null;
        if (lightSensor == null)
            Toast.makeText(this, "No light sensor available, using clock instead", Toast.LENGTH_LONG).show();
        else if (dimEnabled) {
            final float maxLight = lightSensor.getMaximumRange();

            final double fullBright = .4;
            final double fullDark = .1;

            sensorManager.registerListener(new SensorEventListener() {
                //Called when sensor gets new values
                @Override
                public void onSensorChanged(SensorEvent event) {
                    if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                        float current = event.values[0];

                        int out;
                        if (current < fullDark) out = fullDarkLevel;
                        else if (current > fullBright) out = fullBrightlevel;
                        else {
                            double percent = (current - fullDark) / (fullBright - fullDark);
                            out = (int) percent * (fullBrightlevel - fullDarkLevel) + fullDarkLevel;
                        }

                        Log.d(TAG, "Setting brightness to: " + out);
                        Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, out);

                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {

                }
            }, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }


        Location permLoc = new Location("Pittsburgh");
        // These are values for Pittsburgh PA
        permLoc.setLatitude(40.4406);
        permLoc.setLongitude(-79.9959);

        // These for Indy
        //permLoc.setLatitude(39.7739318);
        //permLoc.setLongitude(-86.15002229999999);

        // Initialize data fetchers
        weather = new Weather(this);
        weather.setLocation(permLoc);

        // Initialize fonts
        defaultFont = icon.getTypeface();
        weatherFont = Typeface.createFromAsset(getAssets(), "fonts/weather.ttf");
        iconFont = Typeface.createFromAsset(getAssets(), "fonts/heydings_icons.ttf");

        // Initialize constant fonts
        precipType.setTypeface(weatherFont);

        weather.setAutoUpdate(new Handler(), weatherDelay);

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        setImmersive();
    }

    @Override
    public void onWeatherDataChange()
    {
        Calendar c = Calendar.getInstance();

        int date = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_WEEK);
        int dayCount = c.get(Calendar.DAY_OF_WEEK_IN_MONTH);


        // Birthday
        if (month == Calendar.FEBRUARY && date == 24)
        {
            allWeather.setVisibility(View.GONE);
            messageDisplay.setText("HAPPY\nBIRTHDAY\nMOM!!!\n");
            messageDisplay.setVisibility(View.VISIBLE);
            messageDisplay.setTextSize(125);
        }
        // Mothers day
        else if (month == Calendar.MAY && day == Calendar.SUNDAY && dayCount == 2)
        {
            allWeather.setVisibility(View.GONE);
            messageDisplay.setText("HAPPY\nMOTHER'S\nDAY!!!\n");
            messageDisplay.setVisibility(View.VISIBLE);
            messageDisplay.setTextSize(125);
        }
        //Anniversary
        else if (month == Calendar.JUNE && date == 18)
        {
            allWeather.setVisibility(View.GONE);
            messageDisplay.setText("HAPPY\nANNIVERSARY!!!\n");
            messageDisplay.setTextSize(75);
            messageDisplay.setVisibility(View.VISIBLE);
        }

        else if (month == Calendar.SEPTEMBER && date == 30)
        {
            allWeather.setVisibility(View.GONE);
            messageDisplay.setText("HAPPY\nBIRTHDAY\nDAD!!!\n");
            messageDisplay.setTextSize(75);
            messageDisplay.setVisibility(View.VISIBLE);
        }

        else
        {
            allWeather.setVisibility(View.VISIBLE);
            messageDisplay.setVisibility(View.GONE);

            //Set temps
            temp.setText(weather.getTemp() + " \u2109");
            min.setText(weather.getMin() + " \u2109");
            max.setText(weather.getMax() + " \u2109");

            // Set master icon
            int iconId = weather.getConditionIconId(c.getTimeInMillis() / 1000);
            try
            {
                icon.setText(getResources().getString(iconId));
                icon.setTypeface(weatherFont);
            }
            catch(Exception e)
            {
                Log.d("MAIN", "Unable to set icon to " + iconId);
                icon.setText("-");
                icon.setTypeface(defaultFont);
            }

            // Set precip tile
            //If precip chance is over threshold and its not already raining
            if (weather.getPrecipChance() >= minRainDisplay && !weather.isPrecip())
            {

                //convert float to int%
                String str = String.valueOf((int) (weather.getPrecipChance() * 100)) + '%';
                precipPercent.setText(str);
                precipTile.setVisibility(View.VISIBLE);
            }
            else precipTile.setVisibility(View.GONE);
        }


        if (!hasLightSensor && dimEnabled)

        {
            // Handle screen dimming stuff
            long sunset = weather.getSunSet();
            long sunrise = weather.getSunrise();
            long now = System.currentTimeMillis() / 1000;
            int out;

            // Incase something goes wrong, just stay bright
            if (sunset == -1 || sunrise == -1)
            {
                out = fullBrightlevel;
            }
            else if (now < sunrise - sunriseOffset) //pre sunrise
            {
                out = fullDarkLevel;

            }
            else if (now < sunrise) //sunrise
            {
                double percent = 1 - ((double) sunrise - now) / sunriseOffset;
                out = (int) (percent * (fullBrightlevel - fullDarkLevel)) + fullDarkLevel;

            }
            else if (now < sunset) //day
            {
                out = fullBrightlevel;
            }
            else if (now < sunset + sunsetOffset) //dusk
            {
                double percent = ((double)sunset + sunsetOffset - now) / sunsetOffset;
                out = (int) (percent * (fullBrightlevel - fullDarkLevel)) + fullDarkLevel;
            }
            else //night
            {
                Log.d(TAG, "Night");
                out = fullDarkLevel;
            }


            Log.d(TAG, "Setting brightness to: " + out);
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, out);
        }

    }

    private void setImmersive()
    {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

}