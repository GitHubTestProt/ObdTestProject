package com.unipi.papadopoulos.obdtestproject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.UUID;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
//***************Author***************//
//*********Panagiotis Papadopoulos***//
// ********University of Piraeus****//

public class MainActivity extends Activity {
    // Debugging for LOGCAT
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;
    //kainourgia
    private final static int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    String deviceAddress1;
    public String obdAnswer;
    public ConnectThread mConnectThread;
    public BluetoothSocket mmSocket;

    // declare button for launching website and textview for connection status
    Button tlbutton;
    TextView textView1;

    // EXTRA string to send on to mainactivity
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    public static final UUID MY_UUID = UUID.fromString("00001115-0000-1000-8000-00805f9b34fb");
    //public static   UUID SERIAL_UUID = device.getUuids()[0].getUuid()
    protected static final int SUCCESS_CONNECT = 0;
    protected static final int MESSAGE_READ = 1;
    public String address;
    public ConnectedThread connectedThread ;

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            Log.i("tag", "in handler");
            super.handleMessage(msg);
            switch (msg.what) {
                case SUCCESS_CONNECT:
                    // DO something
                    if(connectedThread!=null)
                    {
                        connectedThread.cancel();
                    }
                    connectedThread = new ConnectedThread((BluetoothSocket) msg.obj);
                    Toast.makeText(getApplicationContext(), "CONNECT", 0).show();
                    String paramString = "010D1\r";
                    String tmp ="";
                    byte[] bytes = paramString.getBytes();
                    connectedThread.write(bytes);
                    connectedThread.run();
                    tmp=obdAnswer.substring(18, 20);
                    Log.e("tmp",tmp);
                    int speed=0;
                    speed = Integer.parseInt(tmp,16);
                    Log.e("speeed", String.valueOf(speed));
                    Toast.makeText(getApplicationContext(), String.valueOf(speed), 0).show();
                    Log.i("tag", "connected");
                    final TextView speedtxtview=(TextView)findViewById(R.id.speedtextview);
                    speedtxtview.setText(String.valueOf(speed));
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String string = new String(readBuf);
                    Toast.makeText(getApplicationContext(), string, 0).show();
                    break;
            }
        }
    };


    @Override
    public void onStop() {
        super.onStop();
    }

    private class ConnectThread extends Thread {

        //private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            String tag = "connectthread";
            BluetoothSocket tmp = null;
            mmDevice = device;
            Log.d("device",mmDevice.toString());
            Log.i(tag, "construct");
            UUID SERIAL_UUID = mmDevice.getUuids()[0].getUuid();
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                //tmp = device.createRfcommSocketToServiceRecord(SERIAL_UUID);
                //  tmp = device.createInsecureRfcommSocketToServiceRecord(SERIAL_UUID);
                mmSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                Log.d("uuid",SERIAL_UUID.toString());

            } catch (IOException e) {
                Log.i(tag, "get socket failed");

            }
            //mmSocket = tmp;
        }

        public void run() {


            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a successful connection or an exception
                Log.i(TAG,"Connecting to socket...");
                mmSocket.connect();
                mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                //  e.printStackTrace();

                try {
                    Log.i(TAG,"Trying fallback...");
                    mmSocket.close();
                    mmSocket =(BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(mmDevice,2);
                    mmSocket.connect();
                    mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket).sendToTarget();
                    Log.i("connected","Connected");
                } catch (Exception e2) {
                    Log.e(TAG, "Couldn't establish Bluetooth connection!");
                    try {
                        mmSocket.close();
                    } catch (IOException e3) {
                        //Log.e(TAG, "unable to close() " + mSocketType + " socket during connection failure", e3);
                    }
                    //connectionFailed();
                    return;
                }
                // return;
            }

            //
        }


        /** Will cancel an in-progress connection, and close the socket */

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            while (true)
            {
                try {
                    char c = (char) mmInStream.read();
                    Log.d("c", String.valueOf(c));
                    obdAnswer = obdAnswer + c;
                    if(c == '>')
                    {
                        // System.out.println("Read from the InputStream: " + obdAnswer);
                        Log.e("conectedthread","connectedthread");
                        //System.out.println("Message length: " + obdAnswer.length());
                        return ;
                    }
                }
                catch (IOException e) {
                    break;
                }
            }
        }


        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmInStream.close();
                mmOutStream.close();
                //mmSocket.close();
            } catch (IOException e) {
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        buttonOnClick();
    }

    private void buttonOnClick()
    {
        Button btnspeed =(Button)findViewById(R.id.speedbutton);
        final TextView speedtxtview=(TextView)findViewById(R.id.speedtextview);
        btnspeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnectThread !=null) {
                    mConnectThread.cancel();
                }
                BluetoothDevice deviceAddress = mBluetoothAdapter.getRemoteDevice(address);
                mConnectThread= new ConnectThread(deviceAddress);
                mConnectThread.start();
                mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket).sendToTarget();

            }
        });
    }

    @Override
    public void onResume()  {
        super.onResume();
        //***************
        checkBTState();

        textView1 = (TextView) findViewById(R.id.connecting);
        textView1.setTextSize(40);
        textView1.setText(" ");

        // Initialize array adapter for paired devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices and append to 'pairedDevices'
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // Add previosuly paired devices to the array
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);//make title viewable
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            //String noDevices = getResources().getText(R.string.none_paired).toString();
            // mPairedDevicesArrayAdapter.add(noDevices);
        }
    }

    // Set up on-click listener for the list (nicked this - unsure)
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            textView1.setText("Connecting...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            address = info.substring(info.length() - 17);
            Log.d("Mlk", address);
            BluetoothDevice deviceAddress = mBluetoothAdapter.getRemoteDevice(address);
            mConnectThread = new ConnectThread(deviceAddress);
            mConnectThread.start();



        }
    };

    private void checkBTState() {
        // Check device has Bluetooth and that it is turned on
        mBtAdapter = BluetoothAdapter.getDefaultAdapter(); // CHECK THIS OUT THAT IT WORKS!!!
        if (mBtAdapter == null) {
            Toast.makeText(getBaseContext(), "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            if (mBtAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);

            }
        }
    }
}