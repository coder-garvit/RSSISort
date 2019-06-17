package com.example.myapplication;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.ArmaRssiFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    private BeaconManager beaconManager;
    TextView textView;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermission();

        setContentView(R.layout.activity_main);

        //getting beaconManager instance (object) for Main Activity class
        beaconManager = BeaconManager.getInstanceForApplication(this);

        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));

        //Binding MainActivity to the BeaconService.
        beaconManager.bind(this);
    }

    // Checking Permission whether ACCESS_COARSE_LOCATION permssion is granted or not
    public void checkPermission(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                AlertDialog.Builder builder=new AlertDialog.Builder(this);
                builder.setTitle("Location Permission");
                builder.setPositiveButton("OK",null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSION_REQUEST_COARSE_LOCATION);
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
        final Region region = new Region("myBeaons",null, null, null);

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
                System.out.println( "I have just switched from seeing/not seeing beacons: "+state);
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
                    for (Beacon b:beacons){
                        //RSSI
                        Log.e("In MainActivity","RSSI is");
                        Integer rssi = Integer.valueOf(b.getRssi());
                        arr.add(rssi);
                    }

                    /*textView = findViewById(R.id.textView1);
                    textView.setText(arr.get(0));
                    textView = findViewById(R.id.textView2);
                    textView.setText(arr.get(1));
                    */
                    Integer maxRssi = Collections.max(arr);
                    textView = findViewById(R.id.textView);
                    textView.setText(Integer.toString(maxRssi));
                }

                if (beacons.size() == 0)
                {
                    textView =  findViewById(R.id.textView);
                    textView.setText("No beacons found :(");
                }
            }
        });
        try {

            //Tells the BeaconService to start looking for beacons that match the passed Region object.
            beaconManager.startMonitoringBeaconsInRegion(region);
        } catch (RemoteException e) {    }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Unbinds an Android Activity or Service to the BeaconService to avoid leak.
        beaconManager.unbind(this);
    }

}
