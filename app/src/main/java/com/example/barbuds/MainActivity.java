package com.example.barbuds;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barbuds.fragment.MyAccountFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import org.imperiumlabs.geofirestore.GeoFirestore;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    int LOCATION_REQUEST_CODE = 13;

    private GeoFirestore geoFirestore;
    private RecyclerView favoritesRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        favoritesRecyclerView = findViewById(R.id.nearby_users_recycler_view);

        // create bottom navigation bar and set up what it will do
        BottomNavigationView bottomNavigation = findViewById(R.id.navigation);
        BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener(){

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_account:
                        final Fragment fragment1 = new MyAccountFragment();
                        final FragmentManager fm = getSupportFragmentManager();
                        fm.beginTransaction().add(R.id.fragment_layout, fragment1,"1").commit();
                       // startActivity(new Intent(MainActivity.this, MainActivity.class));
                        return true;
                    case R.id.action_people:
                        startActivity(new Intent(MainActivity.this, MainActivity.class));
                        return true;
                    }
                    return false;
            }
        };
        bottomNavigation.setOnNavigationItemSelectedListener(navigationItemSelectedListener);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // if location services isn't allowed ask for permission otherwise get nearby users and send current location to database
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if(!isLocationServiceRunning()){
                Intent intent = new Intent(MainActivity.this, LocationHelper.class);
                intent.setAction(Constants.ACTION_START_LOCATION_SERVICE);
                startService(intent);
            }

            LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(
                    nearbyUsersReceiver, new IntentFilter("nearbyUsers"));

            // sets up reference to the "Users" collection to use for geo-querying
            geoFirestore = new GeoFirestore(FirebaseFirestore.getInstance().collection("Users"));
        } else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
    }

    // will stop the nearbyUsersReceiver when moving away from the activity
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(nearbyUsersReceiver);
    }


    // asks for permission to use location services
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == LOCATION_REQUEST_CODE) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission granted
                onStart();
            }
            else {
                // permission not granted
            }
        }
    }

    // checks to see if location services is already running
    private boolean isLocationServiceRunning() {
        ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (LocationHelper.class.getName().equals(service.service.getClassName())) {
                    if (service.foreground) {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    // create BroadcastReceiver that will receive locations from LocationHelper.locationCallback method
    private BroadcastReceiver nearbyUsersReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getBundleExtra("location");
            Location tempLocation = (Location) b.getParcelable("location");

            if(tempLocation != null) {
                geoFirestore.getAtLocation(new GeoPoint(tempLocation.getLatitude(), tempLocation.getLongitude()), 5000, new GeoFirestore.SingleGeoQueryDataEventCallback() {
                    @Override
                    public void onComplete(List<? extends DocumentSnapshot> list, Exception e) {
                        if(e != null) {
                            Log.e(TAG, "onError: " + e);
                        }
                        else {
                            favoritesRecyclerView.setLayoutManager(new LinearLayoutManager((MainActivity.this)));
                            favoritesRecyclerView.setAdapter(new MainActivityAdapter(MainActivity.this, list));
                        }
                    }
                });
            }
        }
    };
}