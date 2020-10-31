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
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.barbuds.fragment.MyAccountFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView bottomNavigation = findViewById(R.id.navigation);

        BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener =new BottomNavigationView.OnNavigationItemSelectedListener(){

            /**
             * Called when an item in the bottom navigation menu is selected.
             *
             * @param item The selected item
             * @return true to display the item as the selected item and false if the item should not be
             * selected. Consider setting non-selectable items as disabled preemptively to make them
             * appear non-interactive.
             */
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_account:
                        final Fragment fragment1 = new MyAccountFragment();
                        final FragmentManager fm = getSupportFragmentManager();
                        fm.beginTransaction().add(R.id.fragment_layout,fragment1,"1").commit();
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if(!isLocationServiceRunning()){
                Intent intent = new Intent(MainActivity.this, LocationHelper.class);
                intent.setAction(Constants.ACTION_START_LOCATION_SERVICE);
                startService(intent);
                Toast.makeText(this,"Location service started",Toast.LENGTH_SHORT).show();
            }

            LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(
                    nearbyUsersReceiver, new IntentFilter("nearbyUsers"));

            CollectionReference collectionRef = FirebaseFirestore.getInstance().collection("Users");
            geoFirestore = new GeoFirestore(collectionRef);
        } else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(nearbyUsersReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

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

    private BroadcastReceiver nearbyUsersReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getBundleExtra("location");
            Location tempLocation = (Location) b.getParcelable("location");

            if(tempLocation != null) {
                geoFirestore.getAtLocation(new GeoPoint(tempLocation.getLatitude(), tempLocation.getLongitude()), 5, new GeoFirestore.SingleGeoQueryDataEventCallback() {
                    @Override
                    public void onComplete(List<? extends DocumentSnapshot> list, Exception e) {
                        if(e != null) {
                            Log.e(TAG, "onError: " + e);
                        }
                        else {
                            Log.d(TAG, list.toString());

                            //this is what where we will display nearby users
                        }
                    }
                });

            }
        }
    };
}