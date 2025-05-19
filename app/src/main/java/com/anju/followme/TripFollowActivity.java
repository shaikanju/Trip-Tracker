package com.anju.followme;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.anju.followme.api.TripPointsFetcher;
import com.anju.followme.databinding.TripfollowmeBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;

import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TripFollowActivity extends FragmentActivity implements OnMapReadyCallback, TripPointsFetcher.TripPointsCallback {
    private TripfollowmeBinding binding;
    private GoogleMap mMap;
    private final float zoomDefault = 15.0f;
    private String tripID;
    private final ArrayList<LatLng> latLonHistory = new ArrayList<>();
    private TripPointsFetcher tripPointsFetcher;
    private Polyline llHistoryPolyline;
    private long tripStartTimeMillis;
    private SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
    private Handler handler = new Handler();
    private RequestQueue requestQueue;
    private Marker carMarker;
    private boolean shouldRecenterMap = true;
    private boolean done=true;

    private AnimatorSet animatorSet;
    private ObjectAnimator objectAnimator1;
    private Runnable fetchRunnable = new Runnable() {
        @Override
        public void run() {
            fetchLastLocation();
            handler.postDelayed(this, 5000); // Fetch every 5 seconds
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = TripfollowmeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        tripID = getIntent().getStringExtra("trip_id");
        binding.textTrip.setText(String.format("Trip ID: %s", tripID));

        tripPointsFetcher = new TripPointsFetcher(this, this);
        requestQueue = Volley.newRequestQueue(this);
        binding.targetIcon.setOnClickListener(v -> {
            shouldRecenterMap = !shouldRecenterMap;
            Toast.makeText(this, shouldRecenterMap ? "Re-centering enabled" : "Re-centering disabled", Toast.LENGTH_SHORT).show();
        });
binding.imageView9.setImageResource(R.drawable.broadcast);
        objectAnimator1 =
                ObjectAnimator.ofFloat(binding.imageView9, "alpha", 1.0f, 0.25f);
        binding.imageView9.setAlpha(1.0f);
        startObjectAnimators();

        setupNetworkCallback();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomDefault));
        mMap.setBuildingsEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        Log.d("map loaded", "map loaded1");
        if (tripID != null) {
            tripPointsFetcher.fetchTripPoints(tripID);
        }
    }


    @Override
    public void onTripPointsFetched(ArrayList<LatLng> latLonHistory, String startTime, String elapsedTime, String distance) {
        if (!NetworkChecker.hasNetworkConnection(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Follow Me- No Network");
            builder.setMessage("No network connection. Cnnot access trip data now \n Cannot follow trip now");
            builder.setNegativeButton("OK", (dialog, which) -> dialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();
            if (animatorSet.isRunning()) {
                animatorSet.cancel();
            }
            return;
        }
        this.latLonHistory.clear();
        this.latLonHistory.addAll(latLonHistory);
        if (latLonHistory.size() > 1) {
            LatLng origin = latLonHistory.get(0);
            mMap.addMarker(new MarkerOptions()
                    .position(origin)
                    .title("Origin")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
               mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 17.0f));}
        displayTripPath();

        // Format and display the start time
        if (startTime != null) {
            try {
                Date startDate = inputFormat.parse(startTime);
                tripStartTimeMillis = startDate.getTime();
                SimpleDateFormat outputFormat = new SimpleDateFormat("EEE MMM d, hh:mm a", Locale.getDefault());
                String formattedDate = outputFormat.format(startDate);
                binding.textStart.setText(String.format("Trip Start: %s", formattedDate));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // Display elapsed time and distance
        binding.textElapsed.setText(String.format("Elapsed: %s", elapsedTime));
        binding.textDistance.setText(String.format("Distance: %s", distance));

        handler.post(fetchRunnable); // Start fetching last location
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
                            Toast.makeText(TripFollowActivity.this, "Network Available", Toast.LENGTH_SHORT).show();
                            binding.network.setVisibility(View.GONE);
                        });
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        super.onLost(network);
                        runOnUiThread(() -> {
                            Toast.makeText(TripFollowActivity.this, "Network Lost", Toast.LENGTH_SHORT).show();


                        });
                    }
                }
        );
    }
    private void startObjectAnimators() {

        objectAnimator1.setDuration(750);
        objectAnimator1.setRepeatCount(ObjectAnimator.INFINITE);
        objectAnimator1.setRepeatMode(ObjectAnimator.REVERSE);



        animatorSet = new AnimatorSet();
        animatorSet.playTogether(objectAnimator1);
        animatorSet.start();
        Log.d("animation started","animation started");
    }



    @Override
    public void onError(String errorMessage) {
        Toast.makeText(this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
    }

    private void displayTripPath() {
        if (mMap == null) {
            Log.e("displayTripPath", "GoogleMap is not ready");
            return;
        }

        if (latLonHistory.size() > 1) {



            // Check if the last element has lat and long of 0 and 0
            LatLng lastPoint = latLonHistory.get(latLonHistory.size() - 1);
            if (lastPoint.latitude == 0 && lastPoint.longitude == 0) {
                latLonHistory.remove(latLonHistory.size() - 1);
                LatLng destination = latLonHistory.get(latLonHistory.size() - 1);
                mMap.addMarker(new MarkerOptions()
                        .position(destination)
                        .title("Destination")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            }
            PolylineOptions polylineOptions = new PolylineOptions();
            for (LatLng ll : latLonHistory) {
                polylineOptions.add(ll);
            }
            llHistoryPolyline = mMap.addPolyline(polylineOptions);
            llHistoryPolyline.setEndCap(new RoundCap());
            llHistoryPolyline.setWidth(12);
            llHistoryPolyline.setColor(Color.BLUE);
            Log.d("displayTripPath", "Trip path displayed");
        }
    }

    private void fetchLastLocation() {
        if (!NetworkChecker.hasNetworkConnection(this)) {
            binding.network.setVisibility(View.VISIBLE);
            if (animatorSet.isRunning()) {
                animatorSet.cancel();
            }
            return;
        }
        String url = "http://christopherhield-001-site4.htempurl.com/api/Datapoints/GetLastLocation/" + tripID;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        double lat = response.getDouble("latitude");
                        double lon = response.getDouble("longitude");
                        String datetime = response.getString("datetime");
                        if (lat == 0 && lon == 0) {
                            showTripEndedDialog();
                            if (animatorSet.isRunning()) {
                                animatorSet.cancel();
                            }
                            handler.removeCallbacks(fetchRunnable); // Stop fetching
                        } else {
                            LatLng lastLocation = new LatLng(lat, lon);
                            if (!latLonHistory.isEmpty()) {
                                LatLng previousLocation = latLonHistory.get(latLonHistory.size() - 1);
                                double distance = calculateDistance(previousLocation, lastLocation);
                                updateDistance(distance);
                            }
                            latLonHistory.add(lastLocation); // Add new location to history
                            displayTripPath(); // Redraw polyline
                            displayAutomobileIcon(lastLocation);
                            if (shouldRecenterMap) {
                                mMap.animateCamera(CameraUpdateFactory.newLatLng(lastLocation));
                            }

                            // Calculate elapsed time from the start of the trip
                            Date lastDate = inputFormat.parse(datetime);
                            long elapsedMillis = lastDate.getTime() - tripStartTimeMillis;
                            updateElapsedTime(elapsedMillis);
                        }
                    } catch (JSONException | ParseException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Failed to fetch last location", Toast.LENGTH_SHORT).show());

        requestQueue.add(jsonObjectRequest);
    }

    private void updateDistance(double additionalDistance) {
        String currentDistanceText = binding.textDistance.getText().toString();
        // Extract the numeric part of the distance string
        String numericPart = currentDistanceText.replace("Distance: ", "").replace(" km", "");
        double currentDistance = Double.parseDouble(numericPart);
        double newDistance = currentDistance + additionalDistance;
        String distanceFormatted = String.format(Locale.getDefault(), "%.2f km", newDistance);
        binding.textDistance.setText(String.format("Distance: %s", distanceFormatted));
    }
    private float calculateBearing(LatLng start, LatLng end) {
        double startLat = Math.toRadians(start.latitude);
        double startLng = Math.toRadians(start.longitude);
        double endLat = Math.toRadians(end.latitude);
        double endLng = Math.toRadians(end.longitude);

        double dLng = endLng - startLng;
        double x = Math.sin(dLng) * Math.cos(endLat);
        double y = Math.cos(startLat) * Math.sin(endLat) - Math.sin(startLat) * Math.cos(endLat) * Math.cos(dLng);

        return (float) Math.toDegrees(Math.atan2(x, y));
    }
    private void updateElapsedTime(long elapsedMillis) {
        long seconds = (elapsedMillis / 1000) % 60;
        long minutes = (elapsedMillis / (1000 * 60)) % 60;
        long hours = (elapsedMillis / (1000 * 60 * 60));
        String elapsedTimeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        binding.textElapsed.setText(String.format("Elapsed: %s", elapsedTimeFormatted));
    }

    private void showTripEndedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Trip Ended");
        builder.setMessage("The trip has ended.");
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    private void displayAutomobileIcon(LatLng location) {
        // Implement the method to display an automobile icon at the given location
        float r = getRadius();
        if (r > 0) {
            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.car);
            Bitmap resized = Bitmap.createScaledBitmap(icon, (int) r, (int) r, false);
            BitmapDescriptor iconBitmap = BitmapDescriptorFactory.fromBitmap(resized);

            MarkerOptions options = new MarkerOptions();
            options.position(location);
            options.icon(iconBitmap);

            if (!latLonHistory.isEmpty()) {
                LatLng previousLocation = latLonHistory.get(latLonHistory.size() - 1);
                float bearing = calculateBearing(previousLocation, location);
                options.rotation(bearing);
            }
            if (carMarker != null) {
                carMarker.remove();
            }

            carMarker = mMap.addMarker(options);
        } // Replace with your automobile icon resource
    }
    private double calculateDistance(LatLng start, LatLng end) {
        double earthRadius = 6371.0; // kilometers
        double dLat = Math.toRadians(end.latitude - start.latitude);
        double dLng = Math.toRadians(end.longitude - start.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(start.latitude)) * Math.cos(Math.toRadians(end.latitude)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
    private float getRadius() {
        float z = mMap.getCameraPosition().zoom;
        return 15f * z - 130f;
    }
}