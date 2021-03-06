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
 * InformationListFragment.java
 * Fragment which displays the main waterfall information in a Details tab.
 */
package info.wncwaterfalls.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.commonsware.cwac.loaderex.acl.SQLiteCursorLoader;

import info.wncwaterfalls.app.R;
import info.wncwaterfalls.app.grid.ImageLoader;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class InformationListFragment extends Fragment 
        implements LoaderManager.LoaderCallbacks<Cursor>, ShareActionProvider.OnShareTargetSelectedListener {
    private static final String TAG = "InformationListFragment";
    
    private static AttrDatabase mDb = null;
    private SQLiteCursorLoader cursorLoader = null;
    private OnWaterfallQueryListener sQueryListener; // Listener for loader callbacks
    
    private Long mWaterfallId;
    private String mWaterfallName;
    
    private int mImageHeight;    
    private ImageLoader mImgLoader;
    
    private ShareActionProvider mShareActionProvider;
    
    public static final String APP_PREFS_NAME = "AppSettingsPreferences";
    public static final String USER_PREF_SHARED_WF = "SharedWaterfalls";

    public final static String IMAGE_FN = "info.wncwaterfalls.app.IMAGE_FN";
    public final static String WF_ID = "info.wncwaterfalls.app.WF_ID";
    public final static String WF_NAME = "info.wncwaterfalls.app.WF_NAME";

    // Like the ResultsActivity, define an interface for listening to requests for queries
    // No arguments, just needs to know the sql to run.
    public interface OnWaterfallQueryListener{
        public Bundle onWaterfallQuery();
    }
    
    public interface OnWaterfallMapQueryListener{
        public Bundle onWaterfallMapQuery();
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // Make sure the containing activity implements the search listener interface
        try {
            sQueryListener = (OnWaterfallQueryListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnWaterfallQueryListener");
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);       
        mDb = new AttrDatabase(getActivity());

        // Get our loader manager, and initialize the
        // query based on the containing Activity's searchMode
        getLoaderManager().initLoader(0, null, this);

        // Create an image loader. Turn off caching.
        mImgLoader = new ImageLoader(getActivity(), false); // TODO: Memory leak? getApplicationContext()?

        // Figure out how high our image is going to be
        int iDisplayHeight = getResources().getDisplayMetrics().heightPixels;
        mImageHeight = iDisplayHeight / 2;
        
        // Turn on our options menu
        setHasOptionsMenu(true);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View waterfallInformationFragmentView = inflater.inflate(R.layout.fragment_information_list, container, false); 
        return waterfallInformationFragmentView;
    }

    @Override
    public void onPause(){
        // TODO: remove bitmap from image view to free up some memory? 
        // Would need to be re-displayed in onResume() if we come back to this activity.
        // Note: looks like the next activity's onCreate, onResume get called before this,
        // so it can be rendered and displayed while this one is still visible,
        // so that might not help much...
        super.onPause();
    }
    
    @Override
    public void onStop(){
        super.onStop();
    }
    
    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.information_list_actions, menu);
        MenuItem item = menu.findItem(R.id.menu_item_waterfall_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        setupShareIntent(); // In case the loader is already back
    }

    // LoaderManager.LoaderCallbacks<Cursor> methods
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Bundle qBundle = sQueryListener.onWaterfallQuery();
        
        // Get the query from our parent activity and pass it to the loader, which will execute it
        cursorLoader = new SQLiteCursorLoader(
                getActivity(), mDb, qBundle.getString("query"), qBundle.getStringArray("args"));
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if(cursor.moveToFirst()){
            String name = cursor.getString(AttrDatabase.COLUMNS.indexOf("name"));
            
            // Update the Activity's title
            getActivity().setTitle(name);

            // Load up the views
            // First load the image view by using the ImageLoader class
            // Determine the photo's file name
            mWaterfallId = cursor.getLong(AttrDatabase.COLUMNS.indexOf("_id"));
            mWaterfallName = cursor.getString(AttrDatabase.COLUMNS.indexOf("name"));
            String fileName = cursor.getString(AttrDatabase.COLUMNS.indexOf("photo_filename"));
            String[] fnParts = fileName.split("\\.(?=[^\\.]+$)");

            final String image_fn = fnParts[0];
            final String wf_name = mWaterfallName;
            final Long wf_id = mWaterfallId;

            // Display image in the image view.
            ImageView mainImageContainer = (ImageView) getView().findViewById(R.id.information_waterfall_image);
            mImgLoader.displayImage(fnParts[0], mainImageContainer, getActivity(), mImageHeight, mImageHeight);
            
            // Add click listener for fullscreen view
            mainImageContainer.setOnClickListener(new OnClickListener() {
                public void onClick(View v){
                    Intent fullScreenIntent = new Intent(v.getContext(), FullScreenImageActivity.class);
                    fullScreenIntent.putExtra(IMAGE_FN, image_fn);
                    fullScreenIntent.putExtra(WF_NAME, wf_name);
                    fullScreenIntent.putExtra(WF_ID, wf_id);
                    InformationListFragment.this.startActivity(fullScreenIntent);
                }
             });

            // Next load the text view containing attributes.
            TextView description = (TextView) getView().findViewById(R.id.information_content_description);
            description.setText(Html.fromHtml(
                cursor.getString(AttrDatabase.COLUMNS.indexOf("description"))).toString());
            
            // Prefix, db key, suffix
            String[][] hikeDetailElements = new String[][]{
                new String[] {"• ", "trail_difficulty", ""},
                new String[] {"• ", "trail_tread", ""},
                new String[] {"• ", "trail_climb", ""},
                new String[] {"• Length: ", "trail_length", " mi"},
                new String[] {"• Lowest Elevation: ", "trail_elevationlow", " ft"},
                new String[] {"• Highest Elevation: ", "trail_elevationhigh", " ft"},
                new String[] {"• Total Climb: ", "trail_elevationgain", " ft"},
                new String[] {"• Configuration: ", "trail_configuration", ""}
            };
            
            // Filter out blank values in the db
            ArrayList<String> hikeDetailList = new ArrayList<String>();
            for(String[] element: hikeDetailElements){
                String elementValue = cursor.getString(AttrDatabase.COLUMNS.indexOf(element[1]));
                if(elementValue != null && elementValue.length() > 0){
                    hikeDetailList.add(element[0] + elementValue + element[2]);
                }
            }
            
            // Stringifiy the ones that made it through
            String hikeDetailTxt = TextUtils.join("\n", hikeDetailList);
            TextView hikeDetails = (TextView) getView().findViewById(R.id.information_content_hike_details);
            hikeDetails.setText(hikeDetailTxt);
            
            TextView hikeDescription = (TextView) getView().findViewById(R.id.information_content_hike_description);
            hikeDescription.setText(Html.fromHtml(
                cursor.getString(AttrDatabase.COLUMNS.indexOf("trail_directions"))).toString());
            
            // Repeat for waterfall attributes...
            String[][] detailElements = new String[][]{
                    new String[] {"• Height: ", "height", ""},
                    new String[] {"• Stream: ", "stream", ""},
                    new String[] {"• Landowner: ", "landowner", ""},
                    new String[] {"• Bottom Elevation: ", "elevation", " ft"}
            };
            
            // Filter out blank values in the db
            ArrayList<String> detailList = new ArrayList<String>();
            for(String[] element: detailElements){
                String elementValue = cursor.getString(AttrDatabase.COLUMNS.indexOf(element[1]));
                if(elementValue != null && elementValue.length() > 0){
                    detailList.add(element[0] + elementValue + element[2]);
                }
            }

            String detailTxt = TextUtils.join("\n", detailList);
            
            TextView waterfallDetails = (TextView) getView().findViewById(R.id.information_content_details);
            waterfallDetails.setText(detailTxt);
            
            TextView drivingDirections = (TextView) getView().findViewById(R.id.information_content_directions);
            drivingDirections.setText(Html.fromHtml(
                cursor.getString(AttrDatabase.COLUMNS.indexOf("directions"))).toString());
            
            setupShareIntent(); // In case the options menu is already created
        
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //TODO: Set to null to prevent memory leaks
        //mAdapter.changeCursor(cursor);
    }

    private void setupShareIntent(){
        // If we have the intent, the waterfall ID, and the share action provider,
        // then set the share intent. This is called by both onLoadFinished and
        // onCreateOptionsMenu because, since the loader is async, they seem to race each other,
        // and it's impossible to know which will finish first.
        if(mWaterfallId != null && mWaterfallName != null && mShareActionProvider != null){
            Intent shareIntent = getDefaultShareIntent();
            if(shareIntent != null){
                mShareActionProvider.setShareIntent(shareIntent);
                mShareActionProvider.setOnShareTargetSelectedListener(this);
            }
        }
    }

    private Intent getDefaultShareIntent(){
        // Create intent and add waterfall url on NorthCarolinaWaterfalls.info
        // TODO: Change when website url changes.
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Waterfall Shared from NorthCarolinaWaterfalls.info");
        
        // Format the share url
        // TODO: Change when website url changes.
        String waterfallUrl = "http://www.northcarolinawaterfalls.info/waterfall/" +
            mWaterfallId + "/" + mWaterfallName.replaceAll("\\s", "_");
        intent.putExtra(Intent.EXTRA_TEXT, waterfallUrl);
        return intent;
    }
    
    public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
        // Shared. Save our share to preferences & to the db
        // Create a new array to hold the prefs.
        // TODO: Remove copy-pasta (duplicated within FullScreenImageActivity) 
        JSONArray sharedWfs = new JSONArray();

        SharedPreferences appPrefs = getActivity().getSharedPreferences(APP_PREFS_NAME, 0);
        
        // Load existing shares.
        String sharedWfJson = appPrefs.getString(USER_PREF_SHARED_WF, "[]");
        if(sharedWfJson != null){
            try {
                sharedWfs = new JSONArray(sharedWfJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        // Add this id and commit
        sharedWfs.put(mWaterfallId);

        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putString(USER_PREF_SHARED_WF, sharedWfs.toString());
        editor.commit();

        // Now update the database for quick searching
        String table = "waterfalls";
        ContentValues values = new ContentValues(1);
        values.put("shared", 1);
        String whereClause = "_id = ?";
        String[] whereArgs = {String.valueOf(mWaterfallId)};
        int rowsUpdated = mDb.update(table, values, whereClause, whereArgs);
        return false;
    }
}
