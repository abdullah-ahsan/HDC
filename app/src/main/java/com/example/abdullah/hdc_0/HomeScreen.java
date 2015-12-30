package com.example.abdullah.hdc_0;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

public class HomeScreen extends AppCompatActivity {
    TextView tv_device_name;
    String deviceName = "";
    boolean deviceNameSet = false;

    int DEVICE_REQUEST_CODE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        Button btn_select_device = (Button) findViewById(R.id.btn_select_device);
        btn_select_device.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchSelectDeviceActivity();
            }
        });

        Button btn_connect = (Button) findViewById(R.id.btn_connect);
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDataActivity();
            }
        });

        tv_device_name = (TextView) findViewById(R.id.tv_current_device_name);


        //get the shared preferences previous device name
        SharedPreferences sharedPreferences = this.getSharedPreferences(getString(R.string.shared_preferences_file_key, Context.MODE_PRIVATE), Context.MODE_PRIVATE);
        deviceName = sharedPreferences.getString(getString(R.string.device_name_tag), getString(R.string.current_device_name_default));
        tv_device_name.setText(deviceName);

        if(!deviceName.equals(getString(R.string.current_device_name_default)))
        {
            deviceNameSet = true;
        }
    }

    //connect button click handler
    private void launchSelectDeviceActivity()
    {
        Intent intent = new Intent(this, ConnectActivity.class);
        startActivityForResult(intent, DEVICE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == DEVICE_REQUEST_CODE)
        {
            if(resultCode == RESULT_OK){
                deviceName = data.getStringExtra(getString(R.string.device_name_tag));
                tv_device_name.setText(deviceName);
                deviceNameSet = true;

                //save the device name for future app runs
                SharedPreferences sharedPreferences = this.getSharedPreferences(getString(R.string.shared_preferences_file_key), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(getString(R.string.device_name_tag), deviceName);
                editor.commit();
            }
        }
    }


    private void startDataActivity()
    {
        Intent intent = new Intent(this, DataActivity.class);
        startActivity(intent);
    }

}
