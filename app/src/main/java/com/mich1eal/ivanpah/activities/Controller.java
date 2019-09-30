package com.mich1eal.ivanpah.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.mich1eal.ivanpah.BWrapper;
import com.mich1eal.ivanpah.R;

import org.json.JSONObject;

import java.util.Calendar;

/**
 * Created by Michael on 8/30/2016.
 */
public class Controller extends Activity
{
    private static final String TAG = Controller.class.getSimpleName();

    private static TextView statusText, dayBrightLabel, nightBrightLabel;
    private static SeekBar dayBrightLevel, nightBrightLevel;
    private static Button retryButton, sendButton, cancelButton, duoButton, settingsButton;
    private static TimePicker timePick;
    private static CheckBox duoCheck, hueCheck;
    private static EditText hueText;
    private static WebView webView;
    private static LinearLayout alarmFrame, hueFrame, hueTimeFrame;

    private static SharedPreferences settings;
    public static String lastMin = "LAST_MINUTE";
    public static String lastHour = "LAST_HOUR";
    public static String dayBright = "DAY_BRIGHT";
    public static String nightBright = "NIGHT_BRIGHT";

    private static int dayBrightDefault = 100;
    private static int nightBrightDefault = 20;

    private static BWrapper bWrap;
    private boolean duoMode = false;
    private long lastTime;
    private long heartbeatDelay = 3 * 1000; //Amount of time between heartbeats, in millis

