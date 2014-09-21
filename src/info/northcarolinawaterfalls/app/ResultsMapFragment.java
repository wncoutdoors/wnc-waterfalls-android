/*
 * Copyright 2014 WNCOutdoors.info
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * ResultsMapFragment.java
 * Fragment which displays the results of a user's query on a map.
 */
package info.northcarolinawaterfalls.app;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.commonsware.cwac.loaderex.acl.SQLiteCursorLoader;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import info.northcarolinawaterfalls.app.ResultsListFragment.OnWaterfallQueryListener;
import info.northcarolinawaterfalls.app.ResultsListFragment.OnWaterfallSelectListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ResultsMapFragment extends SherlockFragment implements
            LoaderManager.LoaderCallbacks<Cursor>, OnInfoWindowClickListener {
    
    private static final String TAG = "ResultsListFragment";
    private MapView mMapView;
    private OnWaterfallQueryListener sQueryListener; // Listener for loader callbacks
    private OnWaterfallSelectListener sSelectListener; // Listener for user waterfall selections
    private OnLocationQueryListener sLocationQueryListener; // Listener for location searches
    
    private boolean mLocationDetermined = false;
    private boolean mActivityCreated = false;

    private static AttrDatabase db = null;
    private SQLiteCursorLoader cursorLoader = null;
    
    // Oh this makes me sad
    private Map<Marker, Long> mMarkersToIds = new HashMap<Marker, Long>();
    private boolean mMapReady;
    private LatLngBounds mResultBounds;
    
    public interface OnLocationQueryListener{
        public void determineLocation(Fragment requestor);
        public void stopLocationClient();
        public short getSearchLocationDistance();
        public Address getOriginAddress();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivityCreated = true;
        initializeLoaderIfReady();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // Make sure the containing activity implements the search listener interface
        try {
            sQueryListener = (OnWaterfallQueryListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnWaterfallQueryListener");
        }

        // And the select listener interface
        try {
            sSelectListener = (OnWaterfallSelectListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnWaterfallSelectListener");
        }
        
        // And the interfaces for obtaining locations from a LocationClient
        try {
            sLocationQueryListener = (OnLocationQueryListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnQueryLocationListener");
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);       
        db = new AttrDatabase(getActivity());
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        Log.d(TAG, "Inside ResultsMapFragment onCreateView");
        View view = inflater.inflate(R.layout.fragment_information_map, container, false);
        mMapView = (MapView) view.findViewById(R.id.information_map_view);
        mMapView.onCreate(savedInstanceState);       
        MapsInitializer.initialize(getActivity()); // TODO: Check returned error code?
        return view;
    }

    // Called when the Activity becomes visible.
    @Override
    public void onStart() {
        mMapReady = false;
        Log.d(TAG, "Inside ResultsMapFragment onStart");
        super.onStart();
        // Will invoke onLocationDetermined when complete.
    }

    // Route certain fragment lifecycle events to MapView
    @Override
    public void onResume() {
        Log.d(TAG, "Inside ResultsMapFragment onResume");
        super.onResume();
        if (null != mMapView){
            mMapView.onResume();
        }

        GoogleMap googleMap = mMapView.getMap();
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Initialize to terrain map; user can switch.
        googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        
        // Set map to run our callback when info window clicked
        googleMap.setOnInfoWindowClickListener(this);
        
        sLocationQueryListener.determineLocation((Fragment) this);
        
        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                mMapReady = true;
                zoomToBounds();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (null != mMapView){
            mMapView.onPause();
        }
    }

    // Called when the Activity is no longer visible.
    @Override
    public void onStop() {
        sLocationQueryListener.stopLocationClient();
        super.onStop();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(null != mMapView){
            mMapView.onDestroy();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != mMapView){
            mMapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (null != mMapView){
            mMapView.onLowMemory();
        }
    }
    
    // Async location determination callback method
    public void onLocationDetermined(){
        // Get our loader manager, and initialize the
        // query based on the containing Activity's searchMode
        // TODO: Hide the overlay.
        Log.d(TAG, "onLocationDetermined finished and loader manager initialized.");
        mLocationDetermined = true;
        initializeLoaderIfReady();
    }
    
    // If we have a location and a fully-realized activity, initialize the loader.
    // Prevents race between onLocationDetermined & onActivityCreated.
    private void initializeLoaderIfReady(){
        if(mLocationDetermined && mActivityCreated){
            getLoaderManager().initLoader(0, null, this);
        }
    }

    // LoaderManager.LoaderCallbacks<Cursor> methods
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "inside onCreateLoader.");
        Bundle qBundle = sQueryListener.onWaterfallQuery();
        
        // Get the query from our parent activity and pass it to the loader, which will execute it
        cursorLoader = new SQLiteCursorLoader(
                getActivity(), db, qBundle.getString("query"), qBundle.getStringArray("args"));
        Log.d(TAG, "We have created a cursorLoader.");
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        int count = cursor.getCount();
        Log.d(TAG, "Cursor returned " + count + " rows.");
        if(count == 0) {
            Log.d(TAG, "Let's have a toast.");
            Context context = getActivity();
            CharSequence text = "No results found for your search.";
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show(); 
        } else {
            // Put the results on a map!
            GoogleMap googleMap = mMapView.getMap();
            double searchLocationDistanceM = 0;
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            if(cursor.moveToFirst()){
                // First, add the "searched for" location, if it was a location search.
                Location originLocation = new Location("");
                Address originAddress = sLocationQueryListener.getOriginAddress();
                if(originAddress != null){
                    // Get searched-for distance and convert to meters.
                    short searchLocationDistance = sLocationQueryListener.getSearchLocationDistance();
                    searchLocationDistanceM =  searchLocationDistance * 1609.34;
                    
                    // Build up a list of Address1, Address2, Address3, if present.
                    ArrayList<String> addressList = new ArrayList<String>(); 
                    for(int i=0; i<=3; i++){
                        String line = originAddress.getAddressLine(i);
                        if(line != null && line.length() > 0){
                            addressList.add(line);
                        }
                    }
                    
                    String addressDesc = TextUtils.join("\n", addressList);
                    if(addressDesc == ""){
                        addressDesc = originAddress.getFeatureName();
                    }
                    if(addressDesc == ""){
                        addressDesc = "Searched Location";
                    }
                    
                    // Create the LatLng and the map marker.
                    LatLng originLatLng = new LatLng(
                            originAddress.getLatitude(), originAddress.getLongitude());
                    googleMap.addMarker(new MarkerOptions()
                        .position(originLatLng)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        .title(addressDesc));
                    
                    boundsBuilder.include(originLatLng);  // In case only one result :)
                    
                    // Translate into a Location for distance comparison.
                    originLocation.setLatitude(originAddress.getLatitude());
                    originLocation.setLongitude(originAddress.getLongitude());
                } else {
                    // Not a location search; don't add point for searched-for location
                    // and don't check radius from that point.
                    Log.d(TAG, "Skipped adding origin address to map.");
                }
                
                // Next, add the results waterfalls.
                // Use do...while since we're already at the first result.
                do{
                    // Get some data from the db
                    Long waterfallId = cursor.getLong(AttrDatabase.COLUMNS.indexOf("_id"));
                    double lat = cursor.getDouble(AttrDatabase.COLUMNS.indexOf("geo_lat"));
                    double lon = cursor.getDouble(AttrDatabase.COLUMNS.indexOf("geo_lon"));
                    
                    // Make sure this one's actually within our search radius. SQL only checked
                    // the bounding box.
                    Location waterfallLocation = new Location("");
                    waterfallLocation.setLatitude(lat);
                    waterfallLocation.setLongitude(lon);
                    
                    if(originAddress == null || 
                        (originLocation.distanceTo(waterfallLocation) <= searchLocationDistanceM)){
                        // Not a location search (originAddress is null: show all) or within radius.
                        // Display on map.
                        String name = cursor.getString(AttrDatabase.COLUMNS.indexOf("name"));
                        
                        LatLng waterfallLatLng = new LatLng(lat, lon);
                        Marker waterfallMarker = googleMap.addMarker(new MarkerOptions()
                            .position(waterfallLatLng)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            .title(name));
                        
                        // Save the id so we can retrieve it when clicked
                        mMarkersToIds.put(waterfallMarker, waterfallId);
                        boundsBuilder.include(waterfallLatLng);
                    }

                } while(cursor.moveToNext());
                
                // Zoom and center the map to bounds
                mResultBounds = boundsBuilder.build();
                zoomToBounds();
            }
        }
    }

    private void zoomToBounds(){
        if(mMapReady && mResultBounds != null){
            GoogleMap googleMap = mMapView.getMap();
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mResultBounds, 15));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // mAdapter.changeCursor(cursor);
    }

    // Marker info window click method
    @Override
    public void onInfoWindowClick(Marker marker) {
        long waterfallId = mMarkersToIds.get(marker);
        sSelectListener.onWaterfallSelected(waterfallId);
    }
}
