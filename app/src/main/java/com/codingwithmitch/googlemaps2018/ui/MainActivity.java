package com.codingwithmitch.googlemaps2018.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.codingwithmitch.googlemaps2018.R;
import com.codingwithmitch.googlemaps2018.UserClient;
import com.codingwithmitch.googlemaps2018.adapters.ChatroomRecyclerAdapter;
import com.codingwithmitch.googlemaps2018.models.Chatroom;
import com.codingwithmitch.googlemaps2018.models.User;
import com.codingwithmitch.googlemaps2018.models.UserLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import static com.codingwithmitch.googlemaps2018.Constants.ERROR_DIALOG_REQUEST;
import static com.codingwithmitch.googlemaps2018.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.codingwithmitch.googlemaps2018.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, ChatroomRecyclerAdapter.ChatroomRecyclerClickListener {

    private static final String TAG = "MainActivity";


    //widgets
    private ProgressBar mProgressBar;

    //vars
    private ArrayList<Chatroom> mChatrooms = new ArrayList<>();
    private Set<String> mChatroomIds = new HashSet<>();
    private ChatroomRecyclerAdapter mChatroomRecyclerAdapter;
    private RecyclerView mChatroomRecyclerView;
    private ListenerRegistration mChatroomEventListener;
    private FirebaseFirestore mDb;

    //BY DEFAULT THE PERMISSION IS RESTRICTED, IF THE USER GRANTS PERMISSION THEN OTHER FEATURES OF THE APP WILL BE AVAILABLE.
    private boolean mLocationPermissionGranted = false;

    //THIS VARIABLE IS USED AS A PART OF GOOGLE MAPS SDK FOR RETRIEVING LAST LOCATION OF THE DEVICE.
    private FusedLocationProviderClient mFusedLocationClient;

    //A PART OF STEP UserLocation.java, LINKING UserLocation.java TO MainActivity.java
    private UserLocation mUserLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProgressBar = findViewById(R.id.progressBar);
        mChatroomRecyclerView = findViewById(R.id.chatrooms_recycler_view);

        findViewById(R.id.fab_create_chatroom).setOnClickListener(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this); //object 2 retrieve the location.
        mDb = FirebaseFirestore.getInstance();

        initSupportActionBar();
        initChatroomRecyclerView();
    }

    //STEP 10: getUserDetails & then userLocation by calling getLastKnownLocation().
     /* this is a summary of getUserDetails method below.
     if(mUserLocation == null){
             //Retrieve the user details from firestore
             }
            else{
            //just get the location by calling getLastKnownLocation.
            }
      */
    private void getUserDetails(){
        if(mUserLocation==null){             //describes that its a new user going into.
            mUserLocation= new UserLocation();
            DocumentReference userRef= mDb.
                    collection(getString(R.string.collection_users)).   //is the collection name.
                    document(FirebaseAuth.getInstance().getUid());      //here document is identified by userID.
            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {       //get method is used as we r retrieving user details.
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        Log.d(TAG,"onComplete: successfully get the user details");
                        User user =task.getResult().toObject(User.class);
                        mUserLocation.setUser(user);
                        ((UserClient)(getApplicationContext())).setUser(user);
                        getLastKnownLocation();
                    }
                }
            });
        }
       else{
            getLastKnownLocation();
        }
    }

     //STEP 12:
    private void saveUserLocation(){
        if(mUserLocation != null){
            DocumentReference locationRef = mDb.
                    collection(getString(R.string.collection_user_locations)).     //or use, collection("User Locations") ,both r same & this is the collection name.
                    document(FirebaseAuth.getInstance().getUid());          //here document is identified by userID on firestore.
            locationRef.set(mUserLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        Log.d(TAG,"saveUserLocation: \ninserted user location into database" +
                                "\n latitude: " + mUserLocation.getGeo_Point().getLatitude() +
                                "\n longitude: " + mUserLocation.getGeo_Point().getLongitude());
                    }
                }
            });
        }
    }

     //STEP 11:
     // Lecture.5.RETRIEVING LAST LOCATION OF THE DEVICE, A PART OF GOOGLE MAPS SDK.
    private void getLastKnownLocation(){
        Log.d(TAG, "getLastKnownLocation: called ");
        //permission check not required here.
      /*  if(ActivityCompat.checkSelfPermission(this,Menifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            return;
        }   */
        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if(task.isSuccessful()){
                    Location location=task.getResult();
                    GeoPoint geoPoint =new GeoPoint(location.getLatitude(),location.getLongitude());
                    Log.d(TAG, "onComplete: latitude " + geoPoint.getLatitude());
                    Log.d(TAG, "onComplete: longitude " + geoPoint.getLongitude());

                    mUserLocation.setGeo_Point(geoPoint);
                    mUserLocation.setTimestamp(null);
                    saveUserLocation();             // calls STEP 12. to upload location into firestore.
                }
            }
        });
    }

   // STEP 2:
    private boolean checkMapServices() {
        if (isServicesOK()) {
            if (isMapsEnabled()) {
                return true;
            }
        }
        return false;
    }

    //STEP 5:
    //TO DISPLAY DIALOG BOX THEN REDIRECT TO PHONE LOCATION SETTINGS
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?").setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                //CHECKING IF USER ACCEPTED OR DENIED THE LOCATION PERMISSION AS HERE , PERMISSIONS_REQUEST_ENABLE_GPS.
                startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
            }
        });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    //STEP 4:
    //CHECKING IF GPS ENABLED OR NOT
    public boolean isMapsEnabled() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            //IF NOT ENABLED IT DISPLAY A DIALOG BOX TO ENABLE IT BY CALLING THE BELOW METHOD.
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    //STEP 7:
    private void getLocationPermission() {
        /* Request location permission, so that we can get the location of the
        device. The result of the permission request is handled by a callback,
        onRequestPermissionsResult.          */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            getChatrooms();
            getUserDetails();                  //5.
        }
        else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    //STEP 3:
    //CHECKING IF GOOGLE PLAY SERVICES IS INSTALLED IN THE DEVICE OR NOT
    public boolean isServicesOK() {
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);
        if (available == ConnectionResult.SUCCESS) {
              Log.d(TAG, "isServicesOK: Google Play Services is working");
              //IF GOOGLE PLAY SERVICES IS INSTALLED THE IT RETURNS TRUE
              return true;
        } else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            //IF NOT INSTALLED THEN THIS DIALOG PROMPTS TO USERS TO INSTALL GOOGLE PLAY SERVICES
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        //RETURNS FALSE IF GOOGLE PLAY SERVICES STILL NOT WORKING.
        return false;
    }

    //STEP 8:, STEP 9 CONTINUES IN UserListFragment.java
    @Override public void onRequestPermissionsResult ( int requestCode, @NonNull String permissions[], @NonNull int[] grantResults){
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
    }

    //STEP 6:
    @Override protected void onActivityResult ( int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: called.");

        //IT WILL CHECK GPS IS ENABLED OR NOT, IF ENABLED THEN IT WILL START "getChatrooms()" AND RETRIEVE LAST LOCATION OTHERWISE CALL THE "getLocationPermission()" TO ASK AGAIN FOR LOCATION PERMISSIONS.
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ENABLE_GPS: {
                if (mLocationPermissionGranted) {
                    getChatrooms();
                    getUserDetails();             //5.
                } else {
                    getLocationPermission();
                }
            }
        }
    }

    private void initSupportActionBar () {
        setTitle("Chatrooms");
    }


    @Override
    public void onClick (View view){
        switch (view.getId()) {

            case R.id.fab_create_chatroom: {
                newChatroomDialog();
            }
        }
    }

        private void initChatroomRecyclerView () {
            mChatroomRecyclerAdapter = new ChatroomRecyclerAdapter(mChatrooms, this);
            mChatroomRecyclerView.setAdapter(mChatroomRecyclerAdapter);
            mChatroomRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

        private void getChatrooms () {

            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setTimestampsInSnapshotsEnabled(true)
                    .build();
            mDb.setFirestoreSettings(settings);

            CollectionReference chatroomsCollection = mDb
                    .collection(getString(R.string.collection_chatrooms));

            mChatroomEventListener = chatroomsCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                    Log.d(TAG, "onEvent: called.");

                    if (e != null) {
                        Log.e(TAG, "onEvent: Listen failed.", e);
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                            Chatroom chatroom = doc.toObject(Chatroom.class);
                            if (!mChatroomIds.contains(chatroom.getChatroom_id())) {
                                mChatroomIds.add(chatroom.getChatroom_id());
                                mChatrooms.add(chatroom);
                            }
                        }
                        Log.d(TAG, "onEvent: number of chatrooms: " + mChatrooms.size());
                        mChatroomRecyclerAdapter.notifyDataSetChanged();
                    }

                }
            });
        }

        private void buildNewChatroom (String chatroomName){

            final Chatroom chatroom = new Chatroom();
            chatroom.setTitle(chatroomName);

            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setTimestampsInSnapshotsEnabled(true)
                    .build();
            mDb.setFirestoreSettings(settings);

            DocumentReference newChatroomRef = mDb
                    .collection(getString(R.string.collection_chatrooms))
                    .document();

            chatroom.setChatroom_id(newChatroomRef.getId());

            newChatroomRef.set(chatroom).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    hideDialog();

                    if (task.isSuccessful()) {
                        navChatroomActivity(chatroom);
                    } else {
                        View parentLayout = findViewById(android.R.id.content);
                        Snackbar.make(parentLayout, "Something went wrong.", Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
        }

        private void navChatroomActivity (Chatroom chatroom){
            Intent intent = new Intent(MainActivity.this, ChatroomActivity.class);
            intent.putExtra(getString(R.string.intent_chatroom), chatroom);
            startActivity(intent);
        }

        private void newChatroomDialog () {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enter a chatroom name");

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            builder.setPositiveButton("CREATE", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (!input.getText().toString().equals("")) {
                        buildNewChatroom(input.getText().toString());
                    } else {
                        Toast.makeText(MainActivity.this, "Enter a chatroom name", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        }

        @Override
        protected void onDestroy () {
            super.onDestroy();
            if (mChatroomEventListener != null) {
                mChatroomEventListener.remove();
            }
        }
   // STEP 1: THIS IS A LOOP.
        @Override
        protected void onResume () {
            super.onResume();
          //  getChatrooms();
            if(checkMapServices()){
                if(mLocationPermissionGranted){
                    getChatrooms();
                    getUserDetails();               //5.
                }
                    else{
                        getLocationPermission();
                    }
            }
        }

        @Override
        public void onChatroomSelected ( int position){
            navChatroomActivity(mChatrooms.get(position));
        }

        private void signOut () {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        @Override
        public boolean onCreateOptionsMenu (Menu menu){
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return super.onCreateOptionsMenu(menu);
        }


        @Override
        public boolean onOptionsItemSelected (MenuItem item){
            switch (item.getItemId()) {
                case R.id.action_sign_out: {
                    signOut();
                    return true;
                }
                case R.id.action_profile: {
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
                }
                default: {
                    return super.onOptionsItemSelected(item);
                }
            }

        }

        private void showDialog () {
            mProgressBar.setVisibility(View.VISIBLE);
        }

        private void hideDialog () {
            mProgressBar.setVisibility(View.GONE);
        }


    }
