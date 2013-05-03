
package info.wncoutdoors.northcarolinawaterfalls;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.ActionBar.Tab;

public class SearchActivity extends SherlockActivity implements ActionBar.TabListener {
    private TextView mSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Sherlock);
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_search);
        mSelected = (TextView)findViewById(R.id.text);
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        ActionBar.Tab tab1 = getSupportActionBar().newTab();
        tab1.setText("WATERFALL");
        tab1.setTabListener(this);
        getSupportActionBar().addTab(tab1);

        ActionBar.Tab tab2 = getSupportActionBar().newTab();
        tab2.setText("HIKE");
        tab2.setTabListener(this);
        getSupportActionBar().addTab(tab2);
        
        ActionBar.Tab tab3 = getSupportActionBar().newTab();
        tab3.setText("LOCATION");
        tab3.setTabListener(this);
        getSupportActionBar().addTab(tab3);
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.search, menu);
        return true;
    }
    */

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction transaction) {
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction transaction) {
        mSelected.setText("Selected: " + tab.getText());
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction transaction) {
    }

}
