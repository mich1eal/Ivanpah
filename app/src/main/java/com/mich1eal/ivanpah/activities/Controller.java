package com.mich1eal.ivanpah.activities;

import android.app.Activity;
import android.os.Bundle;

import com.mich1eal.ivanpah.BluetoothWrapper;
import com.mich1eal.ivanpah.R;

/**
 * Created by Michael on 8/30/2016.
 */
public class Controller extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mirror);

        BluetoothWrapper bWrap = new BluetoothWrapper(this, false);
        bWrap.connectDevices();

        //Set up bluetooth






    }
}