    private static String hueIP;
    private static boolean hueEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);
        statusText = (TextView) findViewById(R.id.control_status);
        dayBrightLabel = (TextView) findViewById(R.id.dayBrightLabel);
        nightBrightLabel = (TextView) findViewById(R.id.nightBrightLabel);
        retryButton = (Button) findViewById(R.id.control_retry);
        sendButton = (Button) findViewById(R.id.control_send);
        cancelButton = (Button) findViewById(R.id.control_cancel);
        settingsButton = (Button) findViewById(R.id.control_settings);
        duoButton = (Button) findViewById(R.id.control_duo);
        duoCheck = (CheckBox) findViewById(R.id.control_duo_check);
        hueCheck = (CheckBox) findViewById(R.id.control_hue_check);
        webView = (WebView) findViewById(R.id.control_web);
        alarmFrame = (LinearLayout) findViewById(R.id.control_frame_alarm);
        hueFrame = (LinearLayout) findViewById(R.id.control_hue_container);
        hueTimeFrame = (LinearLayout) findViewById(R.id.control_hue_time_frame);
        hueText = (EditText) findViewById(R.id.control_hue_time);

        settings = getSharedPreferences(getString(R.string.prefs), MODE_PRIVATE);

        //Initialize prefs for brightness
        if (!settings.contains(dayBright))
        {
            settings.edit().putInt(dayBright, dayBrightDefault).apply();
        }
        if (!settings.contains(nightBright))
        {
            settings.edit().putInt(nightBright, nightBrightDefault).apply();
        }

        //Initialize brightness sliders
        dayBrightLevel = (SeekBar) findViewById(R.id.dayBrightLevel);
        dayBrightLevel.setProgress(settings.getInt(dayBright, 20));
        dayBrightLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                settings.edit().putInt(dayBright, progress).apply();

            }
        });

        nightBrightLevel = (SeekBar) findViewById(R.id.nightBrightLevel);
        nightBrightLevel.setProgress(settings.getInt(nightBright, 20));
        nightBrightLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                settings.edit().putInt(nightBright, progress).apply();

            }
        });




        timePick = (TimePicker) findViewById(R.id.control_time_pick);
        if (settings.contains(lastHour) && settings.contains(lastMin))
        {
            timePick.setCurrentMinute(settings.getInt(lastMin, 1));
            timePick.setCurrentHour(settings.getInt(lastHour, 1));
        }

        hueEnabled = settings.getBoolean(Setup.enableHue, false);

        Typeface iconFont = Typeface.createFromAsset(getAssets(), "fonts/heydings_icons.ttf");
        settingsButton.setTypeface(iconFont);

        if (hueEnabled)
        {
            hueIP = settings.getString(Setup.hueIPStr, "NONE");
            hueFrame.setVisibility(View.VISIBLE);
            hueCheck.setText("Use Phillips Hue (IP: " + hueIP + ")");
        }

        hueCheck.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                {
                    hueTimeFrame.setVisibility(View.VISIBLE);
                    hueText.setText("30");
                }
                else
                {
                    hueTimeFrame.setVisibility(View.GONE);
                }
            }
        });

        retryButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                bWrap.connect();
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, timePick.getCurrentHour());
                cal.set(Calendar.MINUTE, timePick.getCurrentMinute());
                cal.set(Calendar.SECOND, 0);

                JSONObject json = new JSONObject();
                try
                {
                    json.put(BWrapper.alarmTime, cal.getTimeInMillis());
                    if (duoCheck.isChecked()) json.put(BWrapper.username, "mich1eal");
                    if (hueEnabled)
                    {
                        json.put(BWrapper.hueIP, hueIP);
                        json.put(BWrapper.hueTime, Integer.valueOf(hueText.getText().toString()));
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                settings.edit()
                        .putInt(lastMin, cal.get(Calendar.MINUTE))
                        .putInt(lastHour, cal.get(Calendar.HOUR_OF_DAY))
                        .apply();

                bWrap.write(json.toString());
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                bWrap.write(BWrapper.MESSAGE_CANCEL);
            }

        });

        duoButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //Toggle duo mode
                duoMode = !duoMode;
                if (duoMode)
                {
                    alarmFrame.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                    duoButton.setText(R.string.control_cancel_duo);
                }
                else
                {
                    alarmFrame.setVisibility(View.VISIBLE);
                    webView.setVisibility(View.GONE);
                    duoButton.setText(R.string.control_launch_duo);
                }
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                launchSetup();
            }
        });

        //Handler is static to prevent memory leaks. See:
        // http://stackoverflow.com/questions/11278875/handlers-and-memory-leaks-in-android
        bWrap = new BWrapper(this, new BHandler(), false);

        webView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String   failingUrl)
            {

            }
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0");
        webView.loadUrl("https://www.duolingo.com");
    }

    static class BHandler extends Handler
    {
        @Override
        public void handleMessage(Message inputMessage)
        {
            Log.d(TAG, "Message recieved: " + inputMessage.what);
            int msg = R.string.status_error;
            boolean connected = false;
            switch (inputMessage.what)
            {
                case BWrapper.STATE_SEARCHING:
                    msg = R.string.status_searching;
                    retryButton.setEnabled(false);
                    break;
                case BWrapper.STATE_CONNECTED:
                    msg = R.string.status_connect;
                    retryButton.setEnabled(false);
                    connected = true;
                    break;
                case BWrapper.STATE_NO_BLUETOOTH:
                    msg = R.string.status_no_bluetooth;
                    retryButton.setEnabled(true);
                    break;
                case BWrapper.STATE_DISCONNECTED:
                    msg = R.string.status_disconnect;
                    retryButton.setEnabled(true);
                    break;
                case BWrapper.STATE_FOUND:
                    msg = R.string.status_found;
                    retryButton.setEnabled(false);
                    break;
                default:
                    msg = R.string.status_error;
                    retryButton.setEnabled(true);
            }

            if (connected)
            {
                dayBrightLabel.setEnabled(true);
                nightBrightLabel.setEnabled(true);
                dayBrightLevel.setEnabled(true);
                nightBrightLevel.setEnabled(true);
            }
            else
            {
                dayBrightLabel.setEnabled(false);
                nightBrightLabel.setEnabled(false);
                dayBrightLevel.setEnabled(false);
                nightBrightLevel.setEnabled(false);
            }

            statusText.setText(msg);
        }
    }

    @Override
    public void onPause()
    {
        bWrap.close();
        super.onPause();
    }

    @Override
    public void onUserInteraction()
    {
        final long now = System.currentTimeMillis();
        if (duoMode && now - lastTime > heartbeatDelay)
        {
            bWrap.write(BWrapper.MESSAGE_HEARTBEAT);
            lastTime = now;
        }
    }

    private void launchSetup()
    {
        Intent i = new Intent(this, Setup.class);
        startActivity(i);
    }
}