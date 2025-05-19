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

public class UserValidation {
    public static void validateUser(Context context, String userName, String password, AlertDialog dialog, MainActivity.ValidationCallback callback) {
        String url = "http://christopherhield-001-site4.htempurl.com/api/UserAccounts/VerifyUserCredentials";
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JSONObject userJson = new JSONObject();
        try {
            userJson.put("userName", userName);
            userJson.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PUT, url, userJson,
                response -> {
                    try {
                        String result = response.getString("result");
                        String lastName = response.getString("lastName");
                        String user_id = response.getString("userName");
                        String firstName = response.getString("firstName");
                        callback.onSuccess(result, lastName, user_id, firstName);
                        dialog.dismiss();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    Log.e("APIError", "User validation failed: " + error.getMessage());
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