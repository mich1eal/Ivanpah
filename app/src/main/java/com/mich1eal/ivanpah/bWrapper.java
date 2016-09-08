package com.mich1eal.ivanpah;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Michael on 9/4/2016.
 */
public class bWrapper
{
    private static final String TAG = bWrapper.class.getSimpleName();
    private static final String SERVER_NAME = "Ivanpah Smart Mirror";
    private static final String UUID = "d3cb33f4-094f-4b55-b23e-e5d771ab2f92";

    public static final int STATUS_DISCONNECT = 0;
    public static final int STATUS_PAIRED = 1;
    public static final int STATUS_CONNECTED = 2;
    public static final int STATUS_SEARCHING = 3;
    public static final int STATUS_NOT_FOUND = 4;
    public static final int STATUS_NO_BLUETOOTH = 5;
    public static final int STATUS_CONNECTING = 6;
    public static final int STATUS_ERROR = -1;


    private boolean isServer;

    private Context context;
    private bStatusListener listener = null;
    private BluetoothAdapter bAdapter;
    private boolean mirrorFound = false;
    private bWrapper(){}; // Must properly intitialize


    public bWrapper(Context context, boolean isServer, bStatusListener listener)
    {
        this.isServer = isServer;
        this.context = context;
        this.listener = listener;

        bAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.d(TAG, "Bluetooth enabled? : " + bAdapter.isEnabled());
        if (bAdapter == null || !bAdapter.isEnabled())//If device has bluetooth
        {
            if (listener != null) listener.onStatusChange(STATUS_NO_BLUETOOTH);
        }

        else //Else device has bluetooth
        {
            if (listener != null) listener.onStatusChange(STATUS_DISCONNECT);
            // Set name and set to always be discoverable (if mirror)
            if (isServer)
            {
                bAdapter.setName(SERVER_NAME);
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                enableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
                ((Activity) context).startActivityForResult(enableIntent, 5);

            }
        }
    }

    public void connectDevices()
    {
        mirrorFound = false;

        if (bAdapter == null || !bAdapter.isEnabled())//If device has bluetooth
        {
            if (listener != null) listener.onStatusChange(STATUS_NO_BLUETOOTH);
            return;
        }

        if (listener != null) listener.onStatusChange(STATUS_SEARCHING);

        if (isServer)
        {
            new ServerThread().start();
            return;
        }

        // First check for already established pairings
        ArrayList<BluetoothDevice> pairedDevices = new ArrayList<BluetoothDevice>();
        pairedDevices.addAll(bAdapter.getBondedDevices());

        // First go through paired devices
        if (pairedDevices.size() > 0)
        {
            BluetoothDevice device = pairedDevices.get(0);
            Log.d(TAG, "Found a pre-paired device: name = " + device.getName() + " at " + device.getAddress());
            if (device.getName().equals(SERVER_NAME))
            {
                if (listener != null) listener.onStatusChange(STATUS_CONNECTED);
                new ClientThread(device).start();
                return;
            }
        }

        // Otherwise check for new devices
        Log.d(TAG, "No server device paried, searching now");
        // Set up broadcast adapater
        final BroadcastReceiver receiver = new BroadcastReceiver()
        {
            public void onReceive(Context context, Intent intent)
            {
                String action = intent.getAction();
                if (action.equals(BluetoothDevice.ACTION_FOUND)) //device found
                {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d(TAG, "Device found, name = " + device.getName());
                    if (device.getName().equals(SERVER_NAME));
                    {
                        if (listener != null) listener.onStatusChange(STATUS_CONNECTED);
                        mirrorFound = true;
                        new ClientThread(device).start();
                    }
                }
                else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED) && !mirrorFound)
                {
                    if (listener != null) listener.onStatusChange(STATUS_NOT_FOUND);
                }
                else if(action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED))
                {
                    if (listener != null) listener.onStatusChange(STATUS_DISCONNECT);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        context.registerReceiver(receiver, filter);


        bAdapter.startDiscovery();

        //Log.d(TAG, "No servers found");
    }

    public class ServerThread
            extends Thread
    {
        private final BluetoothServerSocket serverSocket;

        public ServerThread()
        {
            // Use a temporary object that is later assigned to serverSocket,
            // because serverSocket is final
            BluetoothServerSocket tmp = null;
            try
            {
                // MY_UUID is the app's UUID string, also used by the client code
                UUID uuid = java.util.UUID.randomUUID();
                tmp = bAdapter.listenUsingRfcommWithServiceRecord(SERVER_NAME, uuid);
            }
            catch (IOException e)
            {
                Log.e(TAG, e.getMessage());
            }
            serverSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Server Thread is running!");
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true)
            {
                try
                {
                    socket = serverSocket.accept();
                }
                catch (IOException e)
                {
                    Log.e(TAG, e.getMessage());
                    break;
                }
                // If a connection was accepted
                if (socket != null)
                {
                    // Do work to manage the connection (in a separate thread)
                    //manageConnectedSocket(socket);
                    Log.d(TAG, "Socket is open!");
                    //serverSocket.close();
                    break;
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ClientThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ClientThread(BluetoothDevice device)
        {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try
            {
                UUID uuid = java.util.UUID.randomUUID();
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        public void run()
        {
            Log.d(TAG, "Client thread running!");
            // Cancel discovery because it will slow down the connection
            bAdapter.cancelDiscovery();

            try
            {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
                Log.d(TAG, "Socket is open!");
            }
            catch (IOException connectException)
            {
                // Unable to connect; close the socket and get out
                try
                {
                    mmSocket.close();
                } catch (IOException closeException)
                {
                }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            // manageConnectedSocket(mmSocket);
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel()
        {
            try
            {
                mmSocket.close();
            }
            catch (IOException e)
            {
            }
        }
    }

    //Interface to be implemented by mirror to notify when a new message has been recieved
    public interface bStatusListener
    {

        //If a string is returned, it will be sent back to client
        public void onStatusChange(int status);
    }

}
