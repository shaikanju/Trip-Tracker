package com.anju.followme.api;

import android.content.Context;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.anju.followme.MainActivity;
import org.json.JSONException;
import org.json.JSONObject;

public class UserRegistration {
    public static void createUserAccount(Context context, JSONObject userJson, AlertDialog dialog, MainActivity.RegistrationCallback callback) {
        String url = "http://christopherhield-001-site4.htempurl.com/api/UserAccounts/CreateUserAccount";
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, userJson,
                response -> {
                    try {
                        String firstName = response.getString("firstName");
                        String lastName = response.getString("lastName");
                        String userName = response.getString("userName");
                        String email = response.getString("email");
                        callback.onSuccess(firstName, lastName, userName, email);
                        dialog.dismiss();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    Log.e("APIError", "User creation failed: " + error.getMessage());
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String errorBody = new String(error.networkResponse.data);
                        callback.onError(errorBody);
                        Log.e("APIError", "Status Code: " + statusCode);
                        Log.e("APIError", "Error Body: " + errorBody);
                    } else {
                        Log.e("APIError", "Network response is null (Check Internet or API URL).");
                    }
                });
        requestQueue.add(jsonObjectRequest);
    }
}