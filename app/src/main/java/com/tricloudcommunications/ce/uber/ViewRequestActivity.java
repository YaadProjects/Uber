package com.tricloudcommunications.ce.uber;

import android.*;
import android.content.Context;
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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

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

    public void updateListView(Location requestLocations){

        if (requestLocations != null) {

            requests.clear();

            ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");

            final ParseGeoPoint geopointLocation = new ParseGeoPoint(requestLocations.getLatitude(), requestLocations.getLongitude());
            query.whereNear("location", geopointLocation);
            query.setLimit(10);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {

                    if (e == null){

                        if (objects.size() > 0){

                            for (ParseObject object : objects){

                                Double distanceInMiles = geopointLocation.distanceInMilesTo((ParseGeoPoint) object.get("location"));

                                //Rounding to one decimal place
                                Double distanceoneDP = (double) Math.round(distanceInMiles * 10) / 10;

                                requests.add(distanceoneDP.toString() + " miles");

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

        requestListView = (ListView) findViewById(R.id.requestListView);
        requests = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, requests);
        requestListView.setAdapter(arrayAdapter);
        requests.clear();
        requests.add("Getting nearby requests...");



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

    }

    @Override
    public void onLocationChanged(Location location) {

        /**
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

        Log.i("Latitude", lat.toString());
        Log.i("Longitude", lng.toString());
        **/

        updateListView(location);


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
        locationManager.requestLocationUpdates(provider, 400, 1, this);
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
