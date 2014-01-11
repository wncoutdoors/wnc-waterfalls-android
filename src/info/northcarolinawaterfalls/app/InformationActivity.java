package info.northcarolinawaterfalls.app;

import android.content.Intent;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import info.northcarolinawaterfalls.app.R;
import info.northcarolinawaterfalls.app.InformationListFragment.OnWaterfallQueryListener;

public class InformationActivity extends SherlockFragmentActivity implements OnWaterfallQueryListener {
    private static final String TAG = "InformationActivity";
    private long selectedWaterfallId;
    private ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Sherlock);
        super.onCreate(savedInstanceState);

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

        ActionBar.Tab tab2 = actionBar.newTab();
        tab2.setText("Map");
        tab2.setTabListener(new TabListener<InformationMapFragment>(
                this,
                "InformationMap",
                InformationMapFragment.class));
        actionBar.addTab(tab2);
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
