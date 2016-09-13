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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Michael on 9/4/2016.
 */
public class bWrapper
{
    private static final String TAG = bWrapper.class.getSimpleName();
    private static final String SERVER_NAME = "Ivanpah Smart Mirror";
    private static final UUID uuid = UUID.fromString("d3cb33f4-094f-4b55-b23e-e5d771ab2f92");


    public static final int MESSAGE_READ = 99;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_SEARCHING = 3;
    public static final int STATE_NOT_FOUND = 4;
    public static final int STATE_NO_BLUETOOTH = 5;
    public static final int STATE_FOUND = 6;

    public static final int STATE_ERROR = -1;

    private int state;
    private Context context;
    private BluetoothAdapter bAdapter;
    private boolean serverFound = false;
    private Handler handler;

    private ConnectedThread connectedThread;


    public bWrapper(Context context, Handler handler)
    {
        assert(context != null);

        this.context = context;
        this.handler = handler;

        bAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void setState(int state)
    {
        //Update state and notify handler
        this.state = state;
        if (handler != null)
        {
            handler.sendEmptyMessage(state);
        }
    }

    private boolean hasBluetooth()
    {
        if (bAdapter == null || !bAdapter.isEnabled())//If device doesn't have bluetooth
        {
            setState(STATE_NO_BLUETOOTH);
            return false;
        }
        return true;
    }

    public void startServer()
    {
        //If no bluetooth, exit.
        if (!hasBluetooth()) return;

        bAdapter.setName(SERVER_NAME);
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        enableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
        ((Activity) context).startActivityForResult(enableIntent, 5);
    }

    //This method needs to be called by the host activity after getting the activity result back
    // from startSever(). This is a terrible implementation and should be fixed
    public void finishServer()
    {
        new ServerThread().start();
    }

    public void startClient()
    {
        //If no bluetooth, exit.
        if (!hasBluetooth()) return;

        if(state == STATE_CONNECTED) return;
        setState(STATE_SEARCHING);

        // First check for already established pairings
        ArrayList<BluetoothDevice> pairedDevices = new ArrayList<BluetoothDevice>();
        pairedDevices.addAll(bAdapter.getBondedDevices());

        // Look for prepaired servers
        if (pairedDevices.size() > 0)
        {
            BluetoothDevice device = pairedDevices.get(0);
            Log.d(TAG, "Found a pre-paired device: name = " + device.getName() + " at " + device.getAddress());
            if (device.getName().equals(SERVER_NAME))
            {
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
                    Log.d(TAG, "Device found: " + device.toString());
                    if (device.getName().equals(SERVER_NAME))
                    {
                        new ClientThread(device).start();
                    }
                }
                else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                        && state != STATE_FOUND
                        && state != STATE_CONNECTED)
                {
                    setState(STATE_NOT_FOUND);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
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
            BluetoothSocket socket;
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
                    setState(STATE_CONNECTED);
                    connectedThread = new ConnectedThread(socket);
                    connectedThread.start();
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
        private final BluetoothSocket socket;
        private final BluetoothDevice client;

        public ClientThread(BluetoothDevice client)
        {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            setState(STATE_FOUND);
            BluetoothSocket tmp = null;
            this.client = client;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try
            {
                tmp = client.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            socket = tmp;
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
                setState(STATE_CONNECTED);
                Log.d(TAG, "Socket starting");
                socket.connect();
                Log.d(TAG, "Socket connected?");
                connectedThread = new ConnectedThread(socket);
                Log.d(TAG, "Socket is open!");
                connectedThread.start();
            }
            catch (IOException connectException)
            {
                // Unable to connect; close the socket and get out
                try
                {
                    socket.close();
                }
                catch (IOException closeException)
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
                socket.close();
            }
            catch (IOException e)
            {
            }
        }
    }

    public void write(String str)
    {
        if (state == STATE_CONNECTED && connectedThread != null)
        {
            Log.d(TAG, "Writing string: str");
            connectedThread.write(str.getBytes());
        }
    }

    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket socket;
        private final InputStream inStream;
        private final OutputStream outStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e)
            {
            }

            inStream = tmpIn;
            outStream = tmpOut;
        }

        public void run()
        {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true)
            {
                try
                {
                    // Read from the InputStream
                    bytes = inStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e)
                {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes)
        {
            try
            {
                outStream.write(bytes);
            } catch (IOException e)
            {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel()
        {
            try
            {
                socket.close();
            } catch (IOException e)
            {
            }
        }
    }
}
