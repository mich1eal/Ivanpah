package com.mich1eal.ivanpah.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mich1eal.ivanpah.BWrapper;
import com.mich1eal.ivanpah.Duolingo;
import com.mich1eal.ivanpah.MirrorAlarm;
import com.mich1eal.ivanpah.R;
import com.mich1eal.ivanpah.Weather;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimerTask;

public class Mirror extends Activity
    implements Weather.WeatherUpdateListener
{
    private static final String TAG = Controller.class.getSimpleName();

    private final static long weatherDelay = 5 * 60 * 1000; //Time between weather updates in millis
    private final static long duoDelay = 5 * 1000; // Time between duo updates in millis
    private final static double minRainDisplay = .1; //Minimum threshold for displaying rain prob
    private final static int DUO_SNOOZE = 2; // Minutes to snooze each time a heartbeat is recieved

    private static TextView temp, max, min, icon,precipType, precipPercent, alarmIcon, alarmText;
    private static LinearLayout precipTile;

    private static Typeface weatherFont, iconFont, defaultFont;
    private static BWrapper bWrap;
    private static Weather weather;
    private static Duolingo duolingo;
    private static MirrorAlarm alarm;

    private static boolean activeDuo = false;
    private static boolean upcomingDuo = false;

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

        // Initialize data fetchers
        weather = new Weather((LocationManager) getSystemService(LOCATION_SERVICE), this);
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

        // Initialize fonts
        defaultFont = icon.getTypeface();
        weatherFont = Typeface.createFromAsset(getAssets(), "fonts/weather.ttf");
        iconFont = Typeface.createFromAsset(getAssets(), "fonts/heydings_icons.ttf");

        // Initialize constant fonts
        precipType.setTypeface(weatherFont);
        alarmIcon.setTypeface(iconFont);

        // Initialize MirrorAlarm
        alarm = new MirrorAlarm(this);
        alarm.setAlarmListener(new MirrorAlarm.AlarmListener()
        {
            @Override
            public void onAlarmSet(Calendar time)
            {
                // Set UI
                String dispString = alarmFormat.format(time.getTime());
                if (upcomingDuo) dispString += " - Duolingo";
                alarmText.setText(dispString);
                alarmText.setVisibility(View.VISIBLE);
                alarmIcon.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancel()
            {
                alarmText.setVisibility(View.GONE);
                alarmIcon.setVisibility(View.GONE);
            }

            @Override
            public void onRing()
            {
                if (upcomingDuo) startDuolingo();
            }

        });

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

    private static void handleCancel()
    {
        upcomingDuo = false;
        //Alarm can't be canceld while duolingo is active
        if (!activeDuo) alarm.cancel();
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
            alarm.setAlarm(cal);
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
        bWrap.close();
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
                final String[] args = msg.split(BWrapper.MESSAGE_DELIM);
                Log.d(TAG, "Recieved string: " + msg);

                // Message will be heartbeat, cancel, a long indicated alarm time,
                // or a duolingo username, then alarm time
                if (msg.equals(BWrapper.MESSAGE_HEARTBEAT)) handleHeartbeat();
                else if (msg.equals(BWrapper.MESSAGE_CANCEL)) handleCancel();
                else if (!activeDuo)
                {
                    if (args.length > 1)
                    {
                        upcomingDuo = true;
                        duolingo.setUsername(args[1]);
                    }
                    else
                    {
                        upcomingDuo = false;
                    }
                    final Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(Long.parseLong(args[0]));
                    alarm.setAlarm(cal);
                }
            }
        }
    }
}