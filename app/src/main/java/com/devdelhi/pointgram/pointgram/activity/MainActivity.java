package com.devdelhi.pointgram.pointgram.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.devdelhi.pointgram.pointgram.R;
import com.devdelhi.pointgram.pointgram.services.Service_GPS;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

public class MainActivity extends AppCompatActivity {

    private LinearLayout mapCard;
    private FirebaseAuth mAuth;
    private TextView tv;
    private Double lat,lng, alt;
    private Float speed,acc;
    private String provider;
    private BroadcastReceiver broadcastReceiver;
    private String TAG = "DEEJAY";
    private android.support.v7.widget.Toolbar mToolbar;
    private ViewPager mViewPager;
    private SectionsPagerAdapter mSectionPagerAdapter;
    private TabLayout mTabLayout;
    private DatabaseReference muserRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity__main);

        if (isUserSignedIn()) {
            mAuth = FirebaseAuth.getInstance();
            muserRef = FirebaseDatabase.getInstance().getReference().child("users_database").child(mAuth.getCurrentUser().getUid());

            mToolbar = findViewById(R.id.main_page_toolbar);
            setSupportActionBar(mToolbar);
            getSupportActionBar().setTitle("Point Gram");

            //TABS

            mViewPager = findViewById(R.id.view_pager);

            mTabLayout = findViewById(R.id.main_page_tabs);

            mSectionPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

            mViewPager.setAdapter(mSectionPagerAdapter);
            mTabLayout.setupWithViewPager(mViewPager);
        }
        else {
            startActivity(new Intent(MainActivity.this, Activity_Start.class));
            finish();
        }
    }

    private void askForPermission(String permission, Integer requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);

            } else {

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
            }
        } else {
            Toast.makeText(this, "" + permission + " is already granted.", Toast.LENGTH_SHORT).show();
        }
    }


    private boolean isUserSignedIn() {
        boolean isSignedin = false;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            isSignedin = true;
        }

        return isSignedin;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    Log.d(TAG, "Broadcast Received");

                    //2.Attach Data
                    lat = intent.getDoubleExtra("lat", 0);
                    lng = intent.getDoubleExtra("lng", 0);
                    alt = intent.getDoubleExtra("alt", 0);
                    speed = intent.getFloatExtra("speed", 0);
                    acc = intent.getFloatExtra("acc", 0);
                    provider = intent.getStringExtra("provider");

                    Log.d(TAG, "Data : " + lat + ", " + lng + " " + speed);
                }
            };
        }
        registerReceiver(broadcastReceiver, new IntentFilter("User_Update")); // Calls onReceive Method
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }

        if (isUserSignedIn()) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                muserRef.child("online").setValue(ServerValue.TIMESTAMP);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseUser mCurrentUser = mAuth.getCurrentUser();
        if (mCurrentUser == null) {
            startActivity(new Intent(MainActivity.this, Activity_Start.class));
        }
        else {
            muserRef.child("online").setValue("true");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    private boolean checkRunTimePermissions() {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    100);
            return  true;
        }
        return false; // We don't need to ask for Permissions
    }

    public void startLocationService() {
        Intent intent = new Intent(getApplicationContext(), Service_GPS.class);
        startService(intent);
    }

    public void stopLocationService() {
        Intent intent = new Intent(getApplicationContext(), Service_GPS.class);
        stopService(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

            }
            else {
                checkRunTimePermissions();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.menu_items, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if (item.getItemId() == R.id.main_logout_btn) {
            logoutUser();
            startActivity(new Intent(MainActivity.this, Activity_Start.class));
        }
        else if (item.getItemId() == R.id.account_settings) {
            startActivity(new Intent(MainActivity.this, Activity_Account_Settings.class));
        }
        else if (item.getItemId() == R.id.all_users) {
            startActivity(new Intent(MainActivity.this, Activity_All_Users.class));
        }
        else if (item.getItemId() == R.id.friends_map) {
            startActivity(new Intent(MainActivity.this, Activity_Friends_Maps.class));
        }
        return true;
    }


    private void logoutUser() {

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            muserRef.child("online").setValue(ServerValue.TIMESTAMP);
        }
        FirebaseAuth.getInstance().signOut();
    }

}
