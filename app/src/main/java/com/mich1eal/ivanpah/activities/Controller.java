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
    private static Button retryButton, sendButton;
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

        retryButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                bWrap.startClient();
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
                Log.d(TAG, "Trying to send the string: " + msg);
                bWrap.write(msg);
            }
        });

        //Handler is static to prevent memory leaks. See:
        // http://stackoverflow.com/questions/11278875/handlers-and-memory-leaks-in-android
        bWrap = new bWrapper(this, new BHandler());
    }

    static class BHandler extends Handler
    {
        @Override
        public void handleMessage(Message inputMessage)
        {
            Log.d(TAG, "Message recieved: " + inputMessage.what);
            switch (inputMessage.what)
            {
                case bWrapper.STATE_SEARCHING:
                    statusText.setText(R.string.status_searching);
                    retryButton.setEnabled(false);
                    break;
                case bWrapper.STATE_CONNECTED:
                    statusText.setText(R.string.status_connect);
                    retryButton.setEnabled(false);
                    break;
                case bWrapper.STATE_NO_BLUETOOTH:
                    statusText.setText(R.string.status_no_bluetooth);
                    retryButton.setEnabled(true);
                    break;
                case bWrapper.STATE_NOT_FOUND:
                    statusText.setText(R.string.status_not_found);
                    retryButton.setEnabled(true);
                    break;
                case bWrapper.STATE_FOUND:
                    statusText.setText(R.string.status_found);
                    retryButton.setEnabled(false);
                    break;
                default:
                    statusText.setText(R.string.status_error);
                    retryButton.setEnabled(true);
            }
        }


    }

}
