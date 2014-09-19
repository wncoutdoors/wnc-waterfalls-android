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
 * InformationActivity.java
 * Activity which hosts the main information tabs, including the
 * waterfall details, photo, and map.
 */
package info.northcarolinawaterfalls.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import info.northcarolinawaterfalls.app.InformationListFragment.OnWaterfallQueryListener;

public class InformationActivity extends SherlockFragmentActivity implements OnWaterfallQueryListener {
    private static final String TAG = "InformationActivity";

    public static final String PREFS_NAME = "AppSettingsPreferences";
    private static final String USER_PREF_SKIP_PLAY_SERVICES = "UserPrefSkipPlayServices";
    
    private long selectedWaterfallId;
    private ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Sherlock);
        super.onCreate(savedInstanceState);
        
        // See if Google Play Services - and thus the Map tab - should be available
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean userPrefSkipPlayServices = settings.getBoolean(USER_PREF_SKIP_PLAY_SERVICES, false);

        // Unpack selected waterfall ID
        Intent intent = getIntent();
        Short defaultLong = 0;
        selectedWaterfallId = intent.getLongExtra(ResultsActivity.SELECTED_WATERFALL_ID, defaultLong);

        Log.d(TAG, "Information activity asked to display waterfall " + selectedWaterfallId);
        
        // Set up tabs
        actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        ActionBar.Tab tab1 = actionBar.newTab();
        tab1.setText("Details");
        tab1.setTabListener(new TabListener<InformationListFragment>(
                                this,
                                "InformationList",
                                InformationListFragment.class));
        actionBar.addTab(tab1, true);

        if(!userPrefSkipPlayServices){
            ActionBar.Tab tab2 = actionBar.newTab();
            tab2.setText("Map");
            tab2.setTabListener(new TabListener<InformationMapFragment>(
                    this,
                    "InformationMap",
                    InformationMapFragment.class));
            actionBar.addTab(tab2);
        }
    }

    // OnWaterfallQueryListener interface methods
    // Called by fragments which want sql to query their loaders with
    // because we know the parameters.
    @Override
    public Bundle onWaterfallQuery() {
        // Set up our query
        String whereClause = "_id = ?";
        String tables = "waterfalls";
        String[] columns = AttrDatabase.COLUMNS.toArray(new String[AttrDatabase.COLUMNS.size()]);

        String query = SQLiteQueryBuilder.buildQueryString(
                false, tables, columns, whereClause, null, null, "_id ASC", null);

        String[] args = {
                String.valueOf(selectedWaterfallId)
        };

        Log.d(TAG, "Waterfall query is: " + query);

        Bundle qBundle = new Bundle();
        qBundle.putString("query", query);
        qBundle.putStringArray("args", args);
        return qBundle;
    }
}
