/*
 * Copyright 2014 WNCOutdoors.info
 * portions Copyright 2014 The Android Open Source Project
 * portions Copyright 2014 Google Inc.
 * portions Copyright 2012 StackOverflow user breceivemail
 * http://stackoverflow.com/questions/3695224/android-sqlite-getting-nearest-locations-with-latitude-and-longitude
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
 * AppInfoSettingsFragment.java
 * Fragment which controls the Settings tab under the App Info activity,
 * and provides information about the status of the Google Play Services
 * library on the user's device, as well as the status of offline maps
 * expansion file, and ability to download that if not completed yet.
 */
package info.wncwaterfalls.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import info.wncwaterfalls.app.ResultsListFragment.OnWaterfallQueryListener;
import info.wncwaterfalls.app.ResultsListFragment.OnWaterfallSelectListener;
import info.wncwaterfalls.app.ResultsMapFragment.OnLocationQueryListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ResultsActivity extends ActionBarActivity implements 
        OnWaterfallQueryListener, OnWaterfallSelectListener, OnLocationQueryListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener {
    
    public static final String PREFS_NAME = "AppSettingsPreferences";
    private static final String USER_PREF_SKIP_PLAY_SERVICES = "UserPrefSkipPlayServices";
    
    private static final String TAG = "ResultsActivity";
    private ActionBar actionBar;
    private boolean showListTab = true;
    
    private Short mSearchMode; // 1: waterfall, 2: hike, 3: location
    
    private String searchTerm;
    
    private short searchTrailLength;
    private short searchTrailDifficulty;
    private short searchTrailClimb;
    
    private Short searchLocationDistance;
    private String searchLocationRelto;
    private String searchLocationReltoTxt;
    private Boolean searchOnlyShared;
    
    private boolean mFoundOrigin = false;
    private Address mOriginAddress;
    private Location mOriginLocation;
    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;
    private int mNumUpdates = 0;
    
    private Fragment mLocationRequestor;

    public static final String SELECTED_WATERFALL_ID = "info.wncwaterfalls.app.SELECTED_WATERFALL_ID";   
    // Called by Location Services when the request to connect the
    // client finishes successfully. This will happen when the user
    // has requested Current Location as the search point.
    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        
        /* Get our location
         * could use:
         * mOriginLocation = mLocationClient.getLastLocation();
         * However, with that it's a pain to wait and try again if the returned location is stale/inaccurate
         * Instead: subscribe to a few 1-5 second location updates.
         * Updates will be turned off once an accurate-ish location is determined, on the assumption
         * that subsequent updates will be more accurate (esp. with GPS).
         * Only caveat to this is timing it out if we don't get any updates at all.
         */

        mLocationRequest = LocationRequest.create();
        
        // Use a balanced accuracy, approximately block level
        // This is fine for basing a query with a radius measured in miles
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        
        // Get updates every 5 seconds, but no more than once per second.
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(1000);
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
        
        // TODO: In rare instances, onLocationChanged may never get called
        // If, for example, settings->location->gps only and it can't get a fix:
        // Need to time it out if that happens.
    }
    
    @Override
    public void onLocationChanged(Location currentLocation){
        mOriginLocation = currentLocation; // Yay!
        // If the reported accuracy is within 1km, we're good.
        if(mOriginLocation != null){
            float accuracy = currentLocation.getAccuracy();
            if(accuracy <= 1000.0){
                // Good enough.
                mOriginAddress = new Address(new Locale("en", "US"));
                mOriginAddress.setFeatureName("Current Location");
                mOriginAddress.setLatitude(mOriginLocation.getLatitude());
                mOriginAddress.setLongitude(mOriginLocation.getLongitude());
                mFoundOrigin = true;
                if(mLocationRequestor != null){
                    // Notify the fragment, which should be sitting there waiting for us,
                    // that we're *finally* done getting the location, which will then
                    // make IT initialize its loader, and call our onWaterfallQuery method.
                    ((ResultsMapFragment) mLocationRequestor).onLocationDetermined();
                } else {
                    //Log.d(TAG, "Ooops, lol, requesting fragment is null.");
                }
                // Turn off updates.
                mLocationClient.removeLocationUpdates(this);
            } else { // Otherwise, we wait for more updates, up to 4.
                if(mNumUpdates >= 5){
                    // 5 inaccurate locations. Give up.
                    mLocationClient.removeLocationUpdates(this);
                    mOriginAddress = new Address(new Locale("en", "US"));
                    ((ResultsMapFragment) mLocationRequestor).onLocationDetermined();
                }
                mNumUpdates += 1;
            }
        } else {
            // Lame.
            mOriginAddress = new Address(new Locale("en", "US"));
        }
    }

    // Called by Location Services if the connection to the
    // location client drops because of an error.
    @Override
    public void onDisconnected() {
        // Display the connection status
        Toast.makeText(this, "Disconnected from location client. Please re-connect.", Toast.LENGTH_SHORT).show();
        // TODO: Notify the map fragment? How do we reconnect? The user doesn't need to know this.
    }

    // Called by Location Services if the attempt to connect fails.
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Google Play services can resolve some errors it detects.
        // If the error has a resolution, try sending an Intent to
        // start a Google Play services activity that can resolve the error.
        if (connectionResult.hasResolution()) {
            // TODO: Google Play Services needs installed.
        } else {
            // TODO: If no resolution is available, display a dialog to the
            // user with the error.
            // showErrorDialog(connectionResult.getErrorCode());
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_AppCompat);
        super.onCreate(savedInstanceState);

        // See if Google Play Services - and thus the Map tab - should be available
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean userPrefSkipPlayServices = settings.getBoolean(USER_PREF_SKIP_PLAY_SERVICES, false);
        
        // Restore or ingest our state from the saved state or the intent's extras
        Bundle instanceConfig;
        
        Intent intent = getIntent();
        instanceConfig = savedInstanceState != null ? savedInstanceState : intent.getExtras();
        
        Short defaultShort = 0;
        
        searchTerm = instanceConfig.getString(SearchActivity.EXTRA_SEARCH_TERM);
        searchTrailLength = instanceConfig.getShort(SearchActivity.EXTRA_SEARCH_TRAIL_LENGTH, defaultShort);
        searchTrailDifficulty = instanceConfig.getShort(SearchActivity.EXTRA_SEARCH_TRAIL_DIFFICULTY, defaultShort);
        searchTrailClimb = instanceConfig.getShort(SearchActivity.EXTRA_SEARCH_TRAIL_CLIMB, defaultShort);
        searchLocationDistance = instanceConfig.getShort(SearchActivity.EXTRA_SEARCH_LOCATION_DISTANCE, defaultShort);
        searchLocationRelto = instanceConfig.getString(SearchActivity.EXTRA_SEARCH_LOCATION_RELTO);
        searchLocationReltoTxt = instanceConfig.getString(SearchActivity.EXTRA_SEARCH_LOCATION_RELTO_TXT);
        searchOnlyShared = instanceConfig.getBoolean(SearchActivity.EXTRA_ONLY_SHARED, false);

        mSearchMode = instanceConfig.getShort(SearchActivity.EXTRA_SEARCH_MODE, defaultShort);
        if(mSearchMode == SearchActivity.SEARCH_MODE_LOCATION && !userPrefSkipPlayServices){
            // Create a location client for getting the current location.
            mLocationClient = new LocationClient(this, this, this);
            
            // Display Map tab
            showListTab = false;                    
        }
        
        // Set up tabs
        actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(false);

        ActionBar.Tab tab1 = actionBar.newTab();
        tab1.setText("List");
        tab1.setTabListener(new TabListener<ResultsListFragment>(
                                this,
                                "ResultsList",
                                ResultsListFragment.class));
        actionBar.addTab(tab1, showListTab);

        if(!userPrefSkipPlayServices){
            ActionBar.Tab tab2 = actionBar.newTab();
            tab2.setText("Map");
            tab2.setTabListener(new TabListener<ResultsMapFragment>(
                    this,
                    "ResultsMap",
                    ResultsMapFragment.class));
            actionBar.addTab(tab2, !showListTab);              
        }
    } // onCreate
    
    /*
     * Adapted from: http://stackoverflow.com/questions/3695224
     * Calculates the end-point from a given source at a given range (meters)
     * and bearing (degrees). This methods uses simple geometry equations to
     * calculate the end-point.
     * TODO: Move this somewhere that makes sense.
     */
    public static Location calculatePositionAtRange(Location origin, double range, double bearing){
        double EarthRadius = 6371000; // m

        double latA = Math.toRadians(origin.getLatitude());
        double lonA = Math.toRadians(origin.getLongitude());
        double angularDistance = range / EarthRadius;
        double trueCourse = Math.toRadians(bearing);

        double lat = Math.asin(
                Math.sin(latA) * Math.cos(angularDistance) +
                        Math.cos(latA) * Math.sin(angularDistance)
                        * Math.cos(trueCourse));

        double dlon = Math.atan2(
                Math.sin(trueCourse) * Math.sin(angularDistance)
                        * Math.cos(latA),
                Math.cos(angularDistance) - Math.sin(latA) * Math.sin(lat));

        double lon = ((lonA + dlon + Math.PI) % (Math.PI * 2)) - Math.PI;

        lat = Math.toDegrees(lat);
        lon = Math.toDegrees(lon);

        Location destination = new Location("");
        destination.setLatitude((float) lat);
        destination.setLongitude((float) lon);

        return destination;
    }
 
    // OnWaterfallQueryListener interface methods
    // Called by fragments which want sql to query their loaders with
    // Will be called after location is determined if we're in location mode.
    @Override
    public Bundle onWaterfallQuery() {
        // Set up our query
        ArrayList<String> whereList = new ArrayList<String>(); // To hold chunks of the WHERE clause
        ArrayList<String> argList = new ArrayList<String>(); // To hold our args
        switch(mSearchMode){
            // See which terms we're going to need to query           
            case SearchActivity.SEARCH_MODE_WATERFALL:
                whereList.add("name like ?");
                argList.add('%' + searchTerm.trim() + '%');
                break;

            case SearchActivity.SEARCH_MODE_HIKE:
                whereList.add("trail_length <= ?");
                argList.add(String.valueOf(searchTrailLength));

                whereList.add("trail_difficulty_num <= ?");
                argList.add(String.valueOf(searchTrailDifficulty));

                whereList.add("trail_climb_num <= ?");
                argList.add(String.valueOf(searchTrailClimb));

                break;

            case SearchActivity.SEARCH_MODE_LOCATION:
                if(mFoundOrigin){
                    // Mi -> M
                    double rangeMeters = searchLocationDistance * 1609.34;
                    
                    // Calculate our bounding box
                    Location pn = calculatePositionAtRange(mOriginLocation, rangeMeters, 0);
                    Location pe = calculatePositionAtRange(mOriginLocation, rangeMeters, 90);
                    Location ps = calculatePositionAtRange(mOriginLocation, rangeMeters, 180);
                    Location pw = calculatePositionAtRange(mOriginLocation, rangeMeters, 270);
                    
                    // Greater than S latitude
                    whereList.add("geo_lat > ?");
                    argList.add(String.valueOf(ps.getLatitude()));
                    
                    // Less than N latitude
                    whereList.add("geo_lat < ?");
                    argList.add(String.valueOf(pn.getLatitude()));
                    
                    // Less than E longitude
                    whereList.add("geo_lon < ?");
                    argList.add(String.valueOf(pe.getLongitude()));
                    
                    // Greater than W longitude
                    whereList.add("geo_lon > ?");
                    argList.add(String.valueOf(pw.getLongitude()));
                } else {
                    // Make sure no results are returned, and a toast should happen
                    // on the map side notifying the user as such.
                    whereList.add("_id = ?");
                    argList.add("");
                }

                // Requester can filter results to actual radius using 
                // android.location.Location.distanceTo()
                break;
        }
        
        // Restrict to only shared falls if box checked
        if(searchOnlyShared != null && searchOnlyShared){
            whereList.add("shared=1");
        }
        
        String tables = "waterfalls";
        
        // Select all
        String[] columns = AttrDatabase.COLUMNS.toArray(new String[AttrDatabase.COLUMNS.size()]);
        
        String and = " AND "; // To join our where clause
        String whereClause = TextUtils.join(and, whereList);
        
        String query = SQLiteQueryBuilder.buildQueryString(
                false, tables, columns, whereClause, null, null, "name ASC", null);
        
        Bundle qBundle = new Bundle();
        qBundle.putString("query", query);
        String[] args = argList.toArray(new String[argList.size()]);
        qBundle.putStringArray("args", args);
        return qBundle;
    }
    
    // A subclass of AsyncTask which uses the built-in Android platform geocoder
    // to return the lat/lon of a given address.
    private class GetLocationTask extends AsyncTask<String, Void, Address>{
        Context mContext;
        Fragment mCaller;
        public GetLocationTask(Context context){
            super();
            mContext = context;
        }

        @Override
        protected Address doInBackground(String... params){
            List<Address> addrList = null;
            
            // Get the coords for the given location
            Geocoder geocoder = new Geocoder(mContext);
            Address originAddress = new Address(new Locale("en", "US")); // Default null address
            
            try {
                addrList = geocoder.getFromLocationName(searchLocationReltoTxt, 1);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if(addrList != null && addrList.size() > 0){
                // Copy over default null address.
                originAddress = addrList.get(0);
                mFoundOrigin = true;
            } else {
                //Log.d(TAG, "Unable to geocode location: " + searchLocationReltoTxt);
            }
            return originAddress;
        }
        
        @Override
        protected void onPostExecute(Address originAddress){
            mOriginAddress = originAddress;
            
            if(originAddress.hasLatitude() && originAddress.hasLongitude()){
                mOriginLocation = new Location("");
                mOriginLocation.setLatitude(mOriginAddress.getLatitude());
                mOriginLocation.setLongitude(mOriginAddress.getLongitude());
            } else {
                //Log.d(TAG, "Oops...your origin location has not latitude/longitude.");
            }
            
            // Now invoke our fragment callback...
            if(mLocationRequestor != null){
                ((ResultsMapFragment) mLocationRequestor).onLocationDetermined();
            }
        }
    }

    public void determineLocation(Fragment requestor){
        mLocationRequestor = requestor;
        // Either start the geocoding in an AsnycTask, or connect the location client, which will
        // invoke onConnected when it's done. If the search mode wasn't by location, this does
        // nothing.
        if(mSearchMode == SearchActivity.SEARCH_MODE_LOCATION && searchLocationRelto.equals("Current Location")){
            startLocationClient(); // Gets the current location when done
        } else if(mSearchMode == SearchActivity.SEARCH_MODE_LOCATION) {
            (new GetLocationTask(this)).execute();
        } else {
            //Search was not location based; not necessary to geocode
            ((ResultsMapFragment) mLocationRequestor).onLocationDetermined();
        }
    }

    private void startLocationClient(){
        if(mLocationClient != null){
            mLocationClient.connect();
        }
    }

    public void stopLocationClient(){
        // TODO: Disconnecting the client invalidates it. If we do this,
        // and then need to use it again later, we'll need to create
        // a new one. After onStop() is called, we'll always
        // run through onCreate or onRestart. Take heed.
        if(mLocationClient != null){
            mLocationClient.disconnect();
        }
    }

    public Address getOriginAddress(){
        // Return the origin address this search was conducted with.
        return mOriginAddress;
    }

    public Location getOriginLocation(){
        // Return the origin address this search was conducted with.
        return mOriginLocation;
    }

    public short getSearchLocationDistance(){
        // Return the origin address this search was conducted with.
        return searchLocationDistance;
    }

    // OnWaterfallQueryListener interface methods
    // Called by fragments which want sql to query their loaders with
    @Override
    public void onWaterfallSelected(long waterfallId) {
        // Create new intent
        Intent intent = new Intent(this, InformationActivity.class);
        
        // Pack it with message containing search data
        intent.putExtra(SELECTED_WATERFALL_ID, waterfallId);
        
        // Start the Information activity
        startActivity(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        savedInstanceState.putShort(SearchActivity.EXTRA_SEARCH_MODE, mSearchMode);
        savedInstanceState.putString(SearchActivity.EXTRA_SEARCH_TERM, searchTerm);
        savedInstanceState.putShort(SearchActivity.EXTRA_SEARCH_TRAIL_LENGTH, searchTrailLength);
        savedInstanceState.putShort(SearchActivity.EXTRA_SEARCH_TRAIL_DIFFICULTY, searchTrailDifficulty);
        savedInstanceState.putShort(SearchActivity.EXTRA_SEARCH_TRAIL_CLIMB, searchTrailClimb);
        savedInstanceState.putShort(SearchActivity.EXTRA_SEARCH_LOCATION_DISTANCE, searchLocationDistance);
        savedInstanceState.putString(SearchActivity.EXTRA_SEARCH_LOCATION_RELTO, searchLocationRelto);
        savedInstanceState.putString(SearchActivity.EXTRA_SEARCH_LOCATION_RELTO_TXT, searchLocationReltoTxt);
        savedInstanceState.putBoolean(SearchActivity.EXTRA_ONLY_SHARED, searchOnlyShared);
        super.onSaveInstanceState(savedInstanceState);
    }
}
