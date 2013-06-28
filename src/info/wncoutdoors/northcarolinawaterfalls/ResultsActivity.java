package info.wncoutdoors.northcarolinawaterfalls;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import info.wncoutdoors.northcarolinawaterfalls.TabListener;

public class ResultsActivity extends SherlockFragmentActivity {
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
    
    private String searchOnlyShared;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Sherlock);
        super.onCreate(savedInstanceState);
       
        // Determine search data
        Intent intent = getIntent();     
        Short defaultShort = 0;
        searchMode = intent.getShortExtra(SearchActivity.EXTRA_SEARCH_MODE, defaultShort);
        
        switch(searchMode){
            // See which terms we're going to need to query
            case SearchActivity.SEARCH_MODE_WATERFALL:
                // Display List tab
                searchTerm = intent.getStringExtra(SearchActivity.EXTRA_SEARCH_TERM);
                Log.d(TAG, "Waterfall search results coming right up for: " + searchTerm);
                break;
            
            case SearchActivity.SEARCH_MODE_HIKE:
                // Display List tab
                searchTrailLength = intent.getShortExtra(SearchActivity.EXTRA_SEARCH_TRAIL_LENGTH, defaultShort);
                searchTrailDifficulty = intent.getStringExtra(SearchActivity.EXTRA_SEARCH_TRAIL_DIFFICULTY);
                searchTrailClimb = intent.getStringExtra(SearchActivity.EXTRA_SEARCH_TRAIL_CLIMB);
                Log.d(TAG, "Hike search results coming right up.");
                
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
        
        // Perform the search!
        AttrDatabase db = new AttrDatabase(this);
        
        // Don't call this on the main thread, yo
        Cursor records = db.getCount();
        
        Log.d(TAG, "There are " + records.getString(0) + " records in the database." );
        
    } // onCreate
    
} // ResultsActivity
