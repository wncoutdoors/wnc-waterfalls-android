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
 * ResultsListFragment.java
 * Fragment which displays the results of a user's query in a scrolling
 * list with photo thumbnails.
 */
package info.northcarolinawaterfalls.app;

import android.app.Activity;
import android.content.Context;
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
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

//https://github.com/commonsguy/cwac-loaderex/blob/master/demo/src/com/commonsware/cwac/loaderex/demo/ConstantsBrowser.java
import com.commonsware.cwac.loaderex.acl.SQLiteCursorLoader;

import info.northcarolinawaterfalls.app.R;
import info.northcarolinawaterfalls.app.grid.GridAdapter;

public class ResultsListFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {

    private static final String TAG = "ResultsListFragment";
    private OnWaterfallQueryListener sQueryListener; // Listener for loader callbacks
    private OnWaterfallSelectListener sSelectListener; // Listener for user waterfall selections

    private static AttrDatabase db = null;
    private SQLiteCursorLoader cursorLoader = null;

    // Adapter used to display the Grid's data.
    private GridAdapter mGridViewAdapter = null;

    // Grid view used to display the data
    private GridView mGridView = null;
    private String[] fromCols = {"name"};
    private int[] toViews = {R.id.grid_text_view};
    private int mNumColumns;
    private int mImageWidth;

    // TODO: Move these outside this class
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
        Log.d(TAG, "Loader manager created.");
        
        // Determine how wide we need our columns to be.
        // Simple adapatation: if we have less than 600 pixels, use
        // 2 columns; otherwise use 3. We could probably make this better.
        int iDisplayWidth = getResources().getDisplayMetrics().widthPixels;
        if(iDisplayWidth < 600){
            mNumColumns = 2;
        } else {
            mNumColumns = 3;
        }
        // Set image width to display width minus 10 for each column (for 5 padding on each side)
        // Discard any fraction
        mImageWidth = (iDisplayWidth - (10 * mNumColumns)) / mNumColumns;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View locationSearchFragmentView = inflater.inflate(
                R.layout.fragment_results_list, container, false);
        mGridView = (GridView) locationSearchFragmentView.findViewById(R.id.ResultsListGrid);
        mGridView.setNumColumns(mNumColumns); // 2 or 3
        mGridViewAdapter = new GridAdapter(
                getActivity(), R.layout.grid_element, fromCols, toViews);
        mGridViewAdapter.setImageWidth(mImageWidth); // Screen width / num columns minus padding
        Log.d(TAG, "GridAdapter created.");
        mGridView.setAdapter(mGridViewAdapter);
        Log.d(TAG, "GridView adapter set.");
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
        int count = cursor.getCount();
        Log.d(TAG, "Cursor returned " + count + " rows.");
        if(count == 0) {
            Log.d(TAG, "Let's have a toast.");
            Context context = getActivity();
            CharSequence text = "No results found for your search.";
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show(); 
        }
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
