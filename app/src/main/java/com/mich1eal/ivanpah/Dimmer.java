package com.mich1eal.ivanpah;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

/**
 * Created by msmil on 6/4/2019.
 */

public class Dimmer
{
    private static final String TAG = "DIMMER";
    private static ContentResolver contentResolver;
    private static int defaultNight = 20;
    private static int defaultDay = 100;

    private static boolean isBright = true;

    private static String dayLevel = "DAY_LEVEL";
    private static String nightLevel = "NIGHT_LEVEL";

    public Dimmer(ContentResolver contentResolver)
    {
        this.contentResolver = contentResolver;

        Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    public void updateBrightness(SharedPreferences prefs, int value, boolean bright)
    {
        if (bright) prefs.edit().putInt(dayLevel, value).apply();
        else prefs.edit().putInt(nightLevel, value).apply();

        // if updating the value that is currently displayed, update the display
        if (bright == isBright)
        {
            setBright(bright, prefs);
        }
    }

    public void setBright(boolean bright, SharedPreferences prefs)
    {
        if(contentResolver == null)
        {
            Log.e(TAG, "Error: contentResolver is null");
            return;
        }

        int brightness;
        if (bright)
        {
            brightness = prefs.getInt(dayLevel, defaultDay);
        }
        else
        {
            brightness = prefs.getInt(nightLevel, defaultNight);
        }

        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness * 255 / 100);
        isBright = bright;
    }

    /*
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
    }*/


    /*SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    hasLightSensor = lightSensor != null;
        if (lightSensor == null) Toast.makeText(this,"No light sensor available, using clock instead", Toast.LENGTH_LONG).show();
        else if (dimEnabled)
    {
        final float maxLight = lightSensor.getMaximumRange();

        final double fullBright = .4;
        final double fullDark = .1;

        sensorManager.registerListener(new SensorEventListener()
        {
            //Called when sensor gets new values
            @Override
            public void onSensorChanged(SensorEvent event)
            {
                if (event.sensor.getType() == Sensor.TYPE_LIGHT)
                {
                    float current = event.values[0];

                    int out;
                    if (current < fullDark) out = fullDarkLevel;
                    else if (current > fullBright) out = fullBrightlevel;
                    else
                    {
                        double percent = (current - fullDark) / (fullBright - fullDark);
                        out = (int) percent * (fullBrightlevel - fullDarkLevel) + fullDarkLevel;
                    }

                    Log.d(TAG, "Setting brightness to: " + out);
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, out);

                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy)
            {

            }
        }, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }*/
}
