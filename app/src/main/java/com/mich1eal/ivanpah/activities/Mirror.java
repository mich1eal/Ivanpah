package com.mich1eal.ivanpah.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mich1eal.ivanpah.AlarmService;
import com.mich1eal.ivanpah.BWrapper;
import com.mich1eal.ivanpah.Dimmer;
import com.mich1eal.ivanpah.Duolingo;
import com.mich1eal.ivanpah.MirrorAlarm;
import com.mich1eal.ivanpah.R;
import com.mich1eal.ivanpah.Weather;

import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class Mirror extends Activity
    implements Weather.WeatherUpdateListener
{
    private static final String TAG = "MIRROR";

    private final static String PREFS_ALARM = "ALARM_TIME_PREFERENCE";

    private final static long weatherDelay = 10 * 60 * 1000; //Time between weather updates in millis
    private final static long duoDelay = 5 * 1000; // Time between duolingo updates in millis
    private final static double minRainDisplay = .1; //Minimum threshold for displaying rain prob
    private final static int DUO_SNOOZE = 2; // Minutes to snooze each time a heartbeat is recieved

    private static String hueIP;
    private static int defaultMinsToHue = 20;
    public static int hueMins = defaultMinsToHue;

    private static TextView temp, max, min, icon, precipType, precipPercent, alarmIcon, alarmText, messageDisplay;
    private static LinearLayout precipTile, allWeather, mirrorRoot;

    private static Typeface weatherFont, iconFont, defaultFont;
    private static BWrapper bWrap;
    private static Weather weather;
    private static Duolingo duolingo;
    private static MirrorAlarm alarm;
    private static Dimmer dimmer;

    private static boolean activeDuo = false, upcomingDuo = false;

    static SharedPreferences preferences;

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
        mirrorRoot = (LinearLayout) findViewById(R.id.mirror_root) ;

        // Set up shared preferences for alarm saving
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        dimmer = new Dimmer(getContentResolver());

        //Initialize weather
        weather = new Weather(this);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null)
        {
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, new LocationListener(){

                @Override
                public void onLocationChanged(Location location)
                {
                    Log.d(TAG, "onLocationChanged");
                    if (location != null)
                    {
                        weather.setLocation(location);
                    }

                }
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override
                public void onProviderEnabled(String provider) {}
                @Override
                public void onProviderDisabled(String provider) {}
            }, null);
        }
        weather.setAutoUpdate(new Handler(), weatherDelay);

        // Initialize fonts
        defaultFont = icon.getTypeface();
        weatherFont = Typeface.createFromAsset(getAssets(), "fonts/weather.ttf");
        iconFont = Typeface.createFromAsset(getAssets(), "fonts/heydings_icons.ttf");

        // Initialize constant fonts
        precipType.setTypeface(weatherFont);
        alarmIcon.setTypeface(iconFont);

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
        if (alarm.isRinging())
        {
            Log.d(TAG, "Alarm is currently ringing!");
            setRing(true);
        }
        else if (alarm.isSet())
        {
            Log.d(TAG, "A set alarm was detected");
            //If alarm is set, show it
            Calendar alarmTime = getSavedAlarmTime();

            if (alarmTime == null)
            {
                Log.e(TAG, "ERROR, if an alarm  has been set Alarmtime should not be null");
                Log.d(TAG, "Showing no alarm");
                displayAlarm(null);
            }

            else
            {
                Log.d(TAG, "A set alarm was detected");
                displayAlarm(alarmTime);
            }
        }
        else
        {
            //If no alarm, set to day mode
            displayAlarm(null);

            Log.d(TAG, "No alarm found");
        }

        // Setup broadcast receiver for alarm updates
        BroadcastReceiver uiReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                Log.d(TAG, "Ring broadcast received by UI");
                setRing(true);
            }
        };

        this.registerReceiver(uiReceiver, new IntentFilter(AlarmService.ACTION_RING));
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

        /*
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
        }*/

        //else

        {
            allWeather.setVisibility(View.VISIBLE);
            messageDisplay.setVisibility(View.GONE);

            //Set temps
            temp.setText(weather.getTemp() + " \u2109");
            min.setText(weather.getMin() + " \u2109");
            max.setText(weather.getMax() + " \u2109");

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
    }

    private static void handleCancel()
    {
        upcomingDuo = false;
        //Alarm can't be cancelled while duolingo is active
        if (!activeDuo)
        {
            setRing(false);
            alarm.cancel();
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
        alarm.cancel();
        activeDuo = false;
    }

    // put off alarm by another 30 seconds
    private static void handleHeartbeat()
    {
        if (activeDuo)
        {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, DUO_SNOOZE);
            setAlarm(cal);
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

    private static void setRing(boolean isRinging)
    {
        if (isRinging)
        {
            if (upcomingDuo) startDuolingo();
        }
        else
        {
            displayAlarm(null);
        }
    }
    private Calendar getSavedAlarmTime()
    {
        long alarmTime = preferences.getLong(PREFS_ALARM, -1);
        if (alarmTime != -1)
        {
            Calendar calendar = Calendar.getInstance();

            calendar.setTimeInMillis(alarmTime);
            return calendar;
        }
        return null;
    }

    private static void saveAlarmTime(Calendar alarmTime)
    {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(PREFS_ALARM, alarmTime.getTimeInMillis());
        editor.apply();
    }

    private static void setAlarm(Calendar calendar)
    {
        alarm.setAlarm(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), hueMins, hueIP);

        saveAlarmTime(calendar);
        displayAlarm(calendar);
    }

    private static void displayAlarm(Calendar calendar)
    {
        //Alarm has been cancelled
        if (calendar == null)
        {
            alarmText.setVisibility(View.GONE);
            alarmIcon.setVisibility(View.GONE);

            dimmer.setBright(true, preferences);

        }
        //Alarm has been set
        else
        {
            String dispString = alarmFormat.format(calendar.getTime());
            if (upcomingDuo || activeDuo) dispString += " - Duolingo";

            alarmText.setText(dispString);
            alarmText.setVisibility(View.VISIBLE);
            alarmIcon.setVisibility(View.VISIBLE);

            dimmer.setBright(false, preferences);
        }
    }

    private static void setBrightness(boolean bright, int value)
    {
        dimmer.updateBrightness(preferences, value, bright);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d(TAG, "onActivityResult");
        if (requestCode == BWrapper.BLUETOOTH_RESPONSE && resultCode == BWrapper.BLUETOOTH_OK)
        {
            Log.d(TAG, "Bluetooth enabled");
            bWrap.onServerInit();
        }
    }

    @Override
    public void onPause()
    {
        if (bWrap != null)
        {
            bWrap.close();
        }
        super.onPause();
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
                else if (msg.equals(BWrapper.MESSAGE_CANCEL)) handleCancel();

                else
                {
                    String tempDuoUserName = null;
                    String tempHueIP = null;
                    int tempHueTime = -1;
                    long tempAlarmTime = -1;

                    int tempBrightLevel = -1;
                    boolean bright = false;


                    JSONObject json;
                    try
                    {
                        json = new JSONObject(msg);
                        if (json.has(BWrapper.alarmTime)) tempAlarmTime = json.getLong(BWrapper.alarmTime);
                        if (json.has(BWrapper.username)) tempDuoUserName = json.getString(BWrapper.username);
                        if (json.has(BWrapper.hueTime)) tempHueTime = json.getInt(BWrapper.hueTime);
                        if (json.has(BWrapper.hueIP)) tempHueIP = json.getString(BWrapper.hueIP);
                        if (json.has(BWrapper.dayBright))
                        {
                            tempBrightLevel = json.getInt(BWrapper.dayBright);
                            bright = true;
                        }
                        if (json.has(BWrapper.nightBright))
                        {
                            tempBrightLevel = json.getInt(BWrapper.nightBright);
                            bright = false;
                        }

                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    if (tempBrightLevel != -1)
                    {
                        Log.d(TAG, "Temp bright level = " + tempBrightLevel);
                        setBrightness(bright, tempBrightLevel);
                    }

                    if (tempAlarmTime != -1)
                    {
                        if (!activeDuo)
                        {
                            if (tempHueIP != null)
                            {
                                hueIP = tempHueIP;
                                hueMins = tempHueTime;
                            }
                            else
                            {
                                hueIP = null;
                                hueMins = -1;
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
                            setAlarm(cal);
                        }
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