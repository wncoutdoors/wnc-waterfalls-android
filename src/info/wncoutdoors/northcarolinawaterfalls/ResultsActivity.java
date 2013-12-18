package info.wncoutdoors.northcarolinawaterfalls;

import android.util.Log;
import android.os.Bundle;
import android.text.TextUtils;
import android.content.Intent;
import android.database.sqlite.SQLiteQueryBuilder;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import info.wncoutdoors.northcarolinawaterfalls.TabListener;
import info.wncoutdoors.northcarolinawaterfalls.ResultsListFragment.OnWaterfallQueryListener;
import info.wncoutdoors.northcarolinawaterfalls.ResultsListFragment.OnWaterfallSelectListener;

import android.location.Geocoder;
import android.location.Address;
import android.location.Location;

public class ResultsActivity extends SherlockFragmentActivity 
        implements OnWaterfallQueryListener, OnWaterfallSelectListener {
    private static final String TAG = "ResultsActivity";
    private ActionBar actionBar;
    private boolean showListTab = true;
    
    private Short searchMode; // 1: waterfall, 2: hike, 3: location
    
    private String searchTerm;
    
    private short searchTrailLength;
    private short searchTrailDifficulty;
    private short searchTrailClimb;
    
    private Short searchLocationDistance;
    private String searchLocationRelto;
    private String searchLocationReltoTxt;
    
    private Boolean searchOnlyShared;
    
    private Address mOriginAddress;

    public static final String SELECTED_WATERFALL_ID = "info.northcarolinawaterfalls.SELECTED_WATERFALL_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Sherlock);
        super.onCreate(savedInstanceState);

        // Unpack search data
        Intent intent = getIntent();
        Short defaultShort = 0;
        searchMode = intent.getShortExtra(SearchActivity.EXTRA_SEARCH_MODE, defaultShort);
        
        switch(searchMode){
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
                // Display Map tab
                showListTab = false;
                searchLocationDistance = intent.getShortExtra(SearchActivity.EXTRA_SEARCH_LOCATION_DISTANCE, defaultShort);
                searchLocationRelto = intent.getStringExtra(SearchActivity.EXTRA_SEARCH_LOCATION_RELTO);
                searchLocationReltoTxt = intent.getStringExtra(SearchActivity.EXTRA_SEARCH_LOCATION_RELTO_TXT);
                Log.d(TAG, "Location search results coming right up.");
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

        ActionBar.Tab tab2 = actionBar.newTab();
        tab2.setText("Map");
        tab2.setTabListener(new TabListener<ResultsMapFragment>(
                this,
                "ResultsMap",
                ResultsMapFragment.class));
        actionBar.addTab(tab2, !showListTab);    
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
    @Override
    public Bundle onWaterfallQuery() {
        // Set up our query
        ArrayList<String> whereList = new ArrayList<String>(); // To hold chunks of the WHERE clause
        ArrayList<String> argList = new ArrayList<String>(); // To hold our args
        switch(searchMode){
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
                // Get the coords for the given location
                Geocoder geocoder = new Geocoder(this);
                
                // For current location, use a LocationProvider
                if(searchLocationRelto == "Current Location"){
                    Log.d(TAG, "Getting current location.");
                } else {
                    Log.d(TAG, "Geocoding location: " + searchLocationReltoTxt);
                    List<Address> addrList = null;
                    try {
                        addrList = geocoder.getFromLocationName(searchLocationReltoTxt, 1);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        // Maybe throw up a toast saying we can't calculate the location?
                    }
                    if(addrList != null && addrList.size() > 0){
                        // Debug
                        for(Address addr: addrList){
                            Log.d(TAG, "Found address: " + addr.toString() + " , " + addr.getLatitude() + ", " + addr.getLongitude());
                        }
                        
                        // Copy address coordinates to a Location
                        mOriginAddress = addrList.get(0);
                        Location originLocation = new Location("");
                        originLocation.setLatitude(mOriginAddress.getLatitude());
                        originLocation.setLongitude(mOriginAddress.getLongitude());
                        
                        // Mi -> M
                        double rangeMeters = searchLocationDistance * 1609.34;
                        
                        // Calculate our bounding box
                        Location pn = calculatePositionAtRange(originLocation, rangeMeters, 0);
                        Location pe = calculatePositionAtRange(originLocation, rangeMeters, 90);
                        Location ps = calculatePositionAtRange(originLocation, rangeMeters, 180);
                        Location pw = calculatePositionAtRange(originLocation, rangeMeters, 270);
                        
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
                    }
                }
                // Listener can filter results to actual radius using 
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
    
    public Address getOriginAddress(){
        // Return the origin address this search was conducted with.
        return mOriginAddress;
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
