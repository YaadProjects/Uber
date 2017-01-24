package com.tricloudcommunications.ce.uber;

import android.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class ViewRequestActivity extends AppCompatActivity implements LocationListener{

    LocationManager locationManager;
    String provider;
    Location location;

    ListView requestListView;
    ArrayList<String> requests;
    ArrayAdapter arrayAdapter;

    ArrayList<String> riderUserNames;

    ArrayList<Double> requestLatitudes = new ArrayList<Double>();
    ArrayList<Double> requestLongitude = new ArrayList<Double>();

    public void updateListView(Location requestLocations){

        if (requestLocations != null) {

            final ParseGeoPoint geoPointLocation = new ParseGeoPoint(requestLocations.getLatitude(), requestLocations.getLongitude());

            ParseUser.getCurrentUser().put("location", geoPointLocation);
            ParseUser.getCurrentUser().saveInBackground();

            ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
            query.whereNear("location", geoPointLocation);
            //query.whereDoesNotExist("driverUsername");
            query.whereEqualTo("driverUsername", "");
            query.setLimit(100);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {

                    if (e == null){

                        requests.clear();
                        requestLatitudes.clear();
                        requestLongitude.clear();

                        if (objects.size() > 0){

                            for (ParseObject object : objects){

                                ParseGeoPoint riderRequestLocations = (ParseGeoPoint) object.get("location");

                                if (riderRequestLocations !=null) {

                                    Double distanceInMiles = geoPointLocation.distanceInMilesTo(riderRequestLocations);

                                    //Rounding to one decimal place
                                    Double distanceOneDP = (double) Math.round(distanceInMiles * 10) / 10;

                                    requests.add(distanceOneDP.toString() + " miles");

                                    requestLatitudes.add(riderRequestLocations.getLatitude());
                                    requestLongitude.add(riderRequestLocations.getLongitude());

                                    riderUserNames.add(object.getString("username"));

                                    Log.i("info", "list updated from Method");
                                }
                            }

                        }else {

                            requests.add("No active request nearby...");
                        }

                        arrayAdapter.notifyDataSetChanged();
                    }
                }
            });

        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_request);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Neaby Requests");

        requestListView = (ListView) findViewById(R.id.requestListView);
        riderUserNames = new ArrayList<String>();
        requests = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, requests);
        requestListView.setAdapter(arrayAdapter);
        requests.clear();
        requestLatitudes.clear();
        requestLongitude.clear();
        //requests.add("Getting nearby requests...");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), false);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        location = locationManager.getLastKnownLocation(provider);

        updateListView(location);

        requestListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (requestLatitudes.size() > position && requestLongitude.size() > position && riderUserNames.size() > position && location != null){

                    Intent intent = new Intent(getApplicationContext(), DriverLocationActivity.class);
                    intent.putExtra("riderLatitude", requestLatitudes.get(position));
                    intent.putExtra("riderLongitude", requestLongitude.get(position));
                    intent.putExtra("driverLatitude", location.getLatitude());
                    intent.putExtra("driverLongitude", location.getLongitude());
                    intent.putExtra("riderUserName", riderUserNames.get(position));
                    startActivityForResult(intent, 1);

                }

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            // refresh list upon return
            updateListView(location);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.view_request_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.view_request_action_settings) {

            ParseQuery<ParseUser> query = ParseUser.getQuery();
            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseUser>() {
                @Override
                public void done(List<ParseUser> objects, ParseException e) {

                    if (e == null){

                        if (objects.size() >0){

                            for (ParseUser object : objects){

                                object.deleteInBackground(new DeleteCallback() {
                                    @Override
                                    public void done(ParseException e) {

                                        if (e == null){

                                            ParseUser.logOut();
                                            Intent intent = new Intent(ViewRequestActivity.this, MainActivity.class);
                                            startActivity(intent);

                                        }else {

                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }
                        }

                    }else {

                        e.printStackTrace();
                    }
                }
            });

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationChanged(Location location) {

        Double lat = location.getLatitude();
        Double lng = location.getLongitude();

        LatLngBounds latLngBounds = new LatLngBounds(new LatLng(lat, lng), new LatLng(lat, lng));

        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        try {

            List<Address> addressList = geocoder.getFromLocation(lat,lng,1);

            if (addressList !=null && addressList.size() > 0){

                Log.i("Address Main Info", addressList.toString());
                Log.i("Address Info 0", addressList.get(0).getAddressLine(0));
                Log.i("Address Info 1", addressList.get(0).getAddressLine(1));
                Log.i("Address Info 2", addressList.get(0).getAddressLine(2));

            }

        } catch (IOException e) {

            e.printStackTrace();
        }

        Log.i("Driver Latitude", lat.toString());
        Log.i("Driver Longitude", lng.toString());

        //requests.clear();
        //requestLatitudes.clear();
        //requestLongitude.clear();
        updateListView(location);
        Log.i("info", "list updated fron onLocationChanged");

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(provider, 400, 0, this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.removeUpdates(this);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
