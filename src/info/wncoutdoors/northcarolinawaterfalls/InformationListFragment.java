package info.wncoutdoors.northcarolinawaterfalls;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragment;
import com.commonsware.cwac.loaderex.acl.SQLiteCursorLoader;

public class InformationListFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "InformationListFragment";
    
    private static AttrDatabase db=null;
    private SQLiteCursorLoader cursorLoader = null;
    private OnWaterfallQueryListener sQueryListener; // Listener for loader callbacks
    
    // Like the ResultsActivity, define an interface for listening to requests for queries
    // No arguments, just needs to know the sql to run.
    public interface OnWaterfallQueryListener{
        public Bundle onWaterfallQuery();
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
    }
    
    // LoaderManager.LoaderCallbacks<Cursor> methods
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Bundle qBundle = sQueryListener.onWaterfallQuery();
        
        // Get the query from our parent activity and pass it to the loader, which will execute it
        cursorLoader = new SQLiteCursorLoader(
                getActivity(), db, qBundle.getString("query"), qBundle.getStringArray("args"));
        Log.d(TAG, "We have created a cursorLoader.");
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d(TAG, "Inside onLoadFinished");
        Log.d(TAG, "Cursor returned " + cursor.getCount() + " rows.");
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /*mAdapter.changeCursor(cursor);*/
    }
    
}
