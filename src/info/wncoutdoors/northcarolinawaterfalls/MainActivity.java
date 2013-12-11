
package info.wncoutdoors.northcarolinawaterfalls;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity {
    
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    
    @Override
    public void onResume(){
        super.onResume();
        /* TODO:
         * Important: Because it is hard to anticipate the state of each device, you must always
         * check for a compatible Google Play services APK before you access Google Play services
         * features. For many apps, the best time to check is during the onResume() method of the
         * main activity.
         */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    public void searchAll(View view){
        // Create new intent
        Intent intent = new Intent(this, ResultsActivity.class);
        
        // Pack it with message containing blank search term (for "show all")
        intent.putExtra(SearchActivity.EXTRA_ONLY_SHARED, false);
        intent.putExtra(SearchActivity.EXTRA_SEARCH_MODE, SearchActivity.SEARCH_MODE_WATERFALL);
        intent.putExtra(SearchActivity.EXTRA_SEARCH_TERM, "");
        
        // Start the Results activity
        startActivity(intent);
    }

    public void searchByName(View view){
        Intent intent = new Intent(this, SearchActivity.class);
        intent.putExtra(SearchActivity.EXTRA_SEARCH_MODE, SearchActivity.SEARCH_MODE_WATERFALL);
        startActivity(intent);
    }

    public void searchByHike(View view){
        Intent intent = new Intent(this, SearchActivity.class);
        intent.putExtra(SearchActivity.EXTRA_SEARCH_MODE, SearchActivity.SEARCH_MODE_HIKE);
        startActivity(intent);
    }
    
    public void searchByLocation(View view){
        Intent intent = new Intent(this, SearchActivity.class);
        intent.putExtra(SearchActivity.EXTRA_SEARCH_MODE, SearchActivity.SEARCH_MODE_LOCATION);
        startActivity(intent);
    }
    
    public void searchByShared(View view){
        Intent intent = new Intent(this, ResultsActivity.class);
        // Pack intent with message containing blank search term (for "show all")
        // and shared = true
        intent.putExtra(SearchActivity.EXTRA_ONLY_SHARED, true);
        intent.putExtra(SearchActivity.EXTRA_SEARCH_MODE, SearchActivity.SEARCH_MODE_WATERFALL);
        intent.putExtra(SearchActivity.EXTRA_SEARCH_TERM, "");
        startActivity(intent);
    }
    
    public void appInfo(View view){
        Log.d(TAG, "Inside appInfo");
    }
}
