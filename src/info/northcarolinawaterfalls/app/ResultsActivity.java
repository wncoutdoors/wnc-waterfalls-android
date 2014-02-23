package info.northcarolinawaterfalls.app;

import android.util.Log;
import android.widget.Toast;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteQueryBuilder;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import info.northcarolinawaterfalls.app.R;
import info.northcarolinawaterfalls.app.TabListener;
import info.northcarolinawaterfalls.app.ResultsListFragment.OnWaterfallQueryListener;
import info.northcarolinawaterfalls.app.ResultsListFragment.OnWaterfallSelectListener;
import info.northcarolinawaterfalls.app.ResultsMapFragment.OnLocationQueryListener;

import android.location.Geocoder;
import android.location.Address;
import android.location.Location;

public class ResultsActivity extends SherlockFragmentActivity implements 
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

    public static final String SELECTED_WATERFALL_ID = "info.northcarolinawaterfalls.app.SELECTED_WATERFALL_ID";
    
    /* Location management code adapted from:
     * http://developer.android.com/training/location/retrieve-current.html
     */
    
    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. This will happen when the user
     * has requested Current Location as the search point.
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        Log.d(TAG, "LocationClient connected.");
        
        /* Get our location
         * could use:
         * mOriginLocation = mLocationClient.getLastLocation();
         * However, with thta it's a pain to wait and try again if the returned location is stale/inaccurate
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
        Log.d(TAG, "Requested location updates from location client.");
        
        // TODO: In rare instances, onLocationChanged may never get called
        // If, for example, settings->location->gps only and it can't get a fix:
        // Need to time it out if that happens.
    }
    
    @Override
    public void onLocationChanged(Location currentLocation){
        Log.d(TAG, "Location update received.");
        mOriginLocation = currentLocation; // Yay!
        // If the reported accuracy is within 1km, we're good.
        if(mOriginLocation != null){
            float accuracy = currentLocation.getAccuracy();
            if(accuracy <= 1000.0){
                Log.d(TAG, "Accurate location determined; running query.");
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
                    Log.d(TAG, "Ooops, lol, requesting fragment is null.");
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
            Log.d(TAG, "onLocationChanged() passed null, how rude.");
            mOriginAddress = new Address(new Locale("en", "US"));
        }
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        // Display the connection status
        Toast.makeText(this, "Disconnected from location client. Please re-connect.", Toast.LENGTH_SHORT).show();
        // TODO: Notify the map fragment? How do we reconnect? The user doesn't need to know this.
    }

    // Called by Location Services if the attempt to connect fails.
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            // TODO: Google Play Services needs installed.
        } else {
            /*
             * TODO: If no resolution is available, display a dialog to the
             * user with the error.
             */
            // showErrorDialog(connectionResult.getErrorCode());
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Sherlock);
        super.onCreate(savedInstanceState);
        
        // See if Google Play Services - and thus the Map tab - should be available
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean userPrefSkipPlayServices = settings.getBoolean(USER_PREF_SKIP_PLAY_SERVICES, false);
        
        // Unpack search data
        Intent intent = getIntent();
        Short defaultShort = 0;
        mSearchMode = intent.getShortExtra(SearchActivity.EXTRA_SEARCH_MODE, defaultShort);
        
        switch(mSearchMode){
            // See which terms we're going to need to query
            case SearchActivity.SEARCH_MODE_WATERFALL:
                // Display List tab
                searchTerm = intent.getStringExtra(SearchActivity.EXTRA_SEARCH_TERM);
                break;
            
            case SearchActivity.SEARCH_MODE_HIKE:
                // Display List tab
                searchTrailLength = intent.getShortExtra(SearchActivity.EXTRA_SEARCH_TRAIL_LENGTH, defaultShort);
                searchTrailDifficulty = intent.getShortExtra(SearchActivity.EXTRA_SEARCH_TRAIL_DIFFICULTY, defaultShort);
                searchTrailClimb = intent.getShortExtra(SearchActivity.EXTRA_SEARCH_TRAIL_CLIMB, defaultShort);
                break;
            
            case SearchActivity.SEARCH_MODE_LOCATION:
                // Create a location client for getting the current location.
                if(!userPrefSkipPlayServices){
                    mLocationClient = new LocationClient(this, this, this);
                    
                    // Display Map tab
                    showListTab = false;
                    searchLocationDistance = intent.getShortExtra(SearchActivity.EXTRA_SEARCH_LOCATION_DISTANCE, defaultShort);
                    searchLocationRelto = intent.getStringExtra(SearchActivity.EXTRA_SEARCH_LOCATION_RELTO);
                    searchLocationReltoTxt = intent.getStringExtra(SearchActivity.EXTRA_SEARCH_LOCATION_RELTO_TXT);                    
                }
                break;
        }
        
        searchOnlyShared = intent.getBooleanExtra(SearchActivity.EXTRA_ONLY_SHARED, false);
        
        // Set up tabs
        actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

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
    
    /**
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
                Log.d(TAG, "Building waterfall query.");
                whereList.add("name like ?");
                argList.add('%' + searchTerm.trim() + '%');
                break;

            case SearchActivity.SEARCH_MODE_HIKE:
                Log.d(TAG, "Building hike query.");
                whereList.add("trail_length <= ?");
                argList.add(String.valueOf(searchTrailLength));

                whereList.add("trail_difficulty_num <= ?");
                argList.add(String.valueOf(searchTrailDifficulty));

                whereList.add("trail_climb_num <= ?");
                argList.add(String.valueOf(searchTrailClimb));

                break;

            case SearchActivity.SEARCH_MODE_LOCATION:
                Log.d(TAG, "Building location query.");               
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
        
        Log.d(TAG, "Query is: " + query);
        Log.d(TAG, "Args are: " + TextUtils.join(" | ", argList));
        
        Bundle qBundle = new Bundle();
        qBundle.putString("query", query);
        String[] args = argList.toArray(new String[argList.size()]);
        qBundle.putStringArray("args", args);
        return qBundle;
    }
    
    /*
     * A subclass of AsyncTask which uses the built-in Android platform geocoder
     * to return the lat/lon of a given address.
     */
    private class GetLocationTask extends AsyncTask<String, Void, Address>{
        Context mContext;
        Fragment mCaller;
        public GetLocationTask(Context context){
            super();
            mContext = context;
        }

        @Override
        protected Address doInBackground(String... params){
            Log.d(TAG, "Geocoding location: " + searchLocationReltoTxt);
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
                // Debug
                for(Address addr: addrList){
                    Log.d(TAG, "Found address: " + addr.toString() + " , " +
                            addr.getLatitude() + ", " + addr.getLongitude());
                }
                // Copy over default null address.
                originAddress = addrList.get(0);
                mFoundOrigin = true;
            } else {
                Log.d(TAG, "Unable to geocode location: " + searchLocationReltoTxt);
            }
            return originAddress;
        }
        
        @Override
        protected void onPostExecute(Address originAddress){
            Log.d(TAG, "Inside GetLocationTask onPostExecute.");
            mOriginAddress = originAddress;
            
            if(originAddress.hasLatitude() && originAddress.hasLongitude()){
                mOriginLocation = new Location("");
                mOriginLocation.setLatitude(mOriginAddress.getLatitude());
                mOriginLocation.setLongitude(mOriginAddress.getLongitude());
            } else {
                Log.d(TAG, "Oops...your origin location has not latitude/longitude.");
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
            Log.d(TAG, "Search was not location based; not necessary to geocode.");
            ((ResultsMapFragment) mLocationRequestor).onLocationDetermined();
        }
    }

    private void startLocationClient(){
        Log.d(TAG, "Connecting location client.");
        if(mLocationClient != null){
            mLocationClient.connect();
        }
    }

    public void stopLocationClient(){
        // TODO: Disconnecting the client invalidates it. If we do this,
        // and then need to use it again later, we'll need to create
        // a new one. After onStop() is called, we'll always
        // run through onCreate or onRestart. Take heed.
        Log.d(TAG, "Disconnecting location client.");
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
        Log.d(TAG, "In onWaterfallSelected callback.");
        
        // Create new intent
        Intent intent = new Intent(this, InformationActivity.class);
        
        // Pack it with message containing search data
        intent.putExtra(SELECTED_WATERFALL_ID, waterfallId);
        
        // Start the Information activity
        startActivity(intent);
    }
} // ResultsActivity
