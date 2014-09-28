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
 * SearchActivity.java
 * Activity which hosts the various tabs within which users can search for
 * waterfalls.
 */
package info.wncwaterfalls.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;

import info.wncwaterfalls.app.R;
import info.wncwaterfalls.app.SearchHikeFragment.OnHikeSearchListener;
import info.wncwaterfalls.app.SearchLocationFragment.OnLocationSearchListener;
import info.wncwaterfalls.app.SearchWaterfallFragment.OnWaterfallSearchListener;

public class SearchActivity extends SherlockFragmentActivity 
    implements OnWaterfallSearchListener, OnHikeSearchListener, OnLocationSearchListener {

    public static final String PREFS_NAME = "AppSettingsPreferences";
    private static final String USER_PREF_SKIP_PLAY_SERVICES = "UserPrefSkipPlayServices";
    
    public static final short SEARCH_MODE_WATERFALL = 0;
    public static final short SEARCH_MODE_HIKE = 1;
    public static final short SEARCH_MODE_LOCATION = 2;

    public final static String EXTRA_ONLY_SHARED = "info.wncwaterfalls.app.EXTRA_ONLY_SHARED";

    public final static String EXTRA_SEARCH_MODE = "info.wncwaterfalls.app.SEARCH_MODE";
    public final static String EXTRA_SEARCH_TERM = "info.wncwaterfalls.app.SEARCH_TERM";

    public final static String EXTRA_SEARCH_TRAIL_LENGTH = "info.wncwaterfalls.app.SEARCH_TRAIL_LENGTH";
    public final static String EXTRA_SEARCH_TRAIL_DIFFICULTY = "info.wncwaterfalls.app.SEARCH_TRAIL_LENGTH";
    public final static String EXTRA_SEARCH_TRAIL_CLIMB = "info.wncwaterfalls.app.SEARCH_TRAIL_CLIMB";

    public final static String EXTRA_SEARCH_LOCATION_DISTANCE = "info.wncwaterfalls.app.SEARCH_LOCATION_DISTANCE";
    public final static String EXTRA_SEARCH_LOCATION_RELTO = "info.wncwaterfalls.app.SEARCH_LOCATION_RELTO";
    public final static String EXTRA_SEARCH_LOCATION_RELTO_TXT = "info.wncwaterfalls.app.SEARCH_LOCATION_RELTO_TXT";
    
    private static final String TAG = "SearchActivity";
    private ActionBar actionBar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Sherlock);
        super.onCreate(savedInstanceState);
        
        short requestedTab = 0;
        
        // See if Google Play Services - and thus the Map tab - should be available
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean userPrefSkipPlayServices = settings.getBoolean(USER_PREF_SKIP_PLAY_SERVICES, false);

        actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(false);
 
        if(savedInstanceState == null){
            Intent intent = getIntent();
            requestedTab = intent.getShortExtra(SearchActivity.EXTRA_SEARCH_MODE, SEARCH_MODE_WATERFALL);
        } else {
            requestedTab = savedInstanceState.getShort("SEARCH_SAVED_SELECTED_INDEX");
        }
        
        Log.d(TAG, "Inside SearchActivity onCreate. searchMode is " + requestedTab);
        
        // Don't need to call setContentView because we're given a
        // default ViewGroup in which to plop our fragments.
        ActionBar.Tab tab1 = actionBar.newTab();
        tab1.setText("WATERFALL");
        tab1.setTabListener(new TabListener<SearchWaterfallFragment>(
                                this,
                                "SearchWaterfall",
                                SearchWaterfallFragment.class));
        actionBar.addTab(tab1, requestedTab==SEARCH_MODE_WATERFALL);

        ActionBar.Tab tab2 = actionBar.newTab();
        tab2.setText("HIKE");
        tab2.setTabListener(new TabListener<SearchHikeFragment>(
                                this,
                                "SearchHike",
                                SearchHikeFragment.class));
        actionBar.addTab(tab2, requestedTab==SEARCH_MODE_HIKE);

        if(!userPrefSkipPlayServices){
            ActionBar.Tab tab3 = actionBar.newTab();
            tab3.setText("LOCATION");
            tab3.setTabListener(new TabListener<SearchLocationFragment>(
                                    this, "SearchLocation",
                                    SearchLocationFragment.class));
            actionBar.addTab(tab3, requestedTab==SEARCH_MODE_LOCATION);   
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getSupportMenuInflater().inflate(R.menu.search, menu);
        return true;
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState){
        outState.putInt("SEARCH_SAVED_SELECTED_INDEX", actionBar.getSelectedNavigationIndex());
        super.onSaveInstanceState(outState);
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
    public void onHikeSearch(boolean onlyShared, short trailLength, short trailDifficulty, short trailClimb){
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
        intent.putExtra(EXTRA_SEARCH_LOCATION_RELTO, relTo);
        intent.putExtra(EXTRA_SEARCH_LOCATION_RELTO_TXT, relToTxt);
        
        // Start the Results activity
        startActivity(intent);
    }
    
}
