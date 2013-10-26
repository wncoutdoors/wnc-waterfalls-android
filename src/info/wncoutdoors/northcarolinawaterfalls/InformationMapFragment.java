package info.wncoutdoors.northcarolinawaterfalls;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
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

import info.wncoutdoors.northcarolinawaterfalls.InformationListFragment.OnWaterfallQueryListener;

public class InformationMapFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "InformationMapFragment";
    private static final int WATERFALL_QUERY_LOADER = 0;
    private static final int WATERFALL_MAP_QUERY_LOADER = 1;
    
    private MapView mMapView;

    private static AttrDatabase db = null;
    private SQLiteCursorLoader cursorLoader = null;
    private OnWaterfallQueryListener sQueryListener; // Listener for loader callbacks
    private OnWaterfallMapQueryListener sMapQueryListener; // Listener for loader callbacks
    
    //TODO: Move query listener interfaces to their own places.
    public interface OnWaterfallMapQueryListener{
        public Bundle onWaterfallMapQuery();
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // Make sure the containing activity implements the search listener interface
        try {
            sQueryListener = (OnWaterfallQueryListener) activity;
            sMapQueryListener = (OnWaterfallMapQueryListener) activity;
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
        googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        return view;

    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);       
        db = new AttrDatabase(getActivity());
        
        // Get our loader manager, and initialize queries.
        getLoaderManager().initLoader(WATERFALL_QUERY_LOADER, null, this); // Waterfall
        getLoaderManager().initLoader(WATERFALL_MAP_QUERY_LOADER, null, this); // Waterfall maps
    }
    
    // Route fragment lifecycle events to MapView
    @Override
    public void onResume() {
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
        super.onDestroy();
        if (null != mMapView)
            mMapView.onDestroy();
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
                
            case InformationMapFragment.WATERFALL_MAP_QUERY_LOADER:
                Bundle mapQBundle = sMapQueryListener.onWaterfallMapQuery();
                cursorLoader = new SQLiteCursorLoader(
                        getActivity(), db, mapQBundle.getString("query"), mapQBundle.getStringArray("args"));
                Log.d(TAG, "Created waterfall map cursorLoader.");
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
                    // Center and zoom the map.
                    double lat = cursor.getDouble(AttrDatabase.COLUMNS.indexOf("geo_lat"));
                    double lon = cursor.getDouble(AttrDatabase.COLUMNS.indexOf("geo_lon"));
                    String name = cursor.getString(AttrDatabase.COLUMNS.indexOf("name"));
                    String desc = cursor.getString(AttrDatabase.COLUMNS.indexOf("description"));
                    
                    Log.d(TAG, "Setting map center to latitude, longitude: " + lat + ", " + lon);
                    GoogleMap map = mMapView.getMap();                    
                    LatLng waterfallLocation = new LatLng(lat, lon);
                    Marker waterfallMarker = map.addMarker(new MarkerOptions()
                        .position(waterfallLocation)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        .title(name));
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(waterfallLocation, 15));
                    break;

                case InformationMapFragment.WATERFALL_MAP_QUERY_LOADER:
                    // Add mbtiles layer
                    Log.d(TAG, "Ready to add mbtiles layer to map.");
                    break;
            }
        }
    }
    
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //TODO: Set to null to prevent memory leaks
        /*mAdapter.changeCursor(cursor);*/
    }

}
