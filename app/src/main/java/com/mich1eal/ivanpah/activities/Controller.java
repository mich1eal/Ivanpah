package com.mich1eal.ivanpah.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mich1eal.ivanpah.bWrapper;
import com.mich1eal.ivanpah.R;

/**
 * Created by Michael on 8/30/2016.
 */
public class Controller extends Activity
{
    private static TextView statusText;
    private static Button retryButton;
    private static bWrapper bWrap;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);
        statusText = (TextView) findViewById(R.id.control_status);
        retryButton = (Button) findViewById(R.id.control_retry);

        bWrap = new bWrapper(this, false, new bWrapper.bStatusListener()
        {
            @Override
            public void onStatusChange(int status)
            {
                switch (status)
                {
                    case bWrapper.STATUS_DISCONNECT:
                        statusText.setText(R.string.status_disconnect);
                        retryButton.setEnabled(true);
                        break;
                    case bWrapper.STATUS_SEARCHING:
                        statusText.setText(R.string.status_searching);
                        retryButton.setEnabled(false);
                        break;
                    case bWrapper.STATUS_CONNECTED:
                        statusText.setText(R.string.status_connect);
                        retryButton.setEnabled(false);
                        break;
                    case bWrapper.STATUS_NO_BLUETOOTH:
                        statusText.setText(R.string.status_no_bluetooth);
                        retryButton.setEnabled(true);
                        break;
                    case bWrapper.STATUS_NOT_FOUND:
                        statusText.setText(R.string.status_not_found);
                        retryButton.setEnabled(true);
                        break;
                    default:
                        statusText.setText(R.string.status_error);
                        retryButton.setEnabled(true);
                }
            }

        });

        retryButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                bWrap.connectDevices();
            }
        });

        bWrap.connectDevices();
        //Set up bluetooth
    }
}
