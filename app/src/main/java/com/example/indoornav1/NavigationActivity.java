package com.example.indoornav1;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.ArmaRssiFilter;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NavigationActivity extends AppCompatActivity  implements BeaconConsumer {
    ProgressDialog progressDialog;
    public TextView tv;
    public ImageView imageView;
    public String start;
    public String end;
    DatabaseReference databaseReference;
    DataSnapshot dataSnapshot;
    private NetworkImageView mNetworkImageView;
    private ImageLoader mImageLoader;
    String bid; //ToDo: To be changed to String
    String imageURL;
    String navStatus;

    //added by garvit
    private BeaconManager beaconManager;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //added by garvit
        checkPermission();

        //getting beaconManager instance (object) for Main Activity class
        beaconManager = BeaconManager.getInstanceForApplication(this);

        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));

        //Binding MainActivity to the BeaconService.
        beaconManager.bind(this);

        setContentView(R.layout.activity_navigation);
        //start=getNearestBeacon();
        start = "bid";

        end = getIntent().getStringExtra("payload");
        Log.e("SearchQuery", end + " is null?");
        tv = findViewById(R.id.textView);
        //ImageView imageView=findViewById(R.id.mapImage);
        //navStatus=new HashMap<>();
        Navigate navigate = new Navigate();
        navigate.execute(start, end);


    }


    public class Navigate extends AsyncTask<String, String, String> {


        @Override
        protected String doInBackground(String... strings) {
            //REST Query
            String cs, ps = null;
            try {

                //The Below statement will run as follows
                /*
                If cs(current state of nearest beacon) will be equal to ps, then no beacon will be loaded
                At (1) the value of cs changes then cs!=ps. In this case a new beacon will be detected and location will be updated

                //Dont get confused with  (2) and (1), Later modify the statement such that only end bid is passed to the asynctask
                //(3): When no beacon is recognised, nothing is done, just the loop is contunued so that a value might be read
                 */

                while (!strings[0].equals(strings[1])) {
                    cs = strings[0];  //---(2)
                    //ToDo: Uncomment the below statement later
                    //cs=getNearestBeacon();  //----(1)
                    if (cs.equals(ps) || cs == null)  //-->(3)
                        continue;
                    else {
                        navStatus = callAPI(strings[0], strings[1]);
                        Log.e("Order", "After callAPI.Data is " + navStatus);
                        //                    //For Testing Purpose
                        //                    if(i==1){ navStatus="https://res.infoq.com/presentations/rest-api-testing-postman/en/slides/sl1-1542245696017.jpg";}
                        //                    if(i==2){navStatus="https://octoperf.com/img/blog/jmeter-rest-api-testing/jmeter-rest-api-testing.jpg";}
                        ps = cs;
                        publishProgress(navStatus);
                    }
                }


            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //ToDo: If imageURL is null then server is down probably
            Log.e("Data ", "Data is" + imageURL);
            return "none";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(NavigationActivity.this);
            progressDialog.setMessage("Maps Loading");
            progressDialog.setCancelable(false);
            progressDialog.show();

        }


        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            progressDialog.dismiss();
            Toast.makeText(getApplicationContext(), "onProgressUpdate", Toast.LENGTH_SHORT).show();
            mNetworkImageView = (NetworkImageView) findViewById(R.id.mapImage);
            mImageLoader = CustomVolleyRequestQueue.getInstance(getApplicationContext()).getImageLoader();
            mImageLoader.get(values[0], ImageLoader.getImageListener(mNetworkImageView, R.mipmap.ic_launcher_round, android.R.drawable.ic_menu_report_image));
            Log.e("Image Published", values[0]);
            mNetworkImageView.setImageUrl(values[0], mImageLoader);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressDialog.dismiss();
            tv.setText(s);
            Log.e("Data ", "Data is" + s);
//                mNetworkImageView = (NetworkImageView) findViewById(R.id.mapImage);
//                mImageLoader = CustomVolleyRequestQueue.getInstance(getApplicationContext()).getImageLoader();
//                mImageLoader.get(s, ImageLoader.getImageListener(mNetworkImageView, R.mipmap.ic_launcher_round, android.R.drawable.ic_menu_report_image));
//                mNetworkImageView.setImageUrl(s, mImageLoader);

        }
    }


    private String callAPI(String start, String end) throws IOException, JSONException {
        Log.e("Inside CallAPI", start);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("src", start);
            jsonObject.put("dest", end);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        URL url = new URL("https://suhas-api.herokuapp.com/map");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Content-Length", String.valueOf(jsonObject.toString().getBytes().length));
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Content-Language", "en-US");
        //Send Request
        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(jsonObject.toString().getBytes());
        outputStream.flush();
        outputStream.close();

        //Get Response
        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuffer response = new StringBuffer();
        while ((line = rd.readLine()) != null) {
            response.append(line);
            response.append('\r');
        }
        rd.close();


        String imageLink = response.toString();
        Log.e("In callAPI", imageLink);
        imageLink = new JSONObject(imageLink).getString("response");
        Log.e("Order", "Step-1");
        return imageLink;


    }


    private String getNearestBeacon() {


        return "1";
    }

    @Override
    protected void onStart() {
        super.onStart();
        Toast.makeText(getApplicationContext(), "OnStart STarted", Toast.LENGTH_LONG).show();
        Log.e("Order", "Step-2");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(getApplicationContext(), "OnResume STarted", Toast.LENGTH_LONG).show();
        Log.e("Order", "Step-2");
    }


