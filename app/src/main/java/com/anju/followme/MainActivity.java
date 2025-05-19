package com.anju.followme;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import com.anju.followme.api.TripIDCheck;
import com.anju.followme.api.UserRegistration;
import com.anju.followme.api.UserValidation;
import com.anju.followme.databinding.ActivityMainBinding;
import com.anju.followme.databinding.DialogFollowBinding;
import com.anju.followme.databinding.DialogLoginBinding;
import com.anju.followme.databinding.DialogRegisterBinding;
import com.anju.followme.databinding.GenerateidBinding;

import android.Manifest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private long startTime;
    private ActivityMainBinding binding;
    private static final long minSplashTime = 3000;
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String user_id;
    private String trip_id_follow;
    private int follow = 0;


    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 102;
    private boolean loggedIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.parseColor("#FFEEEEEE"));
        startTime = System.currentTimeMillis();
        SplashScreen.installSplashScreen(this)
                .setKeepOnScreenCondition(
                        new SplashScreen.KeepOnScreenCondition() {
                            @Override
                            public boolean shouldKeepOnScreen() {
                                Log.d("Main Activity", "shouldKeepOnScreen: " + (System.currentTimeMillis() - startTime));
                                return  (System.currentTimeMillis() - startTime <= minSplashTime);
                            }
                        }
                );
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.imageView3.setOnClickListener(v -> {
            if (!loggedIn) {
                showLoginDialog();
            } else {
                requestPermissions();
            }
        });
        binding.imageView4.setOnClickListener(v -> {
            follow = 1;
            if (!loggedIn) {
                showLoginDialog();
            } else {
                showFollowTripDialog();
            }
        });
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
                            Toast.makeText(MainActivity.this, "Network Available", Toast.LENGTH_SHORT).show();

                        });
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        super.onLost(network);
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Network Lost", Toast.LENGTH_SHORT).show();

                        });
                    }
                }
        );
    }

    private void showFollowTripDialog() {
        AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
        builder2.setIcon(R.drawable.ic_launcher);
        builder2.setTitle("Follow Me");
        builder2.setMessage("Enter the Trip ID to follow:");
        DialogFollowBinding dialogFollow = DialogFollowBinding.inflate(getLayoutInflater());
        builder2.setView(dialogFollow.getRoot());
        builder2.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder2.setPositiveButton("OK", null);
        AlertDialog dialog = builder2.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(p -> {
            checkTripID(dialogFollow.editTextText.getText().toString(), tripIDExists -> {
                if (tripIDExists) {
                    trip_id_follow = dialogFollow.editTextText.getText().toString();
                    Intent intent = new Intent(this, TripFollowActivity.class);
                    intent.putExtra("trip_id", trip_id_follow);
                    startActivity(intent);
                    dialog.dismiss();
                } else {
                    dialog.dismiss();
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                    builder1.setIcon(R.drawable.ic_launcher);
                    builder1.setTitle("Trip Not Found");
                    builder1.setMessage("The Trip ID '" + dialogFollow.editTextText.getText().toString() + "' was not found");
                    builder1.setNegativeButton("OK", (dialog2, which) -> dialog2.dismiss());
                    AlertDialog dialog2 = builder1.create();
                    dialog2.show();
                }
            });
        });
    }

    private void showTripIDCreation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle("Follow Me");
        builder.setMessage("Please provide Trip ID for this journey.\nShare this ID with your friends.");

        GenerateidBinding generateidBinding = GenerateidBinding.inflate(getLayoutInflater());
        builder.setView(generateidBinding.getRoot());

        builder.setNeutralButton("Generate", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.setPositiveButton("OK", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Handle "Generate" button
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(p -> generateNewTripId(generateidBinding));

        // Handle "OK" button
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(p -> {
            String id = generateidBinding.editTextText.getText().toString();
            String regex1 = "^[A-Z0-9]{1,100}$";
            String regex2 = "^[A-Z0-9]{5}-[A-Z0-9]{5}$";

            if (id.isEmpty()) {
                Toast.makeText(this, "ID cannot be empty", Toast.LENGTH_SHORT).show();
            } else if (!id.matches(regex1) && !id.matches(regex2)) {
                Toast.makeText(this, "ID must be 1-100 uppercase alphanumeric characters (A-Z, 0-9)", Toast.LENGTH_SHORT).show();
            } else {
                checkTripID(id, tripIDExists -> {
                    if (tripIDExists) {
                        Toast.makeText(this, "Please enter a new ID as this is already taken", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d("tripId", "Generated trip ID: " + id);

                        Intent intent = new Intent(this, TripLeadActivity.class);
                        intent.putExtra("trip_id", id);
                        intent.putExtra("user_id", user_id);
                        intent.putExtra("first_name", firstName);
                        intent.putExtra("last_name", lastName);
                        startActivity(intent);
                    }
                });
            }
            dialog.dismiss();
        });
    }

    // Define generateNewTripId outside showTripIDCreation
    private void generateNewTripId(GenerateidBinding generateidBinding) {
        String id = makeTripId();
        checkTripID(id, tripIDExists -> {
            if (!tripIDExists) {
                generateidBinding.editTextText.setText(id);
            } else {
                generateNewTripId(generateidBinding);  // Retry if the generated ID is taken
            }
        });
    }



    private void checkTripID(String id, TripCheckCallback callback) {
        TripIDCheck.checkTripID(this, id, callback);
    }

    public interface TripCheckCallback {
        void onResult(boolean exists);
    }

    private String makeTripId() {
        String ALLOWED_CHARACTERS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        sb.append("-");
        for (int i = 0; i < 5; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }

    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle("Follow Me");
        DialogLoginBinding dialogBinding = DialogLoginBinding.inflate(getLayoutInflater());
        builder.setView(dialogBinding.getRoot());
        SharedPreferences prefs1 = this.getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        username = prefs1.getString("username", null);
        password = prefs1.getString("password", null);
        if (username != null && password != null) {
            dialogBinding.etUsername.setText(username);
            dialogBinding.etPassword.setText(password);
        }
        builder.setNeutralButton("Register", (dialog, which) -> {
            dialog.dismiss();
            showRegisterDialog();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.setPositiveButton("Login", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            username = dialogBinding.etUsername.getText().toString();
            password = dialogBinding.etPassword.getText().toString();
            boolean saveCredentials = dialogBinding.checkSaveCredentials.isChecked();
            if (saveCredentials) {
                SharedPreferences prefs = this.getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("username", username);
                editor.putString("password", password);
                editor.apply();
            }
            validateusers(username, password, dialog);
        });
    }

    private void validateusers(String userName, String password, AlertDialog dialog) {
        if (!NetworkChecker.hasNetworkConnection(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Follow Me- No Network");
            builder.setMessage("No network connection. Cannot Login now.");
            builder.setNegativeButton("OK", (dialog1, which) -> dialog.dismiss());
            AlertDialog dialog1 = builder.create();
            dialog1.show();

            return;
        }
        UserValidation.validateUser(this, userName, password, dialog, new ValidationCallback() {
            @Override
            public void onSuccess(String result, String lastName, String user_id, String firstName) {
                if (result.equals("true")) {
                    loggedIn = true;
                    MainActivity.this.lastName = lastName;
                    MainActivity.this.user_id = user_id;
                    MainActivity.this.firstName = firstName;
                }
                requestPermissions();
            }

            @Override
            public void onError(String errorBody) {
                showLoginErrorDialog(errorBody);
            }
        });
    }

    public interface ValidationCallback {
        void onSuccess(String result, String lastName, String user_id, String firstName);
        void onError(String errorBody);
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE);
                } else {
                    if (!isGpsEnabled(this)) {
                        promptEnableGps(this);
                    } else {
                        if (follow == 0) {
                            showTripIDCreation();
                        } else {
                            showFollowTripDialog();
                        }
                    }
                }
            }
        }
    }

    public boolean isGpsEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void promptEnableGps(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("GPS is not enabled. Please enable GPS by turning on \"Use location\" in the location settings to continue")
                .setCancelable(false)
                .setPositiveButton("Go to Location Settings", (dialog, id) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
                }
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE);
                }
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!isGpsEnabled(this)) {
                    promptEnableGps(this);
                } else {
                    if (follow == 0) {
                        showTripIDCreation();
                    } else {
                        showFollowTripDialog();
                    }
                }
            } else {
                Toast.makeText(this, "Background location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showLoginErrorDialog(String errorBody) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle("Follow Me - Login Failed");
        builder.setMessage("Username or Password is incorrect. Please try again.");
        builder.setNegativeButton("OK", (dialog, which) -> showLoginDialog());
        builder.show();
    }

    private void showRegisterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle("Follow Me");
        DialogRegisterBinding registerBinding = DialogRegisterBinding.inflate(getLayoutInflater());
        builder.setView(registerBinding.getRoot());
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.setPositiveButton("Register", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String firstName = registerBinding.etFirstName.getText().toString();
            String lastName = registerBinding.etLastName.getText().toString();
            String email = registerBinding.etEmail.getText().toString();
            String userName = registerBinding.etRegisterUsername.getText().toString();
            String password = registerBinding.etRegisterPassword.getText().toString();
            if (userName.length() < 8 || userName.length() > 12) {
                registerBinding.etRegisterUsername.setError("Username must be between 8 and 12 characters");
            } else if (password.length() < 8 || password.length() > 12) {
                registerBinding.etRegisterPassword.setError("Password must be between 8 and 12 characters");
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                registerBinding.etEmail.setError("Enter a valid email address");
            } else if (firstName.length() < 1 || firstName.length() > 100) {
                registerBinding.etFirstName.setError("First Name must be between 1 and 100 characters");
            } else if (lastName.length() < 1 || lastName.length() > 100) {
                registerBinding.etLastName.setError("Last Name must be between 1 and 100 characters");
            } else {
                JSONObject userJson = new JSONObject();
                try {
                    userJson.put("firstName", firstName);
                    userJson.put("lastName", lastName);
                    userJson.put("userName", userName);
                    userJson.put("password", password);
                    userJson.put("email", email);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                createUserAccount(userJson, dialog);
            }
        });
    }

    private void createUserAccount(JSONObject userJson, AlertDialog dialog) {
        if (!NetworkChecker.hasNetworkConnection(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Follow Me- No Network");
            builder.setMessage("No network connection. Cannot Create User now");
            builder.setNegativeButton("OK", (dialog1, which) -> dialog1.dismiss());
            AlertDialog dialog1 = builder.create();
            dialog1.show();

            return;
        }
        UserRegistration.createUserAccount(this, userJson, dialog, new RegistrationCallback() {
            @Override
            public void onSuccess(String firstName, String lastName, String userName, String email) {
                showSuccessDialog(userName, email);
            }

            @Override
            public void onError(String errorBody) {
                showErrorDialog(errorBody);
            }
        });
    }

    public interface RegistrationCallback {
        void onSuccess(String firstName, String lastName, String userName, String email);
        void onError(String errorBody);
    }

    private void showSuccessDialog(String userName, String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle("Follow Me - Registration Successful");
        builder.setMessage("Welcome " + userName + "!\n\nYour username is: " + userName + "\nYour email is: " + email);
        builder.setNegativeButton("OK", (dialog, which) -> showLoginDialog());
        builder.show();
    }

    private void showErrorDialog(String errorBody) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle("Follow Me - Registration Failed");
        builder.setMessage(errorBody);
        builder.setNegativeButton("OK", (dialog, which) -> showRegisterDialog());
        builder.show();
    }
}