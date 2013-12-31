package info.wncoutdoors.northcarolinawaterfalls;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.commonsware.cwac.loaderex.acl.SQLiteCursorLoader;

import java.util.ArrayList;

import info.northcarolinawaterfalls.R;
import info.wncoutdoors.northcarolinawaterfalls.grid.ImageLoader;
import info.wncoutdoors.northcarolinawaterfalls.grid.ScaleImageView;

public class InformationListFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "InformationListFragment";
    
    private static AttrDatabase db = null;
    private SQLiteCursorLoader cursorLoader = null;
    private OnWaterfallQueryListener sQueryListener; // Listener for loader callbacks
    
    private Long mWaterfallId;
    private String mWaterfallName;
    
    private int mImageHeight;    
    private ImageLoader mImgLoader;
    
    private ShareActionProvider mShareActionProvider;
    
    public final static String IMAGE_FN = "info.northcarolinawaterfalls.IMAGE_FN";
    public final static String WF_ID = "info.northcarolinawaterfalls.WF_ID";
    public final static String WF_NAME = "info.northcarolinawaterfalls.WF_NAME";

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
        db = new AttrDatabase(getActivity());

        // Get our loader manager, and initialize the
        // query based on the containing Activity's searchMode
        getLoaderManager().initLoader(0, null, this);
        Log.d(TAG, "Loader manager onCreate finished.");

        // Create an image loader. Turn off caching.
        mImgLoader = new ImageLoader(getActivity(), false);

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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.information_list_actions, menu);
        MenuItem item = menu.findItem(R.id.menu_item_waterfall_share);
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();
        setupShareIntent(); // In case the loader is already back
    }

    // LoaderManager.LoaderCallbacks<Cursor> methods
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Bundle qBundle = sQueryListener.onWaterfallQuery();
        
        // Get the query from our parent activity and pass it to the loader, which will execute it
        Log.d(TAG, "Query was: " + qBundle.getString("query"));
        Log.d(TAG, "Args were: " + qBundle.getStringArray("args"));
        cursorLoader = new SQLiteCursorLoader(
                getActivity(), db, qBundle.getString("query"), qBundle.getStringArray("args"));
        Log.d(TAG, "We have created a cursorLoader.");
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d(TAG, "Inside onLoadFinished");        
        if(cursor.moveToFirst()){
            Log.d(TAG, "Cursor returned " + cursor.getCount() + " rows.");
            
            // Load up the views
            TextView title = (TextView) getView().findViewById(R.id.information_waterfall_name);
            title.setText(cursor.getString(AttrDatabase.COLUMNS.indexOf("name")));

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
                    new String[] {"• Botttom Elevation: ", "elevation", " ft"}
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
        /*mAdapter.changeCursor(cursor);*/
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
            }
        }
    }

    private Intent getDefaultShareIntent(){
        // Create intent and add waterfall url on NorthCarolinaWaterfalls.info
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Waterfall Shared from NorthCarolinaWaterfalls.info");
        
        // Format the share url
        String waterfallUrl = "http://www.northcarolinawaterfalls.info/waterfall/" +
            mWaterfallId + "/" + mWaterfallName.replaceAll("\\s", "_");
        intent.putExtra(Intent.EXTRA_TEXT, waterfallUrl);
        return intent;
    }
    
}