//toDo: Center the image while its loading
//Todo: Handle all the errors in the app. Handle the communication erros by reading the error variable in JSON response
//todo: Do this whenever the beacon value changes OR every 4s

    // Checking Permission whether ACCESS_COARSE_LOCATION permssion is granted or not
    public void checkPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Location Permission");
                builder.setPositiveButton("OK", null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {

                // If Permission is Granted than its ok
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                }

                // If not Granted then alert the user by the message
                else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            checkPermission();
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }


    @Override
    public void onBeaconServiceConnect() {

        //Constructing a new Region object to be used for Ranging or Monitoring
        final Region region = new Region("myBeaons", null, null, null);

        //Specifies a class that should be called each time the BeaconService sees or stops seeing a Region of beacons.
        beaconManager.addMonitorNotifier(new MonitorNotifier() {

            /*
                This override method is runned when some beacon will come under the range of device.
            */
            @Override
            public void didEnterRegion(Region region) {
                System.out.println("ENTER ------------------->");
                try {

                    //Tells the BeaconService to start looking for beacons that match the passed Region object
                    // , and providing updates on the estimated mDistance every seconds while beacons in the Region
                    // are visible.
                    beaconManager.startRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            /*
                 This override method is runned when beacon that comes in the range of device
                 ,now been exited from the range of device.
             */
            @Override
            public void didExitRegion(Region region) {
                System.out.println("EXIT----------------------->");
                try {

                    //Tells the BeaconService to stop looking for beacons
                    // that match the passed Region object and providing mDistance
                    // information for them.
                    beaconManager.stopRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }


            /*
                 This override method will Determine the state for the device , whether device is in range
               of beacon or not , if yes then i = 1 and if no then i = 0
            */
            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                System.out.println("I have just switched from seeing/not seeing beacons: " + state);
            }
        });


        //Specifies a class that should be called each time the BeaconService gets ranging data,
        // which is nominally once per second when beacons are detected.
        beaconManager.addRangeNotifier(new RangeNotifier() {

            /*
               This Override method tells us all the collections of beacons and their details that
               are detected within the range by device
             */
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {

                // Checking if the Beacon inside the collection (ex. list) is there or not

                // if Beacon is detected then size of collection is > 0
                if (beacons.size() > 0) {

                    final ArrayList<ArrayList<String>> arrayList = new ArrayList<ArrayList<String>>();
                    BeaconManager.setRssiFilterImplClass(ArmaRssiFilter.class);
                    // Iterating through all Beacons from Collection of Beacons
                    ArrayList<Integer> arr = new ArrayList<Integer>();
                    int count = 0;
                    for (Beacon b : beacons) {
                        //RSSI
                        count += 1;
                        System.out.println("Count : " + count);
                        Integer rssi = Integer.valueOf(b.getRssi());
                        arr.add(rssi);
                    }

                    Integer maxRssi = Collections.max(arr);
                }
            }
        });
        try {

            //Tells the BeaconService to start looking for beacons that match the passed Region object.
            beaconManager.startMonitoringBeaconsInRegion(region);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Unbinds an Android Activity or Service to the BeaconService to avoid leak.
        beaconManager.unbind(this);
    }


}