package com.mich1eal.ivanpah.activities;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.mich1eal.ivanpah.MirrorAlarm;
import com.mich1eal.ivanpah.R;
import com.mich1eal.ivanpah.Weather;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;


public class Mirror extends Activity
    implements Weather.WeatherUpdateListener
{
    private static final String TAG = "MIRROR";

    private final static long weatherDelay = 10 * 60 * 1000; //Time between weather updates in millis
    private final static double minRainDisplay = .1; //Minimum threshold for displaying rain prob

    private static int[] backGroundIDs = {
            R.drawable.background1,
            R.drawable.background2,
            R.drawable.background3,
            R.drawable.background4,
            R.drawable.background5};

    private static Random rand = new Random();


    private boolean isDay = true;


    private static TextView temp, max, min, icon, precipType, precipPercent, alarmIcon, alarmText, messageDisplay;
    private static LinearLayout precipTile, alarmToggle, snoozeLayout;
    private static FrameLayout background;
    private static Button brightnessToggle, snoozeSnooze, snoozeCancel;

    private static Typeface weatherFont, iconFont, defaultFont;
    private static Weather weather;
    private static MirrorAlarm alarm;

    private static boolean dialogCancelButton = true;


    private final static int fullDarkLevel = 0;
    private final static int fullBrightlevel = 255;

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
        messageDisplay = (TextView) findViewById(R.id.messageView);

        alarmToggle = (LinearLayout) findViewById(R.id.alarmToggle);
        alarmIcon = (TextView) findViewById(R.id.alarmIcon);
        alarmText = (TextView) findViewById(R.id.alarmText);

        snoozeSnooze = (Button) findViewById(R.id.snoozeSnooze);
        snoozeCancel = (Button) findViewById(R.id.snoozeCancel);
        snoozeLayout = (LinearLayout) findViewById(R.id.snoozeLayout);

        brightnessToggle = (Button) findViewById(R.id.brightnessToggle);

        background = (FrameLayout) findViewById(R.id.background);

        // Stuff for handling screen brightness

        Settings.System.putInt(getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

        Location permLoc = new Location("Vienna");
        // These for Vienna
        /*permLoc.setLatitude(48.2082);
        permLoc.setLongitude(16.3738);*/

        //Indy
        permLoc.setLatitude(39.7684);
        permLoc.setLongitude(-86.1581);

        // Initialize data fetchers
        weather = new Weather((LocationManager) getSystemService(LOCATION_SERVICE), this, permLoc);
        weather.setAutoUpdate(new Handler(), weatherDelay);

        // Initialize fonts
        defaultFont = icon.getTypeface();
        weatherFont = Typeface.createFromAsset(getAssets(), "fonts/weather.ttf");
        iconFont = Typeface.createFromAsset(getAssets(), "fonts/heydings_icons.ttf");

        // Initialize constant fonts
        precipType.setTypeface(weatherFont);
        alarmIcon.setTypeface(iconFont);
        brightnessToggle.setTypeface(weatherFont);

        // Initialize MirrorAlarm
        alarm = new MirrorAlarm(this);
        alarm.setAlarmListener(new MirrorAlarm.AlarmListener()
        {
            @Override
            public void onAlarmSet(Calendar time)
            {
                alarmText.setText(alarmFormat.format(time.getTime()));
                alarmText.setVisibility(View.VISIBLE);
                setDay(false);
            }

            @Override
            public void onCancel()
            {
                alarmText.setVisibility(View.GONE);
                setDay(true);
            }

            @Override
            public void onRing()
            {
                setRing(true);
            }
        });

        // set up brightness toggle button
        brightnessToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                setDay(!isDay); //toggle brightness
            }
        });

        // set up alarm button
        alarmToggle.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v)
            {
                int hour = alarm.getLastAlarmHour();
                int minute = alarm.getLastAlarmMinute();

                TimePickerDialog timePicker;

                timePicker = new TimePickerDialog(Mirror.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        alarm.setAlarm(selectedHour, selectedMinute);
                        dialogCancelButton = false; //if this is called, was not cancelled with cancel button
                        Log.d(TAG, "onTimeSet()");
                    }

                }, hour, minute, true);//Yes 24 hour time

                timePicker.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog)
                    {
                        dialogCancelButton = false; //if this is called, was not cancelled with cancel button
                    }
                });

                timePicker.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        setImmersive();
                        Log.d(TAG, "onDismissed()");
                        if (dialogCancelButton)
                        {
                            alarm.cancel();
                        }
                        dialogCancelButton = true;
                    }
                });
                timePicker.setTitle("Choose Alarm Time");
                timePicker.show();
            }
        }
        );

        //set up Snooze buttons
        snoozeSnooze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                alarm.snooze();
                setRing(false);
            }
        });

        snoozeCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                alarm.cancel();
                setRing(false);
            }
        });

        setDay(true);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        setImmersive();
    }

    @Override
    public void onWeatherDataChange(Weather.WeatherUpdate update)
    {
        temp.setText(update.temp);
        min.setText(update.min);
        max.setText(update.max);

        String cond = update.icon;
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
        if (update.precipChance > minRainDisplay && !update.isPrecip)
        {
            String str = String.valueOf((int) (update.precipChance * 100)) + '%';
            precipPercent.setText(str);

            try
            {
                int id = getResources().getIdentifier(update.precipIcon, "string", getPackageName());
                precipType.setText(getResources().getString(id));

                Log.d(TAG, "Precip type: " + update.precipIcon + ". Precip prob: " + update.precipChance);
                precipTile.setVisibility(View.VISIBLE);
            } catch (Exception e)
            {
                Log.d("MAIN", "No icon found for String " + update.precipIcon);
                precipTile.setVisibility(View.GONE);
            }
        }
        else precipTile.setVisibility(View.GONE);
    }

    //format either to day or night mode
    private void setDay(boolean day)
    {
        isDay = day;
        if (day) //set day
        {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, fullBrightlevel);
            brightnessToggle.setText(R.string.wi_night_clear);

            if (backGroundIDs != null && backGroundIDs.length > 0)
            {
                int id = backGroundIDs[rand.nextInt(backGroundIDs.length)];

                background.setBackgroundResource(id);
            }
            else
            {
                background.setBackgroundColor(Color.CYAN);
            }
        }
        else // set night
        {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, fullDarkLevel);
            brightnessToggle.setText(R.string.wi_day_sunny);
            background.setBackgroundColor(0x000000);
        }
    }

    private void setRing(boolean isRinging)
    {
        if (isRinging)
        {
            alarmToggle.setVisibility(View.GONE);
            snoozeLayout.setVisibility(View.VISIBLE);
            messageDisplay.setVisibility(View.GONE);

            if (!isDay)
            {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, fullBrightlevel);
            }
        }
        else
        {
            alarmToggle.setVisibility(View.VISIBLE);
            snoozeLayout.setVisibility(View.GONE);
            setMessageDisplay();

            //reset brightness etc to before ring started
            setDay(isDay);
        }
    }

    private void setMessageDisplay()
    {
        Calendar today = Calendar.getInstance();

        int date = today.get(Calendar.DAY_OF_MONTH);
        int month = today.get(Calendar.MONTH);

        // Birthday
        if (month == Calendar.SEPTEMBER && date == 10)
        {
            messageDisplay.setText("HAPPY BIRTHDAY TRIXI!!");
            messageDisplay.setVisibility(View.VISIBLE);
        }
        // Anniversary
        else if (month == Calendar.MAY && date == 13)
        {
            messageDisplay.setText("Shönen Jahrestag!!");
            messageDisplay.setVisibility(View.VISIBLE);
        }
        //Nicholas Day
        else if (month == Calendar.DECEMBER && date == 6)
        {
            messageDisplay.setText("Frohen Nikolaus!!");
            messageDisplay.setVisibility(View.VISIBLE);
        }

        //Random
        else if (date == 17 && month % 2 == 0)
        {
            messageDisplay.setText("ICH LIEBE DICH!");
            messageDisplay.setVisibility(View.VISIBLE);
        }

        else messageDisplay.setVisibility(View.GONE); //Set temps
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
