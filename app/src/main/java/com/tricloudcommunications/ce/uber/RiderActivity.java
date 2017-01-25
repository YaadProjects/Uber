package com.tricloudcommunications.ce.uber;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseSession;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class RiderActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, LocationListener {

    private GoogleMap mMap;

    LocationManager locationManager;
    String provider;
    Location location;
    Button callUberButton;
    Button riderLogOutButton;
    TextView riderInfoTextView;
    Boolean requestActive = false;
    Boolean isDriverActive = false;
    Handler handler;

    public void checkForUpdates() {

            ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
            query.whereNotEqualTo("driverUsername", "");

            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {

                    if (e == null && objects.size() > 0) {

                        isDriverActive = true;

                        ParseQuery<ParseUser> getDriverLocationQuery = ParseUser.getQuery();
                        getDriverLocationQuery.whereEqualTo("username", objects.get(0).getString("driverUsername"));
                        getDriverLocationQuery.findInBackground(new FindCallback<ParseUser>() {
                            @Override
                            public void done(List<ParseUser> objects, ParseException e) {

                                if (ActivityCompat.checkSelfPermission(RiderActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(RiderActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

                                if (e == null && objects.size() > 0) {

                                    ParseGeoPoint driverCurrentLocation = objects.get(0).getParseGeoPoint("location");

                                    if (location != null) {

                                        ParseGeoPoint riderCurrentLocztion = new ParseGeoPoint(location.getLatitude(), location.getLongitude());

                                        //Update driver location on the map so rider can see the driver location
                                        LatLng driverOnMapLocation = new LatLng(driverCurrentLocation.getLatitude(), driverCurrentLocation.getLongitude());
                                        LatLng riderOnMapLocation = new LatLng(location.getLatitude(), location.getLongitude());

                                        mMap.clear();
                                        ArrayList<Marker> markers = new ArrayList<>();
                                        markers.add(mMap.addMarker(new MarkerOptions().position(driverOnMapLocation).title("Driver Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))));
                                        markers.add(mMap.addMarker(new MarkerOptions().position(riderOnMapLocation).title("Your Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))));

                                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                        for (Marker marker : markers) {
                                            builder.include(marker.getPosition());
                                        }
                                        LatLngBounds bounds = builder.build();

                                        int padding = 100; // offset from edges of the map in pixels
                                        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                                        mMap.animateCamera(cu);

                                        Double distanceInMiles = driverCurrentLocation.distanceInMilesTo(riderCurrentLocztion);

                                        //Rounding to one decimal place
                                        Double distanceOneDP = (double) Math.round(distanceInMiles * 10) / 10;

                                        if (distanceInMiles <= 0.01) {
                                            riderInfoTextView.setText("Your driver is " + distanceOneDP.toString() + " miles away!");
                                            callUberButton.setVisibility(View.INVISIBLE);
                                            riderLogOutButton.setVisibility(View.INVISIBLE);
                                        }else {
                                            //mMap.clear();

                                            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

                                            try {

                                                List<android.location.Address> addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                                                if (addressList != null && addressList.size() > 0) {

                                                    //Sources: https://developers.google.com/maps/documentation/android-api/views
                                                    //mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 0));
                                                    //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat,lng), 18));
                                                    //LatLngBounds is needed to avoid have partial map show instead of complete map

                                                    LatLngBounds latLngBounds = new LatLngBounds(new LatLng(location.getLatitude(), location.getLongitude()), new LatLng(location.getLatitude(), location.getLongitude()));
                                                    mMap.clear();
                                                    mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())).title(addressList.get(0).getAddressLine(0)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngBounds.getCenter(), 15));
                                                    mMap.addCircle(new CircleOptions().center(new LatLng(location.getLatitude(), location.getLongitude())).radius(10).strokeColor(Color.RED).fillColor(Color.BLUE));

                                                    Log.i("Address Main Info", addressList.toString());
                                                    Log.i("Address Info 0", addressList.get(0).getAddressLine(0));
                                                    Log.i("Address Info 1", addressList.get(0).getAddressLine(1));
                                                    Log.i("Address Info 2", addressList.get(0).getAddressLine(2));

                                                }

                                            } catch (IOException error) {

                                                error.printStackTrace();

                                            }

                                            riderInfoTextView.setText("Your driver is here!");
                                            callUberButton.setVisibility(View.VISIBLE);
                                            riderLogOutButton.setVisibility(View.VISIBLE);

                                        }
                                    }


                                }
                            }
                        });
                    } else {

                        isDriverActive = false;
                        riderInfoTextView.setText("");
                        callUberButton.setVisibility(View.VISIBLE);
                        riderLogOutButton.setVisibility(View.VISIBLE);

                        //Toast.makeText(getApplicationContext(), "Waiting for a Driver", Toast.LENGTH_LONG).show();

                        Double lat = location.getLatitude();
                        Double lng = location.getLongitude();

                        LatLngBounds latLngBounds = new LatLngBounds(new LatLng(lat, lng), new LatLng(lat, lng));

                        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

                        try {

                            List<android.location.Address> addressList = geocoder.getFromLocation(lat, lng, 1);

                            if (addressList != null && addressList.size() > 0) {

                                //Sources: https://developers.google.com/maps/documentation/android-api/views
                                //mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 0));
                                //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat,lng), 18));
                                //LatLngBounds is needed to avoid have partial map show instead of complete map

                                mMap.clear();
                                mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).title(addressList.get(0).getAddressLine(0)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngBounds.getCenter(), 15));
                                mMap.addCircle(new CircleOptions().center(new LatLng(lat, lng)).radius(10).strokeColor(Color.RED).fillColor(Color.BLUE));

                                Log.i("Address Main Info", addressList.toString());
                                Log.i("Address Info 0", addressList.get(0).getAddressLine(0));
                                Log.i("Address Info 1", addressList.get(0).getAddressLine(1));
                                Log.i("Address Info 2", addressList.get(0).getAddressLine(2));

                            }

                        } catch (IOException error) {

                            error.printStackTrace();

                        }

                        Log.i("Latitude", lat.toString());
                        Log.i("Longitude", lng.toString());
                    }

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            checkForUpdates();

                        }
                    }, 2000);
                }

            });

    }

    public void riderLogOut(){

        //Check to see if a request has been put for the current user and delete the request
        ParseQuery<ParseObject> queryRequest = new ParseQuery<ParseObject>("Request");
        queryRequest.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        queryRequest.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {

                if (e == null){

                    if (objects.size() >0){

                        for (ParseObject object : objects){

                            object.deleteInBackground();
                        }
                    }
                }else {

                    e.printStackTrace();
                }
            }
        });

        //After deleting any requests, delete the user and the logout to delete the session
        ParseQuery<ParseUser> queryUser = ParseUser.getQuery();
        queryUser.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        queryUser.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> objects, ParseException e) {

                if (e == null){

                    if (objects.size() > 0){

                        ParseUser.logOut();
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);

                        /**
                        for (ParseObject object : objects){

                            object.deleteInBackground(new DeleteCallback() {
                                @Override
                                public void done(ParseException e) {

                                    if (e == null){

                                        ParseUser.logOut();
                                        Intent intent = new Intent(RiderActivity.this, MainActivity.class);
                                        startActivity(intent);

                                    }else {

                                        Log.i("info", e.toString());
                                    }
                                }
                            });
                        }
                        **/
                    }

                }else {

                    Log.i("info", e.toString());
                }
            }
        });

    }

    public void callUber(){

        Log.i("Info", "Call Uber");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

        if (requestActive){

            //Check to see if a request has been put for the current user
            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {

                    if (e == null){

                        if (objects.size() >0){

                            for (ParseObject object : objects){

                                object.deleteInBackground();
                            }

                            requestActive = false;
                            callUberButton.setText("Call An Uber");
                        }
                    }else {

                        Log.i("Info", e.toString());
                    }
                }
            });

        }else {
            if (location != null) {

                //Put in a Uber request for the current user
                ParseObject request = new ParseObject("Request");

                request.put("username", ParseUser.getCurrentUser().getUsername());
                request.put("driverUsername", "");

                ParseGeoPoint parseGeoPoint = new ParseGeoPoint(location.getLatitude(), location.getLongitude());

                request.put("location", parseGeoPoint);

                request.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {

                        if (e == null) {

                            requestActive = true;
                            callUberButton.setText("Cancel Uber");

                            checkForUpdates();
                        }
                    }
                });

            } else {

                Toast.makeText(this, "Could not find location. Please try again later.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        callUberButton = (Button) findViewById(R.id.callUberButton);
        riderLogOutButton = (Button) findViewById(R.id.riderLogOutButton);
        riderInfoTextView = (TextView) findViewById(R.id.infoTextView);
        handler = new Handler();

        riderLogOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                riderLogOut();

            }
        });

        callUberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                callUber();

            }
        });


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), false);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

        //Check to see if a request has been put for the current user
        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {

                if (e == null){

                    if (objects.size() >0){

                        requestActive = true;
                        callUberButton.setText("Cancel Uber");

                        checkForUpdates();

                    }
                }else {

                    Log.i("Info", e.toString());
                }
            }
        });

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);


       checkForUpdates();

        /**
        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("My Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));


        try {
            if (location != null) {

                onLocationChanged(location);

            }
        } catch (Exception e) {

            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Getting your location... Please Wait", Toast.LENGTH_LONG).show();
        }
        */
    }

    @Override
    public void onLocationChanged(Location location) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

    @Override
    public void onMapLongClick(LatLng latLng) {

    }
}
