package com.mich1eal.ivanpah.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.IdRes;
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
    private static CheckBox hue, duo;
    private static EditText hueIP, duoUsername;

    public static String enableHue = "ENABLE_HUE";
    public static String enableDuo = "ENABLE_DUO";
    public static String hueIPStr = "HUE_IP_STRING";
    public static String duoUNStr = "DUOLINGO_USERNAME";

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
        TextView welcome = (TextView) findViewById(R.id.setup_welcome);

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


        duo = (CheckBox) findViewById(R.id.setup_duo);
        duoUsername = (EditText) findViewById(R.id.setup_duo_user);

        duo.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                duoUsername.setVisibility(View.VISIBLE);
                if(isChecked)
                {
                    duoUsername.setVisibility(View.VISIBLE);
                }
                else
                {
                    duoUsername.setText("");
                    duoUsername.setVisibility(View.GONE);
                }
            }
        });

        radios.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {

                if (checkedId == R.id.setup_radio_mirror)
                {
                    hue.setVisibility(View.GONE);
                    hueIP.setVisibility(View.GONE);
                    duo.setVisibility(View.GONE);
                    duoUsername.setVisibility(View.GONE);
                }
                else if (checkedId == R.id.setup_radio_controller)
                {
                    hue.setVisibility(View.VISIBLE);
                    duo.setVisibility(View.VISIBLE);
                    if (hue.isChecked()) hueIP.setVisibility(View.VISIBLE);
                    if (duo.isChecked()) duoUsername.setVisibility(View.VISIBLE);
                }
            }
        });


        if (!isLaunch) welcome.setVisibility(View.GONE); //if not launch, hide the welcome message

        // set presets if available
        //checked if hue enabled, unchecked by default
        hue.setChecked(settings.getBoolean(enableHue, false));
        duo.setChecked(settings.getBoolean(enableDuo, false));

        //set hue IP unless its null
        hueIP.setText(settings.getString(hueIPStr, ""));
        duoUsername.setText(settings.getString(duoUNStr, ""));

        // set radios to mirror by default
        boolean isMirror = settings.getBoolean(getString(R.string.config), true);
        if (isMirror) radios.check(R.id.setup_radio_mirror);
        else radios.check(R.id.setup_radio_controller);

        go.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //One radio button is pre checked
                int id = radios.getCheckedRadioButtonId();
                boolean isMirror = id == R.id.setup_radio_mirror; //first option is mirror
                settings.edit().putBoolean(getString(R.string.config), isMirror)
                        .putBoolean(enableHue, hue.isChecked())
                        .putBoolean(enableDuo, duo.isChecked())
                        .apply();
                if (hue.isChecked()) settings.edit().putString(hueIPStr, hueIP.getText().toString()).apply();
                if (duo.isChecked()) settings.edit().putString(duoUNStr, duoUsername.getText().toString()).apply();
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
        finish();
    }
}
