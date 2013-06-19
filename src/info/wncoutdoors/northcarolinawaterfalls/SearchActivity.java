package info.wncoutdoors.northcarolinawaterfalls;

import android.util.Log;
import android.view.View;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class SearchActivity extends SherlockFragmentActivity {
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
    
    public static class TabListener<T extends SherlockFragment> implements ActionBar.TabListener{
        // A generic tab listener to handle swapping of fragments.

        private SherlockFragment aFragment;
        private final SherlockFragmentActivity anActivity;
        private final String aFragTag;
        private final Class<T> aClass;

        public TabListener(SherlockFragmentActivity activity, String tag, Class<T> cls) {
            anActivity = activity;
            aFragTag = tag;
            aClass = cls;
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction transaction) {
            Log.d(TAG, "Inside onTabReselected");
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction transaction) {
            Log.d(TAG, "Inside onTabSelected, and transaction is " + transaction);
            SherlockFragment aFragment = (SherlockFragment) anActivity.getSupportFragmentManager().findFragmentByTag(aFragTag);
            // Check if the fragment is already initialized
            if(aFragment == null){
                // Create a new one
                aFragment = (SherlockFragment) SherlockFragment.instantiate(anActivity, aClass.getName());
                transaction.add(android.R.id.content, aFragment, aFragTag);
                Log.d(TAG, "Created new fragment: " + aFragment);
            } else {
                // Attach existing one
                transaction.attach(aFragment);
                Log.d(TAG, "Attached " + this.aFragment + " to transaction.");
            }
            this.aFragment = aFragment;
            Log.d(TAG, "aFragement was: " + (this.aFragment == null ? "null": "NOT null"));
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction transaction) {
            Log.d(TAG, "Inside onTabUnselected");
            if(aFragment != null){
                Log.d(TAG, "Removing fragment.");
                transaction.detach(aFragment);
            }
        }       
    }
}
