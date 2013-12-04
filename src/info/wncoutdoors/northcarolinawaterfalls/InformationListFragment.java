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
import com.commonsware.cwac.loaderex.acl.SQLiteCursorLoader;

import info.wncoutdoors.northcarolinawaterfalls.grid.ImageLoader;
import info.wncoutdoors.northcarolinawaterfalls.grid.ScaleImageView;

public class InformationListFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "InformationListFragment";
    
    private static AttrDatabase db = null;
    private SQLiteCursorLoader cursorLoader = null;
    private OnWaterfallQueryListener sQueryListener; // Listener for loader callbacks
    
    private int mImageHeight;    
    private ImageLoader mImgLoader;
    
    public final static String IMAGE_FN = "info.northcarolinawaterfalls.IMAGE_FN";
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View waterfallInformationFragmentView = inflater.inflate(R.layout.fragment_information_list, container, false);        
        return waterfallInformationFragmentView;
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
            String name = cursor.getString(AttrDatabase.COLUMNS.indexOf("name"));
            String fileName = cursor.getString(AttrDatabase.COLUMNS.indexOf("photo_filename"));
            String[] fnParts = fileName.split("\\.(?=[^\\.]+$)");

            final String image_fn = fnParts[0];
            final String wf_name = name;

            // Display image in the image view.
            ImageView mainImageContainer = (ImageView) getView().findViewById(R.id.information_waterfall_image);
            mImgLoader.displayImage(fnParts[0], mainImageContainer, getActivity(), mImageHeight, mImageHeight);
            
            // Add click listener for fullscreen view
            mainImageContainer.setOnClickListener(new OnClickListener() {
                public void onClick(View v){
                    Intent fullScreenIntent = new Intent(v.getContext(), FullScreenImageActivity.class);
                    fullScreenIntent.putExtra(IMAGE_FN, image_fn);
                    fullScreenIntent.putExtra(WF_NAME, wf_name);
                    InformationListFragment.this.startActivity(fullScreenIntent);
                }
             });

            // Next load the text view containing attributes.
            // TODO: Omit any which are null.
            TextView description = (TextView) getView().findViewById(R.id.information_content_description);
            description.setText(Html.fromHtml(
                cursor.getString(AttrDatabase.COLUMNS.indexOf("description"))).toString());

            String[] hikeDetailList = {
                    "• " + cursor.getString(AttrDatabase.COLUMNS.indexOf("trail_difficulty")),
                    "• " + cursor.getString(AttrDatabase.COLUMNS.indexOf("trail_tread")),
                    "• " + cursor.getString(AttrDatabase.COLUMNS.indexOf("trail_climb")),
                    "• Length: " + cursor.getString(AttrDatabase.COLUMNS.indexOf("trail_length")) + "mi",
                    "• Lowest Elevation: " + cursor.getString(AttrDatabase.COLUMNS.indexOf("trail_elevationlow")) + " ft",
                    "• Highest Elevation: " + cursor.getString(AttrDatabase.COLUMNS.indexOf("trail_elevationhigh")) + " ft",
                    "• Total Climb: " + cursor.getString(AttrDatabase.COLUMNS.indexOf("trail_elevationgain")) + " ft",
                    "• Configuration: " + cursor.getString(AttrDatabase.COLUMNS.indexOf("trail_configuration"))
            };
            String hikeDetailTxt = TextUtils.join("\n", hikeDetailList);
            TextView hikeDetails = (TextView) getView().findViewById(R.id.information_content_hike_details);
            hikeDetails.setText(hikeDetailTxt);
            
            TextView hikeDescription = (TextView) getView().findViewById(R.id.information_content_hike_description);
            hikeDescription.setText(Html.fromHtml(
                cursor.getString(AttrDatabase.COLUMNS.indexOf("trail_directions"))).toString());
            
            String[] detailList = {
                    "• Height: " + cursor.getString(AttrDatabase.COLUMNS.indexOf("height")),
                    "• Stream: " + cursor.getString(AttrDatabase.COLUMNS.indexOf("stream")),
                    "• Landowner: " + cursor.getString(AttrDatabase.COLUMNS.indexOf("landowner")),
                    "• Botttom Elevation: " + cursor.getString(AttrDatabase.COLUMNS.indexOf("elevation")) + " ft"
            };
            String detailTxt = TextUtils.join("\n", detailList);
            
            TextView waterfallDetails = (TextView) getView().findViewById(R.id.information_content_details);
            waterfallDetails.setText(detailTxt);
            
            TextView drivingDirections = (TextView) getView().findViewById(R.id.information_content_directions);
            drivingDirections.setText(Html.fromHtml(
                cursor.getString(AttrDatabase.COLUMNS.indexOf("directions"))).toString());
            
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //TODO: Set to null to prevent memory leaks
        /*mAdapter.changeCursor(cursor);*/
    }
    
}
