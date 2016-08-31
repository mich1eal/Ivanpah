package com.mich1eal.ivanpah;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Michael on 8/30/2016.
 */
public class Setup extends Activity
{
    private static RadioGroup radios;
    private static SharedPreferences settings;

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

        if (!isLaunch) welcome.setVisibility(View.GONE); //if not launch, hide the welcome message

        go.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //One radio button is pre checked
                boolean isMirror = radios.getCheckedRadioButtonId() == 1; //first option is mirror
                settings.edit().putBoolean(getString(R.string.config), isMirror).commit();
                launchMirror();
            }
        });
    }

    private void launchMirror()
    {
        // Launch the correct activity based on saved settings
        boolean mirror = settings.getBoolean(getString(R.string.config), true);
        startActivity(new Intent(this, mirror ? Mirror.class : Controller.class));
    }
}
