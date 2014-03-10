package info.northcarolinawaterfalls.app;

import android.app.Activity;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
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
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
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

    private static AttrDatabase db = null;
    private MBTilesDatabase mTilesDB = null;
    private SQLiteCursorLoader cursorLoader = null;
    private OnWaterfallQueryListener sQueryListener; // Listener for loader callbacks
    private MapBoxOfflineTileProvider mMapBoxTileProvider;
    private TileOverlay mMBTilesTileOverlay;
    private Menu mOptionsMenu;
    private boolean mOptionsMenuCreated;
    private boolean mOfflineMapCreated;
    
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_information_map, container, false);
        mMapView = (MapView) view.findViewById(R.id.information_map_view);
        
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume(); // needed to get the map to display immediately
        
        try {
            MapsInitializer.initialize(getActivity());
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }

        GoogleMap googleMap = mMapView.getMap();
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Initialize to terrain map; user can switch.
        googleMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);       
        db = new AttrDatabase(getActivity());  // TODO: Memory leak? use app context?
        
        // Get our loader manager, and initialize queries.
        getLoaderManager().initLoader(WATERFALL_QUERY_LOADER, null, this); // Waterfall
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
    public void onResume() {
        Log.d(TAG, "Inside InformationMapFragment onResume()");
        super.onResume();
        if (null != mMapView)
            mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (null != mMapView)
            mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        if(null != mMapBoxTileProvider){
            mMapBoxTileProvider.close();
        }
        super.onDestroy();
        if(null != mMapView){
            mMapView.onDestroy();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != mMapView)
            mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (null != mMapView)
            mMapView.onLowMemory();
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
    
    // LoaderManager.LoaderCallbacks<Cursor> methods
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        // Get the query from our parent activity and pass it to the loader, which will execute it
        switch(loaderId){
            case InformationMapFragment.WATERFALL_QUERY_LOADER:
                Bundle qBundle = sQueryListener.onWaterfallQuery();
                cursorLoader = new SQLiteCursorLoader(
                        getActivity(), db, qBundle.getString("query"), qBundle.getStringArray("args"));
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
                    // Get reference to the map
                    GoogleMap map = mMapView.getMap();
                    
                    // Get some data from the db
                    double lat = cursor.getDouble(AttrDatabase.COLUMNS.indexOf("geo_lat"));
                    double lon = cursor.getDouble(AttrDatabase.COLUMNS.indexOf("geo_lon"));
                    String name = cursor.getString(AttrDatabase.COLUMNS.indexOf("name"));
                    String map_name = cursor.getString(AttrDatabase.COLUMNS.indexOf("map_name"));
                    
                    // Update the Activity's title
                    getActivity().setTitle(name);

                    if(null != map_name && "" != map_name){
                        // Initialize tiles db and get file name
                        // TODO: This may need to be off the main thread, or it may need to
                        // be async within MBTilesDatabase
                        mTilesDB = new MBTilesDatabase(getActivity(), map_name);
                        if(mTilesDB.dbFileExists()){
                            createOfflineTileProvider();
                        } else {
                            // Unzip in async task
                            new ExtractMBTilesTask().execute(mTilesDB);
                        }
                    }

                    Log.d(TAG, "Setting map center to latitude, longitude: " + lat + ", " + lon);

                    // Center and zoom the map.
                    LatLng waterfallLocation = new LatLng(lat, lon);
                    Marker waterfallMarker = map.addMarker(new MarkerOptions()
                        .position(waterfallLocation)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        .title(name));
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(waterfallLocation, 15));
                    break;
            }
        }
    }
    
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
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
