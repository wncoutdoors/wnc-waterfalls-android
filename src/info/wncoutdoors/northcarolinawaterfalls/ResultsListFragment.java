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
import android.widget.AdapterView;
import android.widget.GridView;

import com.actionbarsherlock.app.SherlockFragment;

//https://github.com/commonsguy/cwac-loaderex/blob/master/demo/src/com/commonsware/cwac/loaderex/demo/ConstantsBrowser.java
import com.commonsware.cwac.loaderex.acl.SQLiteCursorLoader;

import info.wncoutdoors.northcarolinawaterfalls.grid.GridAdapter;

public class ResultsListFragment 
       extends SherlockFragment
       implements LoaderManager.LoaderCallbacks<Cursor>,
                  AdapterView.OnItemClickListener {

    private static final String TAG = "ResultsListFragment";
    private OnWaterfallQueryListener sQueryListener; // Listener for loader callbacks
    private OnWaterfallSelectListener sSelectListener; // Listener for user waterfall selections

    private static AttrDatabase db=null;
    private SQLiteCursorLoader cursorLoader = null;

    // Adapter used to display the Grid's data.
    private GridAdapter mGridViewAdapter = null;

    // Grid view used to display the data
    private GridView mGridView = null;
    private String[] fromCols = {"name"};
    private int[] toViews = {R.id.grid_text_view};

    // Interface for listening to requests for queries
    // No arguments, just needs to know the sql to run.
    public interface OnWaterfallQueryListener{
        public Bundle onWaterfallQuery();
    }
    
    // Interface for listening to waterfall selections
    public interface OnWaterfallSelectListener{
        public void onWaterfallSelected(long waterfallId);
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

        // And the select listener interface
        try {
            sSelectListener = (OnWaterfallSelectListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnWaterfallSelectListener");
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
        mGridView = (GridView) locationSearchFragmentView.findViewById(R.id.ResultsListGrid);       
        mGridViewAdapter = new GridAdapter(
                getActivity(), R.layout.grid_element, fromCols, toViews);
        Log.d(TAG, "GridAdapter created.");
        mGridView.setAdapter(mGridViewAdapter);
        Log.d(TAG, "StaggeredGridView adapter set.");
        mGridView.setOnItemClickListener(this);
        mGridViewAdapter.notifyDataSetChanged();

        // TODO: Display "no results found" if query returns none
        return locationSearchFragmentView;
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
        mGridViewAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /*mAdapter.changeCursor(cursor);*/
    }

    // Listener for user selections
    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id){
        // id is the db_id field
        sSelectListener.onWaterfallSelected(id);
    }

    @Override
    public void onDestroy() {
      super.onDestroy();
      db.close();
    }
}
