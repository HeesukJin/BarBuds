package com.example.barbuds;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import org.imperiumlabs.geofirestore.GeoFirestore;

import java.util.HashMap;
import java.util.Map;

import static java.security.AccessController.getContext;

public class LocationHelper extends Service {

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;

    private String uid;
    private FirebaseFirestore db;
    private GeoFirestore geoFirestore;

    private static final String TAG = "LocationHelper";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            String action = intent.getAction();
            if(action != null) {
                if(action.equals(Constants.ACTION_START_LOCATION_SERVICE)){
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    db = FirebaseFirestore.getInstance();

                    if (user != null) {
                        uid = user.getUid();
                    }

                    CollectionReference collectionRef = FirebaseFirestore.getInstance().collection("Users");
                    geoFirestore = new GeoFirestore(collectionRef);

                    checkSettingsAndStartLocationUpdates();
                }
                else if(action.equals(Constants.ACTION_STOP_LOCATION_SERVICE)){

                    stopForeground(true);
                    stopSelfResult(startId);
                    fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                }
            }
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if(locationResult == null) {
                Log.d(TAG, "onLocationResult: " + "no location");
            }
            else {
                for(Location location: locationResult.getLocations()) {
                    geoFirestore.setLocation(uid, new GeoPoint(location.getLatitude(), location.getLongitude()));
                    Log.d(TAG, "onLocationResult: " + location.toString());

                    Intent nearbyUsers = new Intent("nearbyUsers");

                    Bundle b = new Bundle();
                    b.putParcelable("location", location);

                    nearbyUsers.putExtra("location", b);
                    LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(nearbyUsers);
                }
            }
        }
    };

    private void checkSettingsAndStartLocationUpdates() {
        LocationSettingsRequest request = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest).build();

        SettingsClient client = LocationServices.getSettingsClient(this);

        Task<LocationSettingsResponse> locationSettingsResponseTask = client.checkLocationSettings(request);

        locationSettingsResponseTask.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                startLocationUpdates();
            }
        });

        locationSettingsResponseTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    ResolvableApiException apiException = (ResolvableApiException) e;

                    try {
                        // just needed a unique int
                        apiException.startResolutionForResult((Activity) getApplicationContext(), 1000);
                    } catch (IntentSender.SendIntentException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    private void startLocationUpdates() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent();
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new Notification.Builder(this, "location_notification_channel")
                        .setContentTitle("Location Service")
                        .setContentText("Running")
                        .setSmallIcon(R.drawable.logo)
                        .setContentIntent(pendingIntent)
                        .build();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            if(notificationManager != null
                    && notificationManager.getNotificationChannel("location_notification_channel")== null){
                NotificationChannel notificationChannel = new NotificationChannel(
                        "location_notification_channel",
                        "Location Service",
                        NotificationManager.IMPORTANCE_HIGH
                );
                notificationChannel.setDescription(("This channel is used by location service"));
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        // Notification ID cannot be 0.
        startForeground(Constants.LOCATION_SERVICE_ID, notification);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = locationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }
}