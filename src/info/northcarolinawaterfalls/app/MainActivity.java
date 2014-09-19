/*
 * Copyright 2014 WNCOutdoors.info
 * portions Copyright 2014 The Android Open Source Project
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
 * MainActivity.java
 * Activity which checks a few requirements for the app to run, and 
 * displays a dashboard of things for the user to do.
 */
package info.northcarolinawaterfalls.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.json.JSONArray;
import org.json.JSONException;

import info.northcarolinawaterfalls.app.ExpansionDownloaderDialogFragment.ExpansionDownloadDialogListener;

public class MainActivity extends SherlockFragmentActivity implements
        ExpansionDownloadDialogListener, OnCancelListener {
    
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "AppSettingsPreferences";
    private static final String USER_PREF_SHARED_WF = "SharedWaterfalls";
    private static final String USER_PREF_PAUSE_DOWNLOAD = "UserPrefPauseDownload";
    private static final String USER_PREF_SKIP_PLAY_SERVICES = "UserPrefSkipPlayServices";
    private static final String APP_PREF_LAST_KNOWN_DB_VERSION = "LastKnownDBVersion";
    private static AttrDatabase mDb = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDb = new AttrDatabase(this);  // TODO: Make sure this isn't leaky as a sieve
        
        SharedPreferences appPrefs = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        int lastKnownDbVersion = appPrefs.getInt(APP_PREF_LAST_KNOWN_DB_VERSION, 0);
        SQLiteDatabase db = mDb.getReadableDatabase();
        int actualDbVersion = db.getVersion();

        Log.d(TAG, "Last known database version is " + lastKnownDbVersion + ". Actual version is " + actualDbVersion);

        // Save actual last version
        if(lastKnownDbVersion != actualDbVersion){
            SharedPreferences.Editor editor = appPrefs.edit();
            editor.putInt(APP_PREF_LAST_KNOWN_DB_VERSION, actualDbVersion);
            editor.commit();
        }

        // This logic seems to simple to possibly be correct
        if(actualDbVersion > 1 && lastKnownDbVersion < actualDbVersion){
            // We've been upgraded. Copy shared id's into db for quick searching
            Log.d(TAG, "Database has been upgraded. Copying shared id's to db...");
            onDatabaseUpgrade();
        }
        
        // We're done with it.
        mDb.close();

    }

    @Override
    protected void onResume(){
        super.onResume();
        if(googlePlayServicesAvailable()){
            // Determine if the expansion files have been downloaded
            // Do this in the else of the above check because it's irrelevant if
            // Play Services aren't available.
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            boolean userPrefPauseDownload = settings.getBoolean(USER_PREF_PAUSE_DOWNLOAD, false);
            if(!ExpansionDownloaderService.expansionFilesDownloaded(this)){
                // Warn user about offline maps.
                if(!userPrefPauseDownload){
                    DialogFragment expansionDownloaderDialogFragment = new ExpansionDownloaderDialogFragment();
                    expansionDownloaderDialogFragment.show(getSupportFragmentManager(), "expansionDownloaderDialogFragment");
                } else {
                    // TODO: We should probably not show this repeatedly.
                    Toast.makeText(
                            getApplicationContext(),
                            "Offline maps not available. Open App Info to download.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        } else {
            // Disable Search by Location button
            Button searchLocationButton = (Button) findViewById(R.id.main_btn_search_location);
            if(searchLocationButton != null){
                searchLocationButton.setClickable(false);
                searchLocationButton.setEnabled(false);
            }
        }
    }

    // Define a request code to send to Google Play services
    // This code is returned in Activity.onActivityResult
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    
    // Define a DialogFragment that displays the error dialog generated in showErrorDialog
    public static class ErrorDialogFragment extends DialogFragment {
        
        // Global field to contain the error dialog
        private Dialog mDialog;
        
        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }
        
        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }
        
        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
   
    // Handle results returned to the FragmentActivity by Google Play services
    // error checking/correcting routine.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Decide what to do based on the original request code
        Log.d(TAG, "Inside onActivityResult.");
        switch (requestCode) {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST :
                // If the result code is Activity.RESULT_OK, we should
                // have Play Services available.
                switch (resultCode) {
                    case Activity.RESULT_OK :
                        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean(USER_PREF_SKIP_PLAY_SERVICES, false);
                        editor.commit();
                        break;
                }
        }
     }
   
    // GooglePlayServices errorDialog onCancel interface
    @Override
    public void onCancel(DialogInterface dialog) {
        // User chose not to install Play Services. This means no map tabs.
        // Save preference.
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(USER_PREF_SKIP_PLAY_SERVICES, true);
        editor.commit();
    }
    
    private boolean googlePlayServicesAvailable() {
        // Check that Google Play services is available
        // Unfortunately, this has the side effect of creating an
        // error dialog, so it's not just a simple check.
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d(TAG, "Google Play services is available.");
            editor.putBoolean(USER_PREF_SKIP_PLAY_SERVICES, false);
            editor.commit();
            return true;
        } else if(!settings.getBoolean(USER_PREF_SKIP_PLAY_SERVICES, false)) {
            // Google Play services was not available for some reason
            // Disable map fragments until this is resolved.
            Log.d(TAG, "Google Play services is NOT available.");
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    resultCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            editor.putBoolean(USER_PREF_SKIP_PLAY_SERVICES, true);
            editor.commit();
            if (errorDialog != null) {
                // Play Services can sometimes provide a dialog with options
                // for the user to correct the issue.
                // Create a new DialogFragment for the error dialog.
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(getSupportFragmentManager(), "Maps");
                // If I'm reading the docs correctly, then if the user opts to install
                // Play Services, startActivityForResult will be called when done, and
                // return through this activity's onActivityResult.
            }
            return false;
        }
        return false;
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
        Intent intent = new Intent(this, AppInfoActivity.class);
        startActivity(intent);
    }
    
    private void startAppInfoActivity(){
        Intent intent = new Intent(this, AppInfoActivity.class);
        startActivity(intent);
    }
    
    // ExpansionDownloadDialogListener interface methods
    public void onExpansionDownloaderDialogPositiveClick(DialogFragment dialog){
        Log.d(TAG, "Download affirmative.");
        startAppInfoActivity();
    }
    
    public void onExpansionDownloaderDialogNegativeClick(DialogFragment dialog){
        Toast.makeText(getApplicationContext(), "Skipped downloading offline maps.", Toast.LENGTH_LONG).show();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(USER_PREF_PAUSE_DOWNLOAD, true);
        editor.commit();
    }
    
    // PlayServicesCheckDialogListener interface methods
    public void onPlayServicesDialogPositiveClick(DialogFragment dialog){
        Log.d(TAG, "Play Services Check affirmative.");
        startAppInfoActivity();
    }
    
    public void onPlayServicesDialogNegativeClick(DialogFragment dialog){
        // OK. Fine.
    }

    public void onDatabaseUpgrade() {
        Log.d(TAG, "Inside database onUpgrade.");
        
        // Re-load shared waterfall preferences into the newly-created DB
        JSONArray sharedWfs = new JSONArray();
        SharedPreferences appPrefs = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        
        // Load existing shares.
        String sharedWfJson = appPrefs.getString(USER_PREF_SHARED_WF, "[]");
        if(sharedWfJson != null){
            try {
                sharedWfs = new JSONArray(sharedWfJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // Copy values back to database for quick searching.
        // Someone please chastise me for doing this on the main thread
        // in an ugly loop.
        for (int i = 0; i < sharedWfs.length(); i++) {
            String table = "waterfalls";
            int waterfallId;
            try {
                waterfallId = sharedWfs.getInt(i);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                continue;
            }
            ContentValues values = new ContentValues(1);
            values.put("shared", 1);
            String whereClause = "_id = ?";
            String[] whereArgs = {String.valueOf(waterfallId)};
            int rowsUpdated = mDb.update(table, values, whereClause, whereArgs);
            Log.d(TAG, "Share saved to DB; id: " + waterfallId);
        }
        Log.d(TAG, "Copied existing shares back to database.");
    }
}
