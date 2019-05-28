package com.mich1eal.ivanpah;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

/**
 * Created by Michael on 2/27/2019.
 */

public class AlarmService extends Service implements MediaPlayer.OnPreparedListener
{
    private static final String TAG = AlarmService.class.getSimpleName();

    public static final String ACTION_RING = "com.mich1eal.ivanpah.action.RING";

    private MediaPlayer mediaPlayer;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId)
    {

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        //Inform UI thread of ring
        Intent uiIntent = new Intent(ACTION_RING);
        sendBroadcast(uiIntent);

        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.reset();
            AssetFileDescriptor afd = getAssets().openFd("river.mp3");
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaPlayer.setLooping(true);
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Mediaplayer has to be prepared before it can start (took 2 ms when I timed it)
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.prepareAsync();
    }

    @Override
    public void onDestroy()
    {
        if (mediaPlayer != null)
        {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        Log.d(TAG, "onDestroy()");

        super.onDestroy();
    }

    /** Called when MediaPlayer is ready */
    public void onPrepared(MediaPlayer player)
    {
        player.start();
    }
}