package com.example.abdullah.hdc_0;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ConnectActivity extends AppCompatActivity {

    private String tag = "ConnectActivity";
    private int REQUEST_ENABLE_BT = 111;
    private BluetoothAdapter bluetoothAdapter;
    private List<String> deviceNames;
    private ListView deviceListListView;
    private ArrayAdapter<String> arrayAdapter;
    private Set<BluetoothDevice> pairedDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        //get the system bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        deviceNames = new ArrayList<>();


        deviceListListView = (ListView)findViewById(R.id.connect_activity_device_list_lv);
        deviceListListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(ConnectActivity.this, "Item is selected", Toast.LENGTH_SHORT).show();
                returnActivityResult(deviceNames.get(position));
            }
        });

        //make an array adapter
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames);

        //attach the adapter to the listview
        final ListView deviceListListView = (ListView) findViewById(R.id.connect_activity_device_list_lv);
        deviceListListView.setAdapter(arrayAdapter);


        if(bluetoothAdapter == null)
        {
            Log.e(tag, "Bluetooth not supported on this device");
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
        }
        else
        {
            //populate the list view with the bonded devices
            populateDeviceNames();

            //check if bluetooth is enabled
            if(!bluetoothAdapter.isEnabled())
            {
                //enable the bluetooth
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBT, REQUEST_ENABLE_BT);
            }

        }
    }

    protected void populateDeviceNames()
    {
        //get the bonded devices
        pairedDevices = bluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            //populate the string
            for(BluetoothDevice device : pairedDevices)
            {
                deviceNames.add(device.getName());
                //Toast.makeText(this, "Added paired devices", Toast.LENGTH_SHORT).show();
            }
        }

        arrayAdapter.notifyDataSetChanged();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //if this result is for the enable bluetooth request
        if(requestCode == REQUEST_ENABLE_BT)
        {
            Log.i(tag, "Bluetooth activated");
            populateDeviceNames();
            arrayAdapter.notifyDataSetChanged();
        }
    }

    public void returnActivityResult (String bluetoothDevice)
    {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(getString(R.string.device_name_tag), bluetoothDevice);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

}







