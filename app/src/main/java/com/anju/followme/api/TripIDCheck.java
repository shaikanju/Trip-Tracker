package com.anju.followme.api;

import android.content.Context;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.anju.followme.MainActivity;

public class TripIDCheck {
    public static void checkTripID(Context context, String id, MainActivity.TripCheckCallback callback) {
        String url = "http://christopherhield-001-site4.htempurl.com/api/Datapoints/TripExists/" + id;
        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    boolean exists = Boolean.parseBoolean(response.trim());
                    Log.d("exist", "exists" + exists);
                    callback.onResult(exists);
                },
                error -> {
                    Log.e("VolleyError", "Error fetching trip ID: " + error.toString());
                    callback.onResult(false);
                });
        queue.add(stringRequest);
    }
}