package com.mich1eal.ivanpah.activities;

import android.app.Activity;
import android.app.TimePickerDialog;
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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.mich1eal.ivanpah.MirrorAlarm;
import com.mich1eal.ivanpah.R;
import com.mich1eal.ivanpah.Weather;

import java.text.SimpleDateFormat;
import java.util.Calendar;


public class Mirror extends Activity
    implements Weather.WeatherUpdateListener
{
    private static final String TAG = "MIRROR";

    private final static long weatherDelay = 10 * 60 * 1000; //Time between weather updates in millis
    private final static long duoDelay = 5 * 1000; // Time between duolingo updates in millis
    private final static double minRainDisplay = .1; //Minimum threshold for displaying rain prob


    private static TextView temp, max, min, icon,precipType, precipPercent, alarmIcon, alarmText, messageDisplay;
    private static LinearLayout precipTile;

    private static Typeface weatherFont, iconFont, defaultFont;
    private static Weather weather;
    private static MirrorAlarm alarm;
    private static boolean hasLightSensor;

    private static Button alarmButton;

    private static boolean dimEnabled = false;

    private static int brightness;

    private static int fullDarkLevel = 0;
    private static int fullBrightlevel = 255;

    // Offsets between sunsets and sunrise for dimming purposes, in seconds
    private static long sunriseOffset = 60 * 60;
    private static long sunsetOffset = 120 * 60;


    //Format for alarm
    private static final SimpleDateFormat alarmFormat = new SimpleDateFormat("H:mm");

    @Override
    public void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
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
        alarmIcon = (TextView) findViewById(R.id.alarmIcon);
        alarmText = (TextView) findViewById(R.id.alarmText);
        messageDisplay = (TextView) findViewById(R.id.messageView);
        alarmButton = (Button) findViewById(R.id.alarmButton);


        // Stuff for handling screen brightness

        Settings.System.putInt(getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
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
        }




        Location permLoc = new Location("Vienna");
        // These for Vienna
        permLoc.setLatitude(48.2082);
        permLoc.setLongitude(16.3738);

        //Indy

        permLoc.setLatitude(39.7684);
        permLoc.setLongitude(-86.1581);



        // Initialize data fetchers
        weather = new Weather((LocationManager) getSystemService(LOCATION_SERVICE), this, permLoc);

        // Initialize fonts
        defaultFont = icon.getTypeface();
        weatherFont = Typeface.createFromAsset(getAssets(), "fonts/weather.ttf");
        iconFont = Typeface.createFromAsset(getAssets(), "fonts/heydings_icons.ttf");

        // Initialize constant fonts
        precipType.setTypeface(weatherFont);
        alarmIcon.setTypeface(iconFont);
        alarmButton.setTypeface(iconFont);

        weather.setAutoUpdate(new Handler(), weatherDelay);




        // Initialize MirrorAlarm
        alarm = new MirrorAlarm(this);
        alarm.setAlarmListener(new MirrorAlarm.AlarmListener()
        {
            @Override
            public void onAlarmSet(Calendar time)
            {
                // Set UI
                alarmText.setText(alarmFormat.format(time.getTime()));
                alarmText.setVisibility(View.VISIBLE);
                alarmIcon.setVisibility(View.VISIBLE);

                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, fullDarkLevel);
            }

            @Override
            public void onCancel()
            {
                alarmText.setVisibility(View.GONE);
                alarmIcon.setVisibility(View.GONE);
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, fullBrightlevel);
            }



            @Override
            public void onRing()
            {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, fullBrightlevel);
            }

        });


        // set up alarm button
        alarmButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v)
            {
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);

                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(Mirror.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        alarm.setAlarm(selectedHour, selectedMinute);
                    }
                }, hour, minute, true);//Yes 24 hour time
                mTimePicker.setTitle("Select Time");
                mTimePicker.show();
            }
        }

        );


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
        if (month == Calendar.SEPTEMBER && date == 10)
        {
            messageDisplay.setText("HAPPY\nBIRTHDAY\nTRIXI!!\n");
            messageDisplay.setVisibility(View.VISIBLE);
            messageDisplay.setTextSize(125);
        }
        // Anniversary
        else if (month == Calendar.MAY && date == 13)
        {
            messageDisplay.setText("Shönen\nJahrestag!!\n");
            messageDisplay.setVisibility(View.VISIBLE);
            messageDisplay.setTextSize(125);
        }
        //Nicholas Day
        else if (month == Calendar.DECEMBER && date == 6)
        {
            messageDisplay.setText("Frohen\nNikolaus!!\n");
            messageDisplay.setTextSize(75);
            messageDisplay.setVisibility(View.VISIBLE);
        }

        //Random
        else if (date == 17 && month % 2 == 0)
        {
            messageDisplay.setText("ICH\nLIEBE\nDICH!\n");
            messageDisplay.setTextSize(75);
            messageDisplay.setVisibility(View.VISIBLE);
        }


        else
        {
            messageDisplay.setVisibility(View.GONE);

            //Set temps

            String tempStr = weather.getC(weather.getTemp()) + "\u00B0";
            String minStr = weather.getC(weather.getMin()) + "\u00B0";
            String maxStr = weather.getC(weather.getMax()) + "\u00B0";

            temp.setText(tempStr);
            min.setText(minStr);
            max.setText(maxStr);

            // Set master icon
            String cond = weather.getCond();
            try
            {
                int id = getResources().getIdentifier(cond, "string", getPackageName());
                icon.setText(getResources().getString(id));
                icon.setTypeface(weatherFont);
            }
            catch(Exception e)
            {
                Log.d("MAIN", "No icon found for String " + cond);
                icon.setText("-");
                icon.setTypeface(defaultFont);
            }

            // Set precip tile
            //If precip chance is over threshold and its not already raining
            if (weather.getPrecipChance() >= minRainDisplay && !weather.isPrecip())
            {
                String precipString = weather.getPrecipType();

                //convert float to int%
                String str = String.valueOf((int) (weather.getPrecipChance() * 100)) + '%';
                precipPercent.setText(str);

                try
                {
                    int id = getResources().getIdentifier(precipString, "string", getPackageName());
                    precipType.setText(getResources().getString(id));
                    precipTile.setVisibility(View.VISIBLE);
                } catch (Exception e)
                {
                    Log.d("MAIN", "No icon found for String " + precipString);
                    precipTile.setVisibility(View.GONE);
                }
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


    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }
}