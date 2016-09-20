package com.mich1eal.ivanpah.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import com.mich1eal.ivanpah.bWrapper;
import com.mich1eal.ivanpah.R;

import java.lang.ref.WeakReference;

/**
 * Created by Michael on 8/30/2016.
 */
public class Controller extends Activity
{
    private static final String TAG = Controller.class.getSimpleName();

    private static TextView statusText;
    private static Button retryButton, sendButton, cancelButton;
    private static TimePicker timePick;
    private static bWrapper bWrap;


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
                String msg = new StringBuilder()
                        .append(timePick.getCurrentHour())
                        .append(':')
                        .append(timePick.getCurrentMinute())
                        .toString();
                bWrap.write(msg);
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                bWrap.write(bWrapper.MESSAGE_CANCEL);
            }

        });

        //Handler is static to prevent memory leaks. See:
        // http://stackoverflow.com/questions/11278875/handlers-and-memory-leaks-in-android
        bWrap = new bWrapper(this, new BHandler(), false);
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
                case bWrapper.STATE_SEARCHING:
                    msg = R.string.status_searching;
                    retryButton.setEnabled(false);
                    break;
                case bWrapper.STATE_CONNECTED:
                    msg = R.string.status_connect;
                    retryButton.setEnabled(false);
                    break;
                case bWrapper.STATE_NO_BLUETOOTH:
                    msg = R.string.status_no_bluetooth;
                    retryButton.setEnabled(true);
                    break;
                case bWrapper.STATE_DISCONNECTED:
                    msg = R.string.status_disconnect;
                    retryButton.setEnabled(true);
                    break;
                case bWrapper.STATE_FOUND:
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

}
