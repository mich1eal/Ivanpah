package com.mich1eal.ivanpah.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mich1eal.ivanpah.BluetoothWrapper;
import com.mich1eal.ivanpah.Duolingo;
import com.mich1eal.ivanpah.JSONGetter;
import com.mich1eal.ivanpah.R;

import java.util.Timer;
import java.util.TimerTask;

public class Mirror extends Activity
    implements JSONGetter.Weather.WeatherUpdateListener
{
    private static JSONGetter.Weather weather;
    private static Duolingo duolingo;
    private final static long weatherDelay = 5 * 60 * 1000;
    private final static double minRainDisplay = .1; //Minimum threshold for displaying rain prob
    private static TextView temp, max, min, icon,precipType, precipPercent, alarmIcon, alarmText;
    private static LinearLayout precipTile;
    private static Typeface weatherFont, iconFont, defaultFont;
    private static BluetoothWrapper bWrap;

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
        weather = new JSONGetter.Weather((LocationManager) getSystemService(LOCATION_SERVICE), this);
        duolingo = new Duolingo();



        // Set up bluetooth
        bWrap = new BluetoothWrapper(this, true);


        /*BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bAdapter == null)//If device has bluetooth
        {
            Toast.makeText(this,
                    getResources().getString(R.string.no_bluetooth),
                    Toast.LENGTH_LONG).show();
        }
        else //Else device has bluetooth
        {
            // Set name of device to app name
            bAdapter.setName(getResources().getString(R.string.bluetooth_name));

            // Make device always discoverable
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            enableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivity(enableIntent); //Automatically asks for permission
        }*/

        // Initialize fonts
        defaultFont = icon.getTypeface();
        weatherFont = Typeface.createFromAsset(getAssets(), "fonts/weather.ttf");
        iconFont = Typeface.createFromAsset(getAssets(), "fonts/heydings_icons.ttf");

        // Initialize constant fonts
        precipType.setTypeface(weatherFont);
        alarmIcon.setTypeface(iconFont);

        // Set up timer for recurring tasks
        TimerTask updater = getUpdater(new Handler());
        Timer timer = new Timer();
        timer.schedule(updater, 0, weatherDelay);
    }

    public TimerTask getUpdater(final Handler handler)
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
                        weather.getNewData();
                        duolingo.getStatus();
                    }
                });
            }
        };
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
        Log.d("MIRROR", "resultCode = " + resultCode);
        Log.d("MIRROR", "requestCode = " + requestCode);
        Log.d("MIRROR", "result ok = " + RESULT_OK);
        if (requestCode == 5)
        {
            Log.d("MIRROR", "Thanks for enable btooth, yo");
            bWrap.connectDevices();

        }

    }


}