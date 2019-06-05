package com.mich1eal.ivanpah;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

/**
 * Created by msmil on 6/4/2019.
 */


public class Dimmer
{
    private static final String TAG = "MIRROR";
    private static ContentResolver contentResolver;
    private static int fullDarkLevel = 0;
    private static int fullBrightlevel = 255;


    public Dimmer(ContentResolver contentResolver)
    {
        this.contentResolver = contentResolver;

        Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    public void setBright(boolean bright)
    {
        if(contentResolver == null)
        {
            Log.e(TAG, "Error: contentResolver is null");
            return;
        }

        if (bright) Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, fullBrightlevel);
        else Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, fullDarkLevel);
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
