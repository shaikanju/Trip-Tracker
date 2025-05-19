package com.anju.followme;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;


import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.anju.followme.databinding.TripleadactivityBinding;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.maps.android.SphericalUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TripLeadActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private static final String TAG = "TripLeadActivity";
    private final float zoomDefault = 15.0f;
    private FusedLocationProviderClient mFusedLocationClient;

    private PointReceiver pointReceiver;
    private Intent locationServiceIntent;
    private TripleadactivityBinding binding;
    private Polyline llHistoryPolyline;
    private final ArrayList<LatLng> latLonHistory = new ArrayList<>();
    private Marker carMarker;
    private String tripID;
    private String userID;
    private String firstName;
    private String lastName;
    private double totalDistance = 0.0;
    private long startTime = 0;
    private ObjectAnimator objectAnimator;
    private AnimatorSet animatorSet;
    SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    public static int screenHeight;
    public static int screenWidth;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = TripleadactivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getScreenDimensions();


        sdf2.setTimeZone(TimeZone.getTimeZone("UTC"));
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        startLocationService();

        Intent intent = getIntent();
        if (intent != null) {
            tripID = intent.getStringExtra("trip_id"); // Default to -1 if not found
            userID = intent.getStringExtra("user_id");
            firstName = intent.getStringExtra("first_name");
            lastName = intent.getStringExtra("last_name");

            Log.d("TripLeadActivity", "Trip ID: " + tripID);
            Log.d("TripLeadActivity", "User ID: " + userID);
            Log.d("TripLeadActivity", "First Name: " + firstName);
            Log.d("TripLeadActivity", "Last Name: " + lastName);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d, hh:mm a", Locale.ENGLISH);

        String currentTime = sdf.format(new Date());

        binding.textTrip.setText("Trip Id: "+ tripID);
        binding.textStart.setText(currentTime);




        binding.imageView6.setTag(R.drawable.pause);
        binding.imageView6.setOnClickListener(v->{
            if (binding.imageView6.getTag() != null && (int) binding.imageView6.getTag() == R.drawable.pause) {
                // Change to play button
                binding.imageView6.setImageResource(R.drawable.play);
                binding.imageView6.setTag(R.drawable.play);
            } else {
                // Change to pause button
                binding.imageView6.setImageResource(R.drawable.pause);
                binding.imageView6.setTag(R.drawable.pause);
            }
        });



binding.imageView9.setImageResource(R.drawable.broadcast);
        objectAnimator =
                ObjectAnimator.ofFloat(binding.imageView9, "alpha", 1.0f, 0.25f);
        binding.imageView9.setAlpha(1.0f);
        startObjectAnimators();

        setupNetworkCallback();
    }
    private void setupNetworkCallback() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        connectivityManager.registerNetworkCallback(
                builder.build(),
                new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        super.onAvailable(network);
                        runOnUiThread(() -> {
                            Toast.makeText(TripLeadActivity.this, "Network Available", Toast.LENGTH_SHORT).show();
                            binding.network.setVisibility(View.GONE);
                        });
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        super.onLost(network);
                        runOnUiThread(() -> {
                            Toast.makeText(TripLeadActivity.this, "Network Lost", Toast.LENGTH_SHORT).show();
                            binding.network.setVisibility(View.VISIBLE);
                            binding.network.setText("NO NETWORK CONNECTION\nNot all data may be received");
                        });
                    }
                }
        );
    }



    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        mMap = googleMap;
        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomDefault));
        mMap.setBuildingsEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
//        if (checkPermission()) {
//            mFusedLocationClient.getLastLocation()
//                    .addOnSuccessListener(this, location -> {
//
//                        LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());
////                        mMap.addMarker(new MarkerOptions().position(origin).title("My Origin"));
//                        Log.d("Map has started","latnlon"+origin);
//                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(origin, zoomDefault));
//                        startLocationService();
//                    })
//                    .addOnFailureListener(
//                            e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
//        }
    }
    private void getScreenDimensions() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;
    }

//    private boolean checkPermission() {
//        if (ContextCompat.checkSelfPermission(this,
//                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
//                PackageManager.PERMISSION_GRANTED) {
//
//            ActivityCompat.requestPermissions(this,
//                    new String[]{
//                            Manifest.permission.ACCESS_FINE_LOCATION
//                    }, LOCATION_REQUEST);
//            return false;
//        }
//        return true;
//    }


//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        if (requestCode == LOCATION_REQUEST) {
//            if (permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
//                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    if (checkPermission()) {
//                        mFusedLocationClient.getLastLocation()
//                                .addOnSuccessListener(this, location -> {
//                                    // Got last known location. In some rare situations this can be null.
//                                    // Add a marker at current location
//                                    LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());
////                        mMap.addMarker(new MarkerOptions().position(origin).title("My Origin"));
//                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(origin, zoomDefault));
//                                })
//                                .addOnFailureListener(
//                                        e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
//                    }
//                } else {
//                    Toast.makeText(this, "Location Permission not Granted", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//        }
//    }

    private void startLocationService() {


            // Create a receiver to get the location updates
            pointReceiver = new PointReceiver(this);

            // Register the receiver
            ContextCompat.registerReceiver(this,
                    pointReceiver,
                    new IntentFilter("com.example.broadcast.MY_BROADCAST"),
                    ContextCompat.RECEIVER_EXPORTED);

        //starting service
        locationServiceIntent = new Intent(this, LocationService.class);

        Log.d(TAG, "startService: START");
        ContextCompat.startForegroundService(this, locationServiceIntent);
        Log.d(TAG, "startService: END");

    }


