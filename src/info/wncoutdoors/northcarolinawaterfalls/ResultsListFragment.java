package info.wncoutdoors.northcarolinawaterfalls;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;

//https://github.com/commonsguy/cwac-loaderex/blob/master/demo/src/com/commonsware/cwac/loaderex/demo/ConstantsBrowser.java
import com.commonsware.cwac.loaderex.acl.SQLiteCursorLoader;
import com.origamilabs.library.views.StaggeredGridView;

import info.wncoutdoors.northcarolinawaterfalls.SearchLocationFragment.OnLocationSearchListener;
import info.wncoutdoors.northcarolinawaterfalls.staggeredGrid.GridAdapter;

public class ResultsListFragment 
       extends SherlockFragment
       implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "ResultsListFragment";
    private OnWaterfallQueryListener sQueryListener;

    private static AttrDatabase db=null;
    private SQLiteCursorLoader cursorLoader = null;

    // Adapter used to display the Grid's data.
    private GridAdapter mGridViewAdapter = null;

    // Grid view used to display the data
    private StaggeredGridView mGridView = null;
    private String[] fromCols = {"name"};
    private int[] toViews = {R.id.staggered_text_view};

    // Interface for listening to requests for queries
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View locationSearchFragmentView = inflater.inflate(
                R.layout.fragment_results_list, container, false);
        mGridView = (StaggeredGridView) locationSearchFragmentView.findViewById(R.id.ResultsListStaggeredGrid);       
        mGridViewAdapter = new GridAdapter(
                getActivity(), R.layout.staggered_grid_element, fromCols, toViews);
        Log.d(TAG, "GridAdapter created.");
        mGridView.setAdapter(mGridViewAdapter);
        Log.d(TAG, "StaggeredGridView adapter set.");
        mGridViewAdapter.notifyDataSetChanged();        
        return locationSearchFragmentView;
    }

    // LoaderManager.LoaderCallbacks<Cursor> methods
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // TODO: Add the where clause
        Bundle qBundle = sQueryListener.onWaterfallQuery();
        cursorLoader = new SQLiteCursorLoader(
                getActivity(), db, qBundle.getString("query"), qBundle.getStringArray("args"));
        Log.d(TAG, "We have created a cursorLoader.");
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d(TAG, "Inside onLoadFinished");
        Log.d(TAG, "Cursor returned " + cursor.getCount() + " rows.");
        mGridViewAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /*mAdapter.changeCursor(cursor);*/
    }

    @Override
    public void onDestroy() {
      super.onDestroy();
      db.close();
    }
}
