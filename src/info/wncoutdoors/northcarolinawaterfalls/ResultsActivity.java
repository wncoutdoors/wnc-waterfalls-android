package info.wncoutdoors.northcarolinawaterfalls;

import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import info.wncoutdoors.northcarolinawaterfalls.SearchActivity.TabListener;

public class ResultsActivity extends SherlockFragmentActivity {
    private static final String TAG = "SearchActivity";
    private ActionBar actionBar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "In onCreate before setTheme");
        setTheme(R.style.Theme_Sherlock);
        super.onCreate(savedInstanceState);

        actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        ActionBar.Tab tab1 = actionBar.newTab();
        tab1.setText("List");
        tab1.setTabListener(new TabListener<ResultsListFragment>(
                                this,
                                "ResultsList",
                                ResultsListFragment.class));
        actionBar.addTab(tab1, true);

        ActionBar.Tab tab2 = actionBar.newTab();
        tab2.setText("Map");
        tab2.setTabListener(new TabListener<ResultsMapFragment>(
                                this,
                                "ResultsMap",
                                ResultsMapFragment.class));
        actionBar.addTab(tab2);

    }
    
}
