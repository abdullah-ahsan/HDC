package com.example.abdullah.hdc_0;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.media.Image;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import cz.msebera.android.httpclient.Header;

public class DataActivity extends AppCompatActivity {

    private String TAG = "DataActivity", deviceName;
    private Bluetooth bluetooth;
    private TextView status, tv_temp, tv_humidity;
    private ImageButton btRefreshButton, cloudUploadButton, historyButton;
    private boolean btConnected = false, firstDataGot = false;
    private static int REQUEST_ENABLE_BT = 111;
    private AsyncHttpClient httpClient;

    private float currentHumidity = -1, currentTemp = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);


        //initialize the httpCient
        httpClient = new AsyncHttpClient();

        //getting the intent
        Intent intent = getIntent();
        //connect to the bluetooth device
        deviceName = intent.getStringExtra(getString(R.string.device_name_tag));

        Log.i(TAG, "Device Name: (" + deviceName + "), length: " + deviceName.length());

        //initiate the different views
        status = (TextView) findViewById(R.id.tv_status);
        status.setText("Connecting to BT device...");
        tv_humidity = (TextView) findViewById(R.id.tv_humidity);
        tv_humidity.setText("N/A");
        tv_temp = (TextView) findViewById(R.id.tv_temp);
        tv_temp.setText("N/A");

        btRefreshButton = (ImageButton)findViewById(R.id.btRefreshFab);
        btRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getBtData();
            }
        });


        cloudUploadButton = (ImageButton)findViewById(R.id.cloudUploadFab);
        cloudUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //for testing, generate random data
                //uncomment top lines in postData() when removing these lines
                //currentHumidity = (float)Math.round(((new Random()).nextFloat()*10 +20)*100f)/100f;
                //currentTemp = (float)Math.round(((new Random()).nextFloat()*10+8)*100f)/100f;
                //tv_humidity.setText(""+currentHumidity); tv_temp.setText(""+currentTemp);
                ///////////////////////////////////////////////////////////////////////////

                Date date = new Date();
                postCloudData(currentHumidity, currentTemp, date.getTime()/1000);
            }
        });

        historyButton = (ImageButton) findViewById(R.id.historyFab);
        historyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(DataActivity.this, GraphActivity.class);
                //give this name back to this activity when coming back
                intent1.putExtra(getString(R.string.device_name_tag), deviceName);
                startActivity(intent1);
            }
        });

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled())
        {
            //enable the bluetooth
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, REQUEST_ENABLE_BT);
            status.setText("Enabling Bluetooth...");
        }
        else
        {
            initBluetooth();
            connectService(deviceName);
        }
    }

    public void connectService(String deviceName){
        if(bluetooth != null)
        {
            try {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter.isEnabled()) {
                    bluetooth.start();
                    bluetooth.connectDevice(deviceName);
                    Log.d(TAG, "connectService: Btservice started - listening");
                } else {
                    Log.d(TAG, "connectService: Btservice started - bluetooth is not enabled");
                }
            } catch(Exception e){
                Log.e(TAG, "Unable to start bt ", e);
                Toast.makeText(DataActivity.this, "connectService: Unable to start bt service with " + deviceName, Toast.LENGTH_SHORT).show();
            }
        }
        else
            status.setText(getString(R.string.bluetooth_disabled_error));

    }

    //bluetooth message handler
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Bluetooth.MESSAGE_STATE_CHANGE:
                    Log.d(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    break;
                case Bluetooth.MESSAGE_WRITE:
                {
                    Log.d(TAG, "MESSAGE_WRITE ");
                    //Toast.makeText(DataActivity.this, "Message Write", Toast.LENGTH_SHORT).show();
                    break;
                }
                case Bluetooth.MESSAGE_READ:
                {
                    Log.d(TAG, "MESSAGE_READ ");
                    //Toast.makeText(DataActivity.this, "Message Read", Toast.LENGTH_SHORT).show();

                    gotBtData(msg.obj, msg.arg1);
                    break;
                }

                case Bluetooth.MESSAGE_DEVICE_NAME:
                    Log.d(TAG, "MESSAGE_DEVICE_NAME "+msg);
                    status.setText("Connected to: " + deviceName);
                    btConnected = true;
                    break;
                case Bluetooth.MESSAGE_TOAST:
                    Log.d(TAG, "MESSAGE_TOAST "+msg);
                    break;
            }
        }
    };

    private void getBtData()
    {
        if(bluetooth != null)
        {
            if(btConnected)
            {
                //send a data query to the bluetooth device
                String btQuery = getResources().getString(R.string.btQuery);
                bluetooth.sendMessage(btQuery);
                status.setText("BT query sent.");
            }
            else
                status.setText("BT device not connected yet.");
        }
        else
            status.setText(getString(R.string.bluetooth_disabled_error));

    }

    private void gotBtData(Object wholeByteArray, int noOfBytes)
    {
        boolean firstDataGot_temp = false, firstDataGot_humidity = false;
        //Toast.makeText(this, "Data received from Bluetooth", Toast.LENGTH_SHORT).show();
        status.setText("Received data from BT device.");
        byte[] byteArray = Arrays.copyOfRange((byte[]) wholeByteArray, 0, noOfBytes);
        String str = new String(byteArray);
        Log.d(TAG, "Whole String received: " + str);

        //the string coming from the bluetooth device is:
        //!(tempFloat)!  -250ms Delay-    @{humidityFloat}@

        //if this condition is true, the temperature data is intact
        if(str.indexOf('(')!=-1 && str.indexOf(')')!=-1)
        {
            String tempString = str.substring(str.indexOf('(')+1 ,str.indexOf(')'));
            try{
                    currentTemp = Float.parseFloat(tempString);
                    tv_temp.setText("" + currentTemp);
                    Log.d(TAG, "Got the temperature: " + currentTemp);
                    firstDataGot_temp = true;
            }
            catch (Exception e)
            {
                    Log.e(TAG, "Couldnt Parse float from temperature string: " + tempString);
            }
        }

        //if this condition is true, the humidity data is intact
        if(str.indexOf('{')!=-1 && str.indexOf('}')!=-1)
        {
            String humidString = str.substring(str.indexOf('{')+1 ,str.indexOf('}'));
            try{
                currentHumidity = Float.parseFloat(humidString);
                tv_humidity.setText("" + currentHumidity);
                Log.d(TAG, "Got the humidity: " + currentHumidity);
                firstDataGot_humidity = true;
            }
            catch (Exception e)
            {
                Log.e(TAG, "Couldnt Parse float from humidity string: " + humidString);
            }
        }

        if(firstDataGot_temp && firstDataGot_humidity)
            firstDataGot = true;
    }

    private void postCloudData(float humidity, float temperature, long timeStamp)
    {
        if(!firstDataGot)   //no data to post
        {
            Toast.makeText(this, "No data to Post", Toast.LENGTH_SHORT).show();
            status.setText("No data to post.");
            return;
        }

        status.setText("Sending data to cloud...");
        String postURL = getResources().getString(R.string.postURL);
        String postURLwithParams = postURL + "&humidity=" + humidity + "&temp=" + temperature + "&time=" + timeStamp;
        //Log.i(TAG, postURLwithParams);
        httpClient.post(postURLwithParams, null, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                status.setText("Data uploaded.");
                Toast.makeText(DataActivity.this, "Data posted to Cloud", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                status.setText("Data upload failure.");
                Toast.makeText(DataActivity.this, "Data post failure", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(bluetooth != null)
            bluetooth.stop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //if this result is for the enable bluetooth request
        if(requestCode == REQUEST_ENABLE_BT)
        {
            if(resultCode == RESULT_OK)
            {
                initBluetooth();
                connectService(deviceName);
            }
            else
            {
                status.setText("Bluetooth disabled.");
                bluetooth = null;
            }

        }
    }

    private void initBluetooth()
    {
        Log.i(TAG, "Enabling bluetooth.");
        bluetooth = new Bluetooth(this, mHandler);
        status.setText("Bluetooth enabled.");
    }
}
