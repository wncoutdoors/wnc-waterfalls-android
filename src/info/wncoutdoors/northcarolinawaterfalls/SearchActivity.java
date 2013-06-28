package info.wncoutdoors.northcarolinawaterfalls;

import android.util.Log;
import android.os.Bundle;
import android.content.Intent;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import info.wncoutdoors.northcarolinawaterfalls.TabListener;
import info.wncoutdoors.northcarolinawaterfalls.SearchWaterfallFragment.OnWaterfallSearchListener;
import info.wncoutdoors.northcarolinawaterfalls.SearchHikeFragment.OnHikeSearchListener;
import info.wncoutdoors.northcarolinawaterfalls.SearchLocationFragment.OnLocationSearchListener;

public class SearchActivity extends SherlockFragmentActivity 
    implements OnWaterfallSearchListener, OnHikeSearchListener, OnLocationSearchListener {
    /* TODO:
     * - save state of forms when activity is stopped
     * (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    public static final short SEARCH_MODE_WATERFALL = 0;
    public static final short SEARCH_MODE_HIKE = 1;
    public static final short SEARCH_MODE_LOCATION = 2;
    
    public final static String EXTRA_ONLY_SHARED = "info.northcarolinawaterfalls.EXTRA_ONLY_SHARED";
    
    public final static String EXTRA_SEARCH_MODE = "info.northcarolinawaterfalls.SEARCH_MODE";
    public final static String EXTRA_SEARCH_TERM = "info.northcarolinawaterfalls.SEARCH_TERM";
    
    public final static String EXTRA_SEARCH_TRAIL_LENGTH = "info.northcarolinawaterfalls.SEARCH_TRAIL_LENGTH";
    public final static String EXTRA_SEARCH_TRAIL_DIFFICULTY = "info.northcarolinawaterfalls.SEARCH_TRAIL_LENGTH";
    public final static String EXTRA_SEARCH_TRAIL_CLIMB = "info.northcarolinawaterfalls.SEARCH_TRAIL_CLIMB";

    public final static String EXTRA_SEARCH_LOCATION_DISTANCE = "info.northcarolinawaterfalls.SEARCH_LOCATION_DISTANCE";
    public final static String EXTRA_SEARCH_LOCATION_RELTO = "info.northcarolinawaterfalls.SEARCH_LOCATION_RELTO";
    public final static String EXTRA_SEARCH_LOCATION_RELTOTXT = "info.northcarolinawaterfalls.SEARCH_LOCATION_RELTOTXT";
    
    private static final String TAG = "SearchActivity";
    private ActionBar actionBar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "In onCreate before setTheme");
        setTheme(R.style.Theme_Sherlock);
        super.onCreate(savedInstanceState);

        actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        /* Don't need to call setContentView because we're given a
         * default ViewGroup in which to plop our fragments.
         */
        ActionBar.Tab tab1 = actionBar.newTab();
        tab1.setText("WATERFALL");
        tab1.setTabListener(new TabListener<SearchWaterfallFragment>(
                                this,
                                "SearchWaterfall",
                                SearchWaterfallFragment.class));
        actionBar.addTab(tab1, true);

        ActionBar.Tab tab2 = actionBar.newTab();
        tab2.setText("HIKE");
        tab2.setTabListener(new TabListener<SearchHikeFragment>(
                                this,
                                "SearchHike",
                                SearchHikeFragment.class));
        actionBar.addTab(tab2);

        ActionBar.Tab tab3 = actionBar.newTab();
        tab3.setText("LOCATION");
        tab3.setTabListener(new TabListener<SearchLocationFragment>(
                                this, "SearchLocation",
                                SearchLocationFragment.class));
        actionBar.addTab(tab3);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getSupportMenuInflater().inflate(R.menu.search, menu);
        return true;
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putInt("SEARCH_SAVED_INDEX", actionBar.getSelectedNavigationIndex());
    }

    
    // OnWaterfallSearchListener interface methods //
    public void onWaterfallSearch(boolean onlyShared, String searchTerm){
        Log.d(TAG, "In onWaterfallSearch callback; term: " + searchTerm);
        
        // Create new intent
        Intent intent = new Intent(this, ResultsActivity.class);
        
        // Pack it with message containing search term
        intent.putExtra(EXTRA_ONLY_SHARED, onlyShared);
        intent.putExtra(EXTRA_SEARCH_MODE, SEARCH_MODE_WATERFALL);
        intent.putExtra(EXTRA_SEARCH_TERM, searchTerm);
        
        // Start the Results activity
        startActivity(intent);
    }

    // OnHikeSearchListener interface methods //
    public void onHikeSearch(boolean onlyShared, short trailLength, String trailDifficulty, String trailClimb){
        Log.d(TAG, "In onHikeSearch callback.");
        
        // Create new intent
        Intent intent = new Intent(this, ResultsActivity.class);
        
        // Pack it with message containing search data
        intent.putExtra(EXTRA_ONLY_SHARED, onlyShared);
        intent.putExtra(EXTRA_SEARCH_MODE, SEARCH_MODE_HIKE);
        intent.putExtra(EXTRA_SEARCH_TRAIL_LENGTH, trailLength);
        intent.putExtra(EXTRA_SEARCH_TRAIL_DIFFICULTY, trailDifficulty);
        intent.putExtra(EXTRA_SEARCH_TRAIL_CLIMB, trailClimb);
        
        // Start the Results activity
        startActivity(intent);
    }
    
    // OnLocationSearchListener interface methods //
    public void onLocationSearch(boolean onlyShared, short distance, String relTo, String relToTxt){
        Log.d(TAG, "In onLocationSearch callback.");
        
        // Create new intent
        Intent intent = new Intent(this, ResultsActivity.class);
        
        // Pack it with message containing search data
        intent.putExtra(EXTRA_ONLY_SHARED, onlyShared);
        intent.putExtra(EXTRA_SEARCH_MODE, SEARCH_MODE_LOCATION);
        intent.putExtra(EXTRA_SEARCH_LOCATION_DISTANCE, distance);
        intent.putExtra(EXTRA_SEARCH_TRAIL_DIFFICULTY, relTo);
        intent.putExtra(EXTRA_SEARCH_TRAIL_CLIMB, relToTxt);
        
        // Start the Results activity
        startActivity(intent);
    }
    
}