private void startObjectAnimators() {

    objectAnimator.setDuration(750);
    objectAnimator.setRepeatCount(ObjectAnimator.INFINITE);
    objectAnimator.setRepeatMode(ObjectAnimator.REVERSE);

    animatorSet = new AnimatorSet();
    animatorSet.playTogether(objectAnimator);
    animatorSet.start();
}
    public void updateLocation(LatLng latLng, float bearing) {


        Log.d(TAG, "updateLocation: " + latLng + " " + bearing);
        if (!latLonHistory.isEmpty()) {
            // Compute distance from last location to the new one
            LatLng lastLocation = latLonHistory.get(latLonHistory.size() - 1);
            double distance = SphericalUtil.computeDistanceBetween(lastLocation, latLng)/1000.0; // Returns Kilometers
            totalDistance += distance; // Update total distance
            Log.d(TAG, "Distance from last point: " + distance + " meters");
        }
        binding.textDistance.setText("Distance: " + String.format("%.2f", totalDistance) + " km");

        latLonHistory.add(latLng);
        if (llHistoryPolyline != null) {
            llHistoryPolyline.remove(); // Remove old polyline
        }
        if (latLonHistory.size() == 1) { // First update



            startTime = System.currentTimeMillis();
            mMap.addMarker(new MarkerOptions().alpha(0.5f).position(latLng).title("My Origin").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomDefault));
            return;
        }
        long elapsedTimeMillis = System.currentTimeMillis() - startTime;
        String elapsedTime = formatElapsedTime(elapsedTimeMillis);
         binding.textElapsed.setText("elapsed: "+elapsedTime);
        if (latLonHistory.size() > 1) { // Second (or more) update
            PolylineOptions polylineOptions = new PolylineOptions();

            for (LatLng ll : latLonHistory) {
                polylineOptions.add(ll);
            }
            llHistoryPolyline = mMap.addPolyline(polylineOptions);
            llHistoryPolyline.setEndCap(new RoundCap());
            llHistoryPolyline.setWidth(12);
            llHistoryPolyline.setColor(Color.BLUE);

            float r = getRadius();
            if (r > 0) {
                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.car);
                Bitmap resized = Bitmap.createScaledBitmap(icon, (int) r, (int) r, false);
                BitmapDescriptor iconBitmap = BitmapDescriptorFactory.fromBitmap(resized);

                MarkerOptions options = new MarkerOptions();
                options.position(latLng);
                options.icon(iconBitmap);
                options.rotation(bearing);

                if (carMarker != null) {
                    carMarker.remove();
                }

                carMarker = mMap.addMarker(options);
            }
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        binding.imageView5.setVisibility(View.GONE);
        binding.textView7.setVisibility(View.GONE);
        Date now = new Date();
        String formatteddate = sdf2.format(now);
        if (binding.imageView6.getTag() != null && (int) binding.imageView6.getTag() == R.drawable.pause) {
        JSONObject userJson = new JSONObject();
        try{
            userJson.put("tripId",tripID);
            userJson.put("latitude",latLng.latitude);
            userJson.put("longitude",latLng.longitude);
            userJson.put("datetime",formatteddate);
            userJson.put("userName",userID);
        }
        catch(JSONException e){
            e.printStackTrace();
        }
        addTripPoint(userJson);}
    }
private void addTripPoint(JSONObject userJson )
{
    if (!NetworkChecker.hasNetworkConnection(this)) {
       binding.network.setVisibility(View.VISIBLE);

        return;
    }

    String url = "http://christopherhield-001-site4.htempurl.com/api/Datapoints/AddTripPoint";
    RequestQueue requestQueue = Volley.newRequestQueue(this);
    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, userJson,
            response -> {
                // Handle successful response
                try {
                    String tripID = response.getString("tripId");
                    double latitude= response.getDouble("latitude");
                    double longitude = response.getDouble("longitude");
                    String userName = response.getString("userName");

                    // Handle the successful user creation logic

                    Log.d("response",response.toString());


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> {
                // Handle error response (HTTP 400: Bad Request)
                Log.e("APIError", "Add Trip Point failed" + error.getMessage());
                if (error.networkResponse != null) {
                    int statusCode = error.networkResponse.statusCode;
                    String errorBody = new String(error.networkResponse.data);


                    Log.e("APIError", "Status Code: " + statusCode);
                    Log.e("APIError", "Error Body: " + errorBody);
                } else {
                    Log.e("APIError", "Network response is null (Check Internet or API URL).");
                }
            });

    requestQueue.add(jsonObjectRequest);
}


    private String formatElapsedTime(long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = (millis / (1000 * 60 * 60));

        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }
    private float getRadius() {
        float z = mMap.getCameraPosition().zoom;
        return 15f * z - 130f;
    }
    public void stopLocations(View v) {
        if (pointReceiver != null) {
            unregisterReceiver(pointReceiver);
        }
        finish();
        Date now = new Date();
        String formatteddate = sdf2.format(now);
        JSONObject userJson = new JSONObject();
        try{
            userJson.put("tripId",tripID);
            userJson.put("latitude",0.0);
            userJson.put("longitude",0.0);
            userJson.put("datetime",formatteddate);
            userJson.put("userName",userID);
        }
        catch(JSONException e){
            e.printStackTrace();
        }
        addTripPoint(userJson);

        stopService(locationServiceIntent);
    }

    public void shareTripId(View view) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Follow Me Trip Id: "+tripID);
        shareIntent.putExtra(Intent.EXTRA_TEXT, userID+" has shared a 'Follow Me' Trip ID with you.\nUse Follow Me Trip ID: "+tripID);
        startActivity(Intent.createChooser(shareIntent, "Share Trip ID via"));
    }
}
