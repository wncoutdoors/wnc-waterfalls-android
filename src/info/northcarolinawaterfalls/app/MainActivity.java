
package info.northcarolinawaterfalls.app;

import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import info.northcarolinawaterfalls.app.R;
import info.northcarolinawaterfalls.app.ExpansionDownloaderDialogFragment.ExpansionDownloadDialogListener;

public class MainActivity extends SherlockFragmentActivity implements ExpansionDownloadDialogListener {
    
    private static final String TAG = "MainActivity";
    
    public static final String PREFS_NAME = "AppSettingsPreferences";
    private static final String USER_PREF_PAUSE_DOWNLOAD = "UserPrefPauseDownload";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Determine if the expansion files have been downloaded
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean userPrefPauseDownload = settings.getBoolean(USER_PREF_PAUSE_DOWNLOAD, false);
        if(!ExpansionDownloaderService.expansionFilesDownloaded(this)){
            // Warn user about offline maps.
            if(!userPrefPauseDownload){
                DialogFragment expansionDownloaderDialogFragment = new ExpansionDownloaderDialogFragment();
                expansionDownloaderDialogFragment.show(getSupportFragmentManager(), "expansionDownloaderDialogFragment");
            } else {
                Toast.makeText(getApplicationContext(), "Offline maps not available. Open App Info to download.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Offline maps are ready.", Toast.LENGTH_LONG).show();
        }      
        setContentView(R.layout.activity_main);
    }

    
    @Override
    protected void onResume(){
        super.onResume();
        /* TODO:
         * Important: Because it is hard to anticipate the state of each device, you must always
         * check for a compatible Google Play services APK before you access Google Play services
         * features. For many apps, the best time to check is during the onResume() method of the
         * main activity.
         */
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    */
    
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
        Intent intent = new Intent(this, AppInfoActivity.class);
        startActivity(intent);
    }
    
    // ExpansionDownloadDialogListener interface methods
    public void onDialogPositiveClick(DialogFragment dialog){
        Log.d(TAG, "Download affirmative.");
        Intent intent = new Intent(this, AppInfoActivity.class);
        startActivity(intent);
    }
    
    public void onDialogNegativeClick(DialogFragment dialog){
        Toast.makeText(getApplicationContext(), "Skipped downloading offline maps.", Toast.LENGTH_LONG).show();
    }
}
