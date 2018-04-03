package com.mich1eal.ivanpah.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mich1eal.ivanpah.BWrapper;
import com.mich1eal.ivanpah.Duolingo;
import com.mich1eal.ivanpah.MirrorAlarm;
import com.mich1eal.ivanpah.R;
import com.mich1eal.ivanpah.Weather;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;


public class Mirror extends Activity
    implements Weather.WeatherUpdateListener
{
    private static final String TAG = "MIRROR";

    private final static long weatherDelay = 10 * 60 * 1000; //Time between weather updates in millis
    private final static long duoDelay = 5 * 1000; // Time between duolingo updates in millis
    private final static double minRainDisplay = .1; //Minimum threshold for displaying rain prob
    private final static int DUO_SNOOZE = 2; // Minutes to snooze each time a heartbeat is recieved

    private final static String HUE_URL_BASE = "http://";
    private final static String HUE_URL_END1 = "/api/qsrRaSO7t7sb6MyAa8saqzgCZMCelVPNUIY1qJWL/lights/2/state";
    private final static String HUE_URL_END2 = "/api/qsrRaSO7t7sb6MyAa8saqzgCZMCelVPNUIY1qJWL/lights/3/state";


    private static TextView temp, max, min, icon,precipType, precipPercent, alarmIcon, alarmText, messageDisplay;
    private static LinearLayout precipTile, allWeather;

    private static Typeface weatherFont, iconFont, defaultFont;
    private static BWrapper bWrap;
    private static Weather weather;
    private static Duolingo duolingo;
    private static MirrorAlarm alarm;
    private static boolean hasLightSensor;

    private static boolean dimEnabled = false;

    private static boolean activeDuo = false, upcomingDuo = false, hasDuo = true;

    private static int brightness;

    private static int fullDarkLevel = 0;
    private static int fullBrightlevel = 255;

    // Offsets between sunsets and sunrise for dimming purposes, in seconds
    private static long sunriseOffset = 60 * 60;
    private static long sunsetOffset = 120 * 60;

    private static boolean hueEnabled = false;
    private static String hueIP;
    private static int hueMins;



    //Format for alarm
    private static final SimpleDateFormat alarmFormat = new SimpleDateFormat("h:mm a");

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

        Boolean hasDuo = getSharedPreferences(getString(R.string.prefs), MODE_PRIVATE).getBoolean(Setup.hasDuo, true);

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
        allWeather = (LinearLayout) findViewById(R.id.allWeather);



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




        Location permLoc = null;
        if (getSharedPreferences(getString(R.string.prefs), MODE_PRIVATE).getBoolean(Setup.alwaysPittsburgh, false))
        {
            permLoc = new Location("Vienna");
            // These are values for Pittsburgh PA
            //permLoc.setLatitude(40.4406);
            //permLoc.setLongitude(-79.9959);

            // These for Indy
            //permLoc.setLatitude(39.7739318);
            //permLoc.setLongitude(-86.15002229999999);

            // These for Vienna
            permLoc.setLatitude(48.2082);
            permLoc.setLongitude(16.3738);
        }

        // Initialize data fetchers
        weather = new Weather((LocationManager) getSystemService(LOCATION_SERVICE), this, permLoc);

        // Initialize fonts
        defaultFont = icon.getTypeface();
        weatherFont = Typeface.createFromAsset(getAssets(), "fonts/weather.ttf");
        iconFont = Typeface.createFromAsset(getAssets(), "fonts/heydings_icons.ttf");

        // Initialize constant fonts
        precipType.setTypeface(weatherFont);
        alarmIcon.setTypeface(iconFont);

        weather.setAutoUpdate(new Handler(), weatherDelay);


        // ***************************** DUOLINGO ONLY *****************************
        /* EVERYTHING BELOW HERE WILL ONLY HAPPEN IF DUOLINGO IS ENABLED IN SETTINGS */
        if (!hasDuo) return;

        duolingo = new Duolingo();
        duolingo.setOnStreakListener(new Duolingo.OnNewStreakListener()
        {
            // Once a new duolingo streak is achieved, the duolingo phase ends
            @Override
            public void onNewStreak()
            {
                endDuolingo();
            }
        });

        // Set up bluetooth
        bWrap = new BWrapper(this, new BHandler(), true);
        bWrap.setAutoReconnect(true);

        // Initialize MirrorAlarm
        alarm = new MirrorAlarm(this);
        alarm.setAlarmListener(new MirrorAlarm.AlarmListener()
        {
            @Override
            public void onAlarmSet(Calendar time, boolean hueEnabledForAlarm)
            {
                // Set UI
                String dispString = alarmFormat.format(time.getTime());
                if (upcomingDuo || activeDuo) dispString += " - Duolingo";
                alarmText.setText(dispString);
                alarmText.setVisibility(View.VISIBLE);
                alarmIcon.setVisibility(View.VISIBLE);

                hueEnabled = hueEnabledForAlarm;

                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, fullDarkLevel);
            }

            @Override
            public void onCancel()
            {
                alarmText.setVisibility(View.GONE);
                alarmIcon.setVisibility(View.GONE);
                upcomingDuo = false;
                hueEnabled = false;
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, fullBrightlevel);
            }

            @Override
            public void onRing()
            {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, fullBrightlevel);
                if (upcomingDuo) startDuolingo();
            }

        });


        //Stuff for hue

        Timer timer = new Timer();
        TimerTask task = new TimerTask()
        {
            public void run()
            {
                if (hueEnabled)
                {
                    // how much time till alarm
                    long timeLeft = alarm.timeToAlarm();
                    if (timeLeft > 0)
                    {
                        long brightTime = hueMins * 60 * 1000; // time to full brightness in milliseconds
                        if (timeLeft < brightTime)
                        {
                            int bright = (int) ((brightTime - timeLeft) * 255 / brightTime);

                            new URLCaller().execute(HUE_URL_BASE + hueIP + HUE_URL_END1,
                                    "{\"on\":true, \"bri\":" + bright + "}");

                            new URLCaller().execute(HUE_URL_BASE + hueIP + HUE_URL_END2,
                                    "{\"on\":true, \"bri\":" + bright + "}");
                        }
                    }

                }
            }
        };


        timer.schedule(task, 0, 5 * 1000);
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
            allWeather.setVisibility(View.GONE);
            messageDisplay.setText("HAPPY\nBIRTHDAY\nTRIXI!!\n");
            messageDisplay.setVisibility(View.VISIBLE);
            messageDisplay.setTextSize(125);
        }
        // Anniversary
        else if (month == Calendar.MAY && date == 13)
        {
            allWeather.setVisibility(View.GONE);
            messageDisplay.setText("Shönen\nJahrestag!!\n");
            messageDisplay.setVisibility(View.VISIBLE);
            messageDisplay.setTextSize(125);
        }
        //Nicholas Day
        else if (month == Calendar.DECEMBER && date == 6)
        {
            allWeather.setVisibility(View.GONE);
            messageDisplay.setText("Frohen\nNikolaus!!\n");
            messageDisplay.setTextSize(75);
            messageDisplay.setVisibility(View.VISIBLE);
        }

        //Random
        else if (date == 17 && month % 2 == 0)
        {
            allWeather.setVisibility(View.GONE);
            messageDisplay.setText("ICH\nLIEBE\nDICH!\n");
            messageDisplay.setTextSize(75);
            messageDisplay.setVisibility(View.VISIBLE);
        }


        else
        {
            allWeather.setVisibility(View.VISIBLE);
            messageDisplay.setVisibility(View.GONE);

            //Set temps
            temp.setText(weather.getC(weather.getTemp()) + " \u2103");
            min.setText(weather.getC(weather.getMin()) + " \u2103");
            max.setText(weather.getC(weather.getMax()) + " \u2103");

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

    private static void startDuolingo()
    {
        if (activeDuo == false) duolingo.setAutoUpdate(new Handler(), duoDelay);
        activeDuo = true;
        upcomingDuo = false;
    }

    private static void endDuolingo()
    {
        duolingo.cancelAutoUpdate();
        activeDuo = false;
        alarm.cancel();
    }

    // put off alarm by another 30 seconds
    private static void handleHeartbeat()
    {
        if (activeDuo)
        {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, DUO_SNOOZE);
            alarm.setAlarm(cal, hueEnabled);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d("MIRROR", "onActivityResult");
        if (requestCode == BWrapper.BLUETOOTH_RESPONSE && resultCode == BWrapper.BLUETOOTH_OK)
        {
            Log.d("MIRROR", "Bluetooth enabled");
            bWrap.onServerInit();
        }
    }

    @Override
    public void onDestroy()
    {
        if (bWrap != null)
        {
            bWrap.close();
        }
        super.onDestroy();
    }

    public TimerTask snoozeTimer(final Handler handler)
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
                        alarm.ring();
                    }
                });
            }
        };
    }

    static class BHandler extends Handler
    {
        @Override
        public void handleMessage(Message inputMessage)
        {
            if (inputMessage.what == BWrapper.MESSAGE_READ)
            {
                final String msg = (String) inputMessage.obj;
                Log.d(TAG, "Recieved string: " + msg);

                // Message will be heartbeat, cancel, a long indicated alarm time,
                // or a duolingo username, then alarm time
                if (msg.equals(BWrapper.MESSAGE_HEARTBEAT)) handleHeartbeat();
                else if (msg.equals(BWrapper.MESSAGE_CANCEL))
                {
                    if (!activeDuo) alarm.cancel();
                }

                else
                {
                    String tempDuoUserName = null;
                    String tempHueIP = null;
                    int tempHueTime = -1;
                    long tempAlarmTime = -1;

                    JSONObject json;
                    try
                    {
                        json = new JSONObject(msg);
                        if (json.has(BWrapper.alarmTime)) tempAlarmTime = json.getLong(BWrapper.alarmTime);
                        if (json.has(BWrapper.username)) tempDuoUserName = json.getString(BWrapper.username);
                        if (json.has(BWrapper.hueTime)) tempHueTime = json.getInt(BWrapper.hueTime);
                        if (json.has(BWrapper.hueIP)) tempHueIP = json.getString(BWrapper.hueIP);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    if (!activeDuo)
                    {
                        if (tempHueIP != null)
                        {
                            hueEnabled = true;
                            hueIP = tempHueIP;
                            hueMins = tempHueTime;
                            Log.d(TAG, "Hue enabled, IP set to: " + hueIP);
                        }
                        else
                        {
                            hueEnabled = false;
                        }

                        if (tempDuoUserName != null)
                        {
                            upcomingDuo = true;
                            duolingo.setUsername(tempDuoUserName);
                        }
                        else
                        {
                            upcomingDuo = false;
                        }

                        final Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(tempAlarmTime);
                        alarm.setAlarm(cal, hueEnabled);
                    }

                }
            }
        }
    }

    public static class URLCaller extends AsyncTask<String, Void, Void>
    {
        @Override
        protected Void doInBackground(String... params)
        {
            HttpURLConnection con = null;
            URL url = null;
            OutputStreamWriter out = null;

            try
            {
                url = new URL(params[0]);

                con = (HttpURLConnection) url.openConnection();;

                con.setDoOutput(true);
                con.setRequestMethod("PUT");
                out = new OutputStreamWriter(con.getOutputStream());
                Log.d(TAG, "Writing output: " + params[1]);
                out.write(params[1]);
                out.close();

                con.getResponseMessage(); //This is necessary to actually make the call I guess?
                Log.d(TAG, "Response code from hue = " + con.getResponseCode());
                Log.d(TAG, "Response message from hue = " + con.getResponseMessage());
            }
            catch (Exception e)
            {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }

            return null;
        }
    }
}