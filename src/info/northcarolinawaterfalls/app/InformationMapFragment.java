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
 * InformationmapFragment.java
 * Fragment which controls the waterfall's map tab in the Information activity.
 * Allows offline maps to be displayed.
 */
package info.northcarolinawaterfalls.app;

import android.app.Activity;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider;
import com.commonsware.cwac.loaderex.acl.SQLiteCursorLoader;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import java.io.File;

import info.northcarolinawaterfalls.app.R;
import info.northcarolinawaterfalls.app.InformationListFragment.OnWaterfallQueryListener;

public class InformationMapFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "InformationMapFragment";
    private static final int WATERFALL_QUERY_LOADER = 0;
    
    private MapView mMapView;
    private static AttrDatabase mAttrDb = null;
    private MBTilesDatabase mTilesDB = null;
    private SQLiteCursorLoader cursorLoader = null;
    private MapBoxOfflineTileProvider mMapBoxTileProvider;
    private TileOverlay mMBTilesTileOverlay;
    private Menu mOptionsMenu;
    private boolean mOptionsMenuCreated;
    private boolean mOfflineMapCreated;

    private double mLat;
    private double mLon;
    private String mName;
    private String mMapName;
    
    private OnWaterfallQueryListener sQueryListener; // Listener for loader callbacks
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // Make sure the containing activity implements the search listener interface
        try {
            sQueryListener = (OnWaterfallQueryListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement the correct query listeners.");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Inside InformationMapFragment onCreate()");
        super.onCreate(savedInstanceState);       
        mAttrDb = new AttrDatabase(getActivity());  // TODO: Memory leak? use app context?
        
        // Get our loader manager, and initialize queries.
        getLoaderManager().initLoader(WATERFALL_QUERY_LOADER, null, this); // Waterfall
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        Log.d(TAG, "Inside InformationMapFragment onCreateView()");
        View view = inflater.inflate(R.layout.fragment_information_map, container, false);
        mMapView = (MapView) view.findViewById(R.id.information_map_view);
        mMapView.onCreate(savedInstanceState);
        int errorCode = MapsInitializer.initialize(getActivity()); // TODO: Check code?
        return view;
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d(TAG, "Creating options menu.");
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.information_map_actions, menu);
    }
    
    @Override
    public void onPrepareOptionsMenu(Menu menu){
        mOptionsMenuCreated = true;
        mOptionsMenu = menu;
        enableOfflineMapToggle();
    }
    
    @Override
    public void onStart(){
        super.onStart();
        
        Log.d(TAG, "Inside InformationMapFragment onStart()");
        if(mMapView == null){
            mMapView = (MapView) getActivity().findViewById(R.id.information_map_view);
            setupMap();
        }
        // MapView doesn't have onStart, so no need to route there.
    }
    
    @Override
    public void onResume() {
        Log.d(TAG, "Inside InformationMapFragment onResume()");
        super.onResume();
        if (null != mMapView){
            mMapView.onResume();
        }
    }
    
    private boolean setMapTypeIfUnchecked(MenuItem item, int newType){
        GoogleMap googleMap = mMapView.getMap();
        if(!item.isChecked()){
            googleMap.setMapType(newType);
            item.setChecked(true);
        }
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){           
            case R.id.menu_item_map_type_street:
                return setMapTypeIfUnchecked(item, GoogleMap.MAP_TYPE_NORMAL);

            case R.id.menu_item_map_type_terrain:
                return setMapTypeIfUnchecked(item, GoogleMap.MAP_TYPE_TERRAIN);

            case R.id.menu_item_map_type_none:
                return setMapTypeIfUnchecked(item, GoogleMap.MAP_TYPE_NONE);

            case R.id.menu_item_map_show_overlay:
                if(mMBTilesTileOverlay != null){
                    mMBTilesTileOverlay.setVisible(!mMBTilesTileOverlay.isVisible());
                    item.setChecked(!item.isChecked());
                }
                return true;

            case R.id.menu_item_map_tracking:
                GoogleMap map = mMapView.getMap();
                map.setMyLocationEnabled(!map.isMyLocationEnabled());
                item.setChecked(!item.isChecked());
                return true;

            default:
                Log.d(TAG, "Something else selected from menu!");
                return super.onOptionsItemSelected(item);
        }
    }
    
    // Route fragment lifecycle events to MapView
    @Override
    public void onPause() {
        Log.d(TAG, "Inside InformationMapFragment onPause()");       
        super.onPause();
    }

    @Override
    public void onStop(){
        Log.d(TAG, "Inside InformationMapFragment onStop()");
        mTilesDB = null;
        if(null != mMBTilesTileOverlay){
            mMBTilesTileOverlay.clearTileCache();
            mMBTilesTileOverlay = null;
        }
        
        if(null != mMapBoxTileProvider){
            mMapBoxTileProvider.close();
            mMapBoxTileProvider = null;
        }
        
        GoogleMap map = mMapView.getMap();
        if(map != null){
            map.clear();
        }
        
        if(null != mMapView){
            mMapView.onPause();
            mMapView = null;
        }
        super.onStop();
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "Inside InformationMapFragment onDestroy()");
        super.onDestroy();
        if(null != mMapView){
            mMapView.onDestroy();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != mMapView){
            mMapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (null != mMapView){
            mMapView.onLowMemory();
        }
    }
    
    private void createOfflineTileProvider(){
        File tilesDBFile = mTilesDB.getDBFile();
        Log.d(TAG, "Adding custom tile layer to map.");
        
        MapBoxOfflineTileProvider mMapBoxTileProvider = new MapBoxOfflineTileProvider(tilesDBFile);
        
        // Create new TileOverlayOptions instance.
        TileOverlayOptions overlayOptions = new TileOverlayOptions();
        
        // Set the tile provider on the TileOverlayOptions.
        overlayOptions.tileProvider(mMapBoxTileProvider);
        
        GoogleMap map = mMapView.getMap();
        mMBTilesTileOverlay = map.addTileOverlay(overlayOptions);
        
        mOfflineMapCreated = true;
        enableOfflineMapToggle();
    }
    
    private void enableOfflineMapToggle(){
        if(mOptionsMenuCreated && mOfflineMapCreated){
            MenuItem item = mOptionsMenu.findItem(R.id.menu_item_map_show_overlay);
            if(item != null){
                item.setEnabled(true);
            }
        }
    }
    
    private void setupMap(){
        // Update the Activity's title
        getActivity().setTitle(mName);
        
        // Get reference to the map
        GoogleMap map = mMapView.getMap();
        map.getUiSettings().setMyLocationButtonEnabled(true);

        // Initialize to terrain map; user can switch.
        map.setMapType(GoogleMap.MAP_TYPE_NONE);

        if(null != mMapName && !mMapName.trim().isEmpty()){
            // Initialize tiles mAttrDb and get file name
            // TODO: This may need to be off the main thread, or it may need to
            // be async within MBTilesDatabase
            mTilesDB = new MBTilesDatabase(getActivity(), mMapName);
            if(mTilesDB.dbFileExists()){
                createOfflineTileProvider();
            } else {
                // Unzip in async task
                new ExtractMBTilesTask().execute(mTilesDB);
            }
        }

        Log.d(TAG, "Setting map center to latitude, longitude: " + mLat + ", " + mLon);

        // Center and zoom the map.
        LatLng waterfallLocation = new LatLng(mLat, mLon);
        Marker waterfallMarker = map.addMarker(new MarkerOptions()
            .position(waterfallLocation)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            .title(mName));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(waterfallLocation, 15));
    }
    
    // LoaderManager.LoaderCallbacks<Cursor> methods
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        // Get the query from our parent activity and pass it to the loader, which will execute it
        switch(loaderId){
            case InformationMapFragment.WATERFALL_QUERY_LOADER:
                Bundle qBundle = sQueryListener.onWaterfallQuery();
                cursorLoader = new SQLiteCursorLoader(
                        getActivity(), mAttrDb, qBundle.getString("query"), qBundle.getStringArray("args"));
                Log.d(TAG, "Created waterfall cursorLoader.");
                break;
        }
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d(TAG, "Inside onLoadFinished");
        int loaderId = loader.getId();
        if(cursor.moveToFirst()){
            switch(loaderId){
                case InformationMapFragment.WATERFALL_QUERY_LOADER:
                    // Get some data from the db
                    mLat = cursor.getDouble(AttrDatabase.COLUMNS.indexOf("geo_lat"));
                    mLon = cursor.getDouble(AttrDatabase.COLUMNS.indexOf("geo_lon"));
                    mName = cursor.getString(AttrDatabase.COLUMNS.indexOf("name"));
                    mMapName = cursor.getString(AttrDatabase.COLUMNS.indexOf("map_name"));
                    
                    // Now that we have what we need...
                    setupMap();
                    break;
            }
        }
    }
    
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "Inside InformationMapFragment onLoaderReset");
        //TODO: Set to null to prevent memory leaks
        /*mAdapter.changeCursor(cursor);*/
    }
    
    /*
     * AsyncTask used to unzip large MBTiles databases.
     */
    private class ExtractMBTilesTask extends AsyncTask<MBTilesDatabase, Void, Boolean>{
        protected Boolean doInBackground(MBTilesDatabase... dbs){
            MBTilesDatabase tilesDB = dbs[0];
            boolean success = tilesDB.extractDBFile();
            Log.d(TAG, "ExtractMBTilesTask doInBackground: extractDBFile() returned " + success);
            return success;
        }

        @Override
        protected void onPostExecute(Boolean result){
            Log.d(TAG, "Inside ExtractMBTilesTask PostExecute");
            if(result){
                createOfflineTileProvider();
            } else {
                // Also notify - this impacts user experience.
                CharSequence error = "Offline map not available :(";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(
                        InformationMapFragment.this.getActivity(), error, duration);
                toast.show();
            }
        }
    }

}
