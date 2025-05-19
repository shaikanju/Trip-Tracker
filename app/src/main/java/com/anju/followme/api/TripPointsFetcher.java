package com.anju.followme.api;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TripPointsFetcher {
    private final RequestQueue requestQueue;
    private final TripPointsCallback callback;
    private final Context context;
    private final SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
    public TripPointsFetcher(Context context, TripPointsCallback callback) {
        this.requestQueue = Volley.newRequestQueue(context);
        this.callback = callback;
        this.context = context;
    }

    public void fetchTripPoints(String tripID) {
        String url = "http://christopherhield-001-site4.htempurl.com/api/Datapoints/GetTrip/" + tripID;

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    ArrayList<LatLng> latLonHistory = new ArrayList<>();
                    String startTime = null;
                    String endTime = null;
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject point = response.getJSONObject(i);
                            double lat = point.getDouble("latitude");
                            double lon = point.getDouble("longitude");
                            String datetime = point.getString("datetime");
                            Log.d("latnlong", "lat: " + lat + " lon: " + lon);
                            latLonHistory.add(new LatLng(lat, lon));

                            if (i == 0) {
                                startTime = datetime;
                            }
                        }

                        // Check if the last point has latitude and longitude equal to 0
                        if (!latLonHistory.isEmpty()) {
                            LatLng lastPoint = latLonHistory.get(latLonHistory.size() - 1);
                            if (lastPoint.latitude == 0 && lastPoint.longitude == 0 && latLonHistory.size() > 1) {
                                endTime = response.getJSONObject(response.length() - 2).getString("datetime");
                                LatLng secondLastPoint = latLonHistory.get(latLonHistory.size() - 2);
                                double distance = calculateDistance(latLonHistory.get(0), secondLastPoint);
                                String distanceFormatted = String.format(Locale.getDefault(), "%.2f km", distance);
                                Date startDate = inputFormat.parse(startTime);
                                Date endDate = inputFormat.parse(endTime);
                                long elapsedMillis = endDate.getTime() - startDate.getTime();
                                String elapsedTimeFormatted = formatElapsedTime(elapsedMillis);
                                callback.onTripPointsFetched(latLonHistory, startTime, elapsedTimeFormatted, distanceFormatted);
                            } else {
                                endTime = response.getJSONObject(response.length() - 1).getString("datetime");
                                double distance = calculateDistance(latLonHistory.get(0), lastPoint);
                                String distanceFormatted = String.format(Locale.getDefault(), "%.2f km", distance);
                                Date startDate = inputFormat.parse(startTime);
                                Date endDate = inputFormat.parse(endTime);
                                long elapsedMillis = endDate.getTime() - startDate.getTime();
                                String elapsedTimeFormatted = formatElapsedTime(elapsedMillis);
                                callback.onTripPointsFetched(latLonHistory, startTime, elapsedTimeFormatted, distanceFormatted);
                            }
                        } else {
                            callback.onTripPointsFetched(latLonHistory, startTime, "00h 00m 00s", "0.00 km");
                        }
                    } catch (JSONException | ParseException e) {
                        e.printStackTrace();
                        callback.onError(e.getMessage());
                    }
                },
                error -> {
                    Toast.makeText(context, "Failed to fetch trip points", Toast.LENGTH_SHORT).show();
                    callback.onError(error.getMessage());
                });

        requestQueue.add(jsonArrayRequest);
    }
    private String formatElapsedTime(long elapsedMillis) {
        long seconds = (elapsedMillis / 1000) % 60;
        long minutes = (elapsedMillis / (1000 * 60)) % 60;
        long hours = (elapsedMillis / (1000 * 60 * 60));
        return String.format(Locale.getDefault(), "%02dh %02dm %02ds", hours, minutes, seconds);
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
    public interface TripPointsCallback {
        void onTripPointsFetched(ArrayList<LatLng> latLonHistory, String startTime, String elapsedTime, String distance);
        void onError(String errorMessage);
    }
}