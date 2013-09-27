package info.wncoutdoors.northcarolinawaterfalls;

import android.util.Log;
import android.os.Bundle;
import android.text.TextUtils;
import android.content.Intent;
import android.database.sqlite.SQLiteQueryBuilder;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import java.util.ArrayList;

import info.wncoutdoors.northcarolinawaterfalls.TabListener;
import info.wncoutdoors.northcarolinawaterfalls.ResultsListFragment.OnWaterfallQueryListener;
import info.wncoutdoors.northcarolinawaterfalls.ResultsListFragment.OnWaterfallSelectListener;

public class ResultsActivity extends SherlockFragmentActivity implements OnWaterfallQueryListener, OnWaterfallSelectListener {
    private static final String TAG = "ResultsActivity";
    private ActionBar actionBar;
    private boolean showListTab = true;
    
    private Short searchMode; // 1: waterfall, 2: hike, 3: location
    
    private String searchTerm;
    
    private Short searchTrailLength;
    private String searchTrailDifficulty;
    private String searchTrailClimb;
    
    private Short searchLocationDistance;
    private String searchLocationRelto;
    private String searchLocationReltoTxt;
    
    private Boolean searchOnlyShared;
    
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
                searchTrailDifficulty = intent.getStringExtra(SearchActivity.EXTRA_SEARCH_TRAIL_DIFFICULTY);
                searchTrailClimb = intent.getStringExtra(SearchActivity.EXTRA_SEARCH_TRAIL_CLIMB);
                break;
            
            case SearchActivity.SEARCH_MODE_LOCATION:
                // Display Map tab
                showListTab = false;
                searchLocationDistance = intent.getShortExtra(SearchActivity.EXTRA_SEARCH_LOCATION_DISTANCE, defaultShort);
                searchLocationRelto = intent.getStringExtra(SearchActivity.EXTRA_SEARCH_LOCATION_RELTO);
                searchLocationReltoTxt = intent.getStringExtra(SearchActivity.EXTRA_SEARCH_LOCATION_RELTOTXT);
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
                if(searchTrailLength != null){
                    whereList.add("trail_length <= ?");
                    argList.add(searchTrailLength.toString());
                }

                if(searchTrailDifficulty != null){
                    whereList.add("trail_difficulty <= ?");
                    argList.add(searchTrailDifficulty.toString());
                }
                
                if(searchTrailClimb != null){
                    whereList.add("trail_climb <= ?");
                    argList.add(searchTrailDifficulty.toString());
                }

                break;

            case SearchActivity.SEARCH_MODE_LOCATION:
                Log.d(TAG, "Building location query.");
                // oh joy, let's do a spatial query here.
                break;
        }
        
        // Restrict to only shared falls if box checked
        if(searchOnlyShared != null && searchOnlyShared){
            whereList.add("shared=1");
        }
        
        String tables = "waterfalls";
        String[] columns = {
            "_id", "name", "alt_names", "description", "height", "stream", "landowner",
            "elevation", "directions", "trail_directions", "trail_difficulty", "trail_difficulty_num",
            "trail_length", "trail_climb", "trail_elevationlow", "trail_elevationhigh",
            "trail_elevationgain", "trail_tread", "trail_configuration", "photo", "photo_filename",
            "shared" };
        
        String and = " AND "; // To join our where clause
        String whereClause = TextUtils.join(and, whereList);
        
        String query = SQLiteQueryBuilder.buildQueryString(
                false, tables, columns, whereClause, null, null, "_id ASC", null);
        
        Log.d(TAG, "Query is: " + query);
        
        Bundle qBundle = new Bundle();
        qBundle.putString("query", query);
        String[] args = argList.toArray(new String[argList.size()]);
        qBundle.putStringArray("args", args);
        return qBundle;
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
