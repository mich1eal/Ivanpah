package com.mich1eal.ivanpah.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.TimePicker;

import com.mich1eal.ivanpah.BWrapper;
import com.mich1eal.ivanpah.R;

import java.util.Calendar;

/**
 * Created by Michael on 8/30/2016.
 */
public class Controller extends Activity
{
    private static final String TAG = Controller.class.getSimpleName();

    private static TextView statusText;
    private static Button retryButton, sendButton, cancelButton, duoButton;
    private static TimePicker timePick;
    private static CheckBox duoCheck;
    private static WebView webView;

    private static BWrapper bWrap;
    private boolean duoMode = false;
    private long lastTime;
    private long heartbeatDelay = 3 * 1000; //Amount of time between heartbeats, in millis


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);
        statusText = (TextView) findViewById(R.id.control_status);
        retryButton = (Button) findViewById(R.id.control_retry);
        sendButton = (Button) findViewById(R.id.control_send);
        timePick = (TimePicker) findViewById(R.id.control_time_pick);
        cancelButton = (Button) findViewById(R.id.control_cancel);
        duoButton = (Button) findViewById(R.id.control_duo);
        duoCheck = (CheckBox) findViewById(R.id.control_duo_check);
        webView = (WebView) findViewById(R.id.control_web);

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

                String msg = String.valueOf(cal.getTimeInMillis());
                if (duoCheck.isChecked()) msg = msg + BWrapper.MESSAGE_DELIM + "mich1eal";

                bWrap.write(msg);
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
                duoButton.setText(duoMode ? "Cancel Duolingo" : "Launch Duolingo");
            }
        });



        //Handler is static to prevent memory leaks. See:
        // http://stackoverflow.com/questions/11278875/handlers-and-memory-leaks-in-android
        bWrap = new BWrapper(this, new BHandler(), false);

        //webView.loadUrl("https://www.duolingo.com");
    }

    static class BHandler extends Handler
    {
        @Override
        public void handleMessage(Message inputMessage)
        {
            Log.d(TAG, "Message recieved: " + inputMessage.what);
            int msg = R.string.status_error;
            switch (inputMessage.what)
            {
                case BWrapper.STATE_SEARCHING:
                    msg = R.string.status_searching;
                    retryButton.setEnabled(false);
                    break;
                case BWrapper.STATE_CONNECTED:
                    msg = R.string.status_connect;
                    retryButton.setEnabled(false);
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
            statusText.setText(msg);
        }
    }

    @Override
    public void onDestroy()
    {
        bWrap.close();
        super.onDestroy();
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

}
