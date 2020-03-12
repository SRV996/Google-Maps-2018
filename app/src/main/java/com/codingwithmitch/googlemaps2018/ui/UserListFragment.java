package com.codingwithmitch.googlemaps2018.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import com.codingwithmitch.googlemaps2018.R;
import com.codingwithmitch.googlemaps2018.adapters.UserRecyclerAdapter;
import com.codingwithmitch.googlemaps2018.models.User;
import com.codingwithmitch.googlemaps2018.models.UserLocation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

import static com.codingwithmitch.googlemaps2018.Constants.MAPVIEW_BUNDLE_KEY;

/*
The basic steps for adding a map are:
(You only need to do this step once.) Follow the steps in the project configuration guide to get the API,
*obtain a key and add the required attributes to your Android manifest.
*Add a Fragment object to the Activity that will handle the map.
*The easiest way to do this is to add a <fragment> element to the layout file for the Activity.
*Implement the OnMapReadyCallback interface and use the onMapReady(GoogleMap) callback method to get a handle to the GoogleMap object.
*The GoogleMap object is the internal representation of the map itself. To set the view options for a map, you modify its GoogleMap object.
*Call getMapAsync() on the fragment to register the callback.
*/

public class UserListFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "UserListFragment";

    //widget
    private RecyclerView mUserListRecyclerView;
    private MapView mMapView;                                 // TAKEN FROM https://developers.google.com/maps/documentation/android-sdk/map#mapfragment > MAPVIEW > A SAMPLE.

    //vars
    private ArrayList<User> mUserList = new ArrayList<>();
    private ArrayList<UserLocation> mUserLocation = new ArrayList<>();   //list of userlocation object.@lec.9.
    private UserRecyclerAdapter mUserRecyclerAdapter;
    private GoogleMap mGoogleMap;                             // @lec.10.for setting the cameraview.
    private LatLngBounds mMapBoundary;                       //set the boundary for the camera view.
    private UserLocation mUserPosition;                      //position of a authenticated users. & we need method to set this.


    public static UserListFragment newInstance(){
        return new UserListFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
          final ArrayList<User> users = getArguments().getParcelableArrayList(getString(R.string.intent_user_list));
           mUserList.addAll(users);
         final ArrayList<UserLocation> locations= getArguments().getParcelableArrayList(getString(R.string.intent_user_locations));    //
           mUserLocation.addAll(locations);
            //will give the list of users location who is in the chatroom.
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view  = inflater.inflate(R.layout.fragment_user_list, container, false);
        mUserListRecyclerView = view.findViewById(R.id.user_list_recycler_view);
        mMapView = view.findViewById(R.id.user_list_map);           // A PART OF STEP 9: TAKEN FROM https://developers.google.com/maps/documentation/android-sdk/map#mapfragment > MAPVIEW > A SAMPLE.

        initUserListRecyclerView();
        initGoogleMap(savedInstanceState);
       // display the username & his geopoint in log for just debugging purpose. Its not required.
        for(UserLocation userLocation : mUserLocation){
            Log.d(TAG,"onCreateView: user location : " + userLocation.getUser().getUsername());
            Log.d(TAG,"onCreateView: geopoint : " + userLocation.getGeo_Point().getLatitude() + " , "
            + userLocation.getGeo_Point().getLongitude());  }

        setUserPosition();  //hence,we have userposition & reference to the googlemap. Now we need to set the cameraview.

        return view;
    }
    // to set the camera view

    private void setCameraView(){
        // 1st set the boundary.Here bottom&top , left&right boundary r same.Here GPS coordinate is .2 from each other.
        //overall area of map view window: .2 * .2 = .04
        double bottomBoundary = mUserPosition.getGeo_Point().getLatitude() - .1;
        double leftBoundary = mUserPosition.getGeo_Point().getLongitude() - .1;
        double topBoundary = mUserPosition.getGeo_Point().getLatitude() + .1;
        double rightBoundary = mUserPosition.getGeo_Point().getLongitude() + .1;
         mMapBoundary = new LatLngBounds(
                 new LatLng(bottomBoundary,leftBoundary),
                 new LatLng(topBoundary,rightBoundary)
         );
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mMapBoundary,0));   //with zero zoom level.

    }
  // setUserPosition method for the authenticated users.
    private void setUserPosition(){
        for(UserLocation userLocation: mUserLocation){
            if(userLocation.getUser().getUser_id().equals(FirebaseAuth.getInstance().getUid())){
                mUserPosition = userLocation;
            }
        }
    }

    // A PART OF STEP 9: TAKEN FROM https://developers.google.com/maps/documentation/android-sdk/map#mapfragment > MAPVIEW > A SAMPLE.
    private void initGoogleMap(Bundle savedInstanceState){
        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        mMapView.onCreate(mapViewBundle);

        mMapView.getMapAsync(this);
    }

    private void initUserListRecyclerView(){
        mUserRecyclerAdapter = new UserRecyclerAdapter(mUserList);
        mUserListRecyclerView.setAdapter(mUserRecyclerAdapter);
        mUserListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    //STEP 9:
    //*********************************************************************************************************************************************
    // TAKEN FROM https://developers.google.com/maps/documentation/android-sdk/map#mapfragment > MAPVIEW > A SAMPLE.
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mMapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        map.addMarker(new MarkerOptions().position(new LatLng(0,0)).title("Marker"));             // MAP MARKER ON MAP.

     /* PERMISSION CHECK WHICH MUST BE ADDED EXPLICITLY IN OLD VERSION IS NOT REQUIRED HERE.
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            return;
        }            */
        map.setMyLocationEnabled(true);     //THIS IS MY LOCATION ON MAP.
        mGoogleMap = map;                // reference to a googlemap that being setuo using the onMapReady method.& now set userposition.
        setCameraView();                 // call.
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }


}

 /*
* MapView, a subclass of the Android View class, allows you to place a map in an Android View.
  A View represents a rectangular region of the screen, and is a fundamental building block for Android applications and widgets.
  Much like a MapFragment, the MapView acts as a container for the map, exposing core map functionality through the GoogleMap object.
* When using the API in fully interactive mode,
 users of the MapView class must forward the following activity lifecycle methods to the corresponding methods in the
 MapView class: onCreate(), onStart(), onResume(), onPause(), onStop(), onDestroy(), onSaveInstanceState(), and onLowMemory().
 */

















