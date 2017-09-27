package com.mich1eal.ivanpah.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.mich1eal.ivanpah.R;

/**
 * Created by Michael on 8/30/2016.
 */
public class Setup extends Activity
{
    private static RadioGroup radios;
    private static SharedPreferences settings;
    private static CheckBox duoCheck, gps, hue;
    private static EditText hueIP;

    public static String hasDuo = "HAS_DUO";
    public static String alwaysPittsburgh = "ALWAYAS_PITTSBURGH";
    public static String enableHue = "ENABLE_HUE";
    public static String hueIPStr = "HUE_IP_STRING";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        settings = getSharedPreferences(getString(R.string.prefs), MODE_PRIVATE);




        boolean isLaunch = getIntent().getAction() == Intent.ACTION_MAIN;
        boolean isSetUp = settings.contains(getString(R.string.config));
        if (isLaunch && isSetUp)
        {
            // New launch and app is already configured - go straight to function
            launchMirror();
        }

        // Else need to load this activity
        setContentView(R.layout.activity_setup);
        Button go = (Button) findViewById(R.id.setup_go);
        radios = (RadioGroup) findViewById(R.id.setup_radio);
        radios.check(radios.getChildAt(0).getId()); //Set first option selected by default
        TextView welcome = (TextView) findViewById(R.id.setup_welcome);

        duoCheck = (CheckBox) findViewById(R.id.setup_duo);
        gps = (CheckBox) findViewById(R.id.setup_gps);
        hue = (CheckBox) findViewById(R.id.setup_hue);
        hueIP = (EditText) findViewById(R.id.setup_hue_ip);

        hue.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
               @Override
               public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                   hueIP.setVisibility(View.VISIBLE);
                   if(isChecked)
                   {
                       hueIP.setVisibility(View.VISIBLE);
                   }
                   else
                   {
                       hueIP.setText("");
                       hueIP.setVisibility(View.GONE);
                   }
               }
           });


        if (!isLaunch) welcome.setVisibility(View.GONE); //if not launch, hide the welcome message

        go.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //One radio button is pre checked
                int id = radios.getCheckedRadioButtonId();
                boolean isMirror = id == 1; //first option is mirror
                settings.edit().putBoolean(getString(R.string.config), isMirror).apply();
                settings.edit().putBoolean(enableHue, hue.isChecked()).apply();
                settings.edit().putString(hueIPStr, hueIP.getText().toString()).apply();
                settings.edit().putBoolean(hasDuo, duoCheck.isChecked()).apply();
                settings.edit().putBoolean(alwaysPittsburgh, gps.isChecked()).apply();


                launchMirror();
            }
        });
    }

    private void launchMirror()
    {
        // Launch the correct activity based on saved settings
        boolean mirror = settings.getBoolean(getString(R.string.config), true);

        Intent i = new Intent(this, mirror ? Mirror.class : Controller.class);
        startActivity(i);
    }
}
