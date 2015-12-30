package com.example.abdullah.hdc_0;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import cz.msebera.android.httpclient.Header;

public class GraphActivity extends AppCompatActivity {

    private String TAG = "GraphActivity";
    private ImageButton cloudRefreshButton;
    private GraphView tempGraph, humidityGraph;
    private TextView tv_time_range;
    private AsyncHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        //initialize the httpCient
        httpClient = new AsyncHttpClient();

        tempGraph = (GraphView) findViewById(R.id.tempGraph);
        tempGraph.setTitle("Temperature");
        humidityGraph = (GraphView) findViewById(R.id.humidityGraph);
        humidityGraph.setTitle("Humidity");

        cloudRefreshButton = (ImageButton) findViewById(R.id.cloudRefreshFab);
        cloudRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCloudData();
            }
        });

        tv_time_range = (TextView)findViewById(R.id.tv_time_range);
        tv_time_range.setText("Time Range: N/A");
    }
    private void drawGraphs(JSONArray jsonArray)
    {
        LineGraphSeries<DataPoint> tempSeries = new LineGraphSeries<DataPoint>();
        LineGraphSeries<DataPoint> humiditySeries = new LineGraphSeries<DataPoint>();

        for(int i = 0; i <=  jsonArray.length() ; i = i+1) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(jsonArray.length() - i);

                double temp = jsonObject.getDouble("temp");
                DataPoint dataPoint = new DataPoint(i, temp);
                tempSeries.appendData(dataPoint, true, 10);

                double humidity = jsonObject.getDouble("humidity");
                DataPoint dataPoint2 = new DataPoint(i, humidity);
                humiditySeries.appendData(dataPoint2, true, 10);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            long latestTime = jsonArray.getJSONObject(0).getLong("time");
            long oldestTime = jsonArray.getJSONObject(jsonArray.length() - 1).getLong("time");
            Date latestTimeS = new java.util.Date(latestTime*1000);
            Date oldestTimeS = new java.util.Date(oldestTime*1000);

            //java.util.Date time=new java.util.Date((long)timeStamp*1000);
            tv_time_range.setText(oldestTimeS.toLocaleString() + " to " + latestTimeS.toLocaleString());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.e(TAG, "Error parsing time from json array");
        }

        //uncomment these if time series need to be hidden
        tempGraph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        humidityGraph.getGridLabelRenderer().setHorizontalLabelsVisible(false);

        tempGraph.addSeries(tempSeries);
        tempGraph.setTitle("Temperature");
        humidityGraph.addSeries(humiditySeries);
        humidityGraph.setTitle("Humidity");
    }


    //get data from the cloud
    private void getCloudData()
    {
        //status.setText("Getting data from cloud...");
        String getURL = getResources().getString(R.string.getURL);
        httpClient.get(getURL, new JsonHttpResponseHandler() {

            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                //Toast.makeText(DataActivity.this, "getData: onSuccess: json Onject", Toast.LENGTH_SHORT).show();
            }

            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                //Toast.makeText(DataActivity.this, "getData: onSuccess: json array", Toast.LENGTH_SHORT).show();
                gotCloudData(response);
            }

        });
    }



    private void gotCloudData(JSONArray jsonArray) {
        //String response = jsonArray.toString();
        //textView.setText(response);
        //status.setText("Received data from cloud.");
        Toast.makeText(this, "Data received from Cloud", Toast.LENGTH_SHORT).show();
        drawGraphs(jsonArray);
    }

}
