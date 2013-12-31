package info.northcarolinawaterfalls;

import android.location.Address;
import android.location.Location;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.commonsware.cwac.loaderex.acl.SQLiteCursorLoader;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import info.northcarolinawaterfalls.R;
import info.northcarolinawaterfalls.ResultsListFragment.OnWaterfallQueryListener;
import info.northcarolinawaterfalls.ResultsListFragment.OnWaterfallSelectListener;

public class ResultsMapFragment extends SherlockFragment implements
            LoaderManager.LoaderCallbacks<Cursor>, OnInfoWindowClickListener {
    
    private static final String TAG = "ResultsListFragment";
    private MapView mMapView;
    private OnWaterfallQueryListener sQueryListener; // Listener for loader callbacks
    private OnWaterfallSelectListener sSelectListener; // Listener for user waterfall selections
    private OnLocationQueryListener sLocationQueryListener; // Listener for location searches

    private static AttrDatabase db = null;
    private SQLiteCursorLoader cursorLoader = null;
    
    // Oh this makes me sad
    private Map<Marker, Long> mMarkersToIds = new HashMap<Marker, Long>();
    
    public interface OnLocationQueryListener{
        public void determineLocation(Fragment requestor);
        public void stopLocationClient();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onActivityCreated(savedInstanceState);
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
        
        // And the interfaces for obtaining locations from a LocationClient
        try {
            sLocationQueryListener = (OnLocationQueryListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnQueryLocationListener");
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);       
        db = new AttrDatabase(getActivity());
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
            // TODO: Toast?? Error dialog? Use getActivity().googlePlayServicesAvailable()?
        }

        GoogleMap googleMap = mMapView.getMap();
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Initialize to terrain map; user can switch.
        googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        
        // Set map to run our callback when info window clicked
        googleMap.setOnInfoWindowClickListener(this);
        return view;
    }

    // Called when the Activity becomes visible.
    @Override
    public void onStart() {
        super.onStart();
        sLocationQueryListener.determineLocation((Fragment) this);
        // Will invoke onLocationDetermined when complete.
    }

    // Called when the Activity is no longer visible.
    @Override
    public void onStop() {
        sLocationQueryListener.stopLocationClient();
        super.onStop();
    }

    // Route certain fragment lifecycle events to MapView
    @Override
    public void onResume() {
        super.onResume();
        if (null != mMapView){
            mMapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (null != mMapView){
            mMapView.onPause();
        }
    }

    @Override
    public void onDestroy() {
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
    
    // Async location determination callback method
    public void onLocationDetermined(){
        // Get our loader manager, and initialize the
        // query based on the containing Activity's searchMode
        // TODO: Hide the overlay.
        getLoaderManager().initLoader(0, null, this);
        Log.d(TAG, "onLocationDetermined finished and loader manager initialized.");
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
        } else {
            // Put the results on a map!
            GoogleMap map = mMapView.getMap();
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            if(cursor.moveToFirst()){
                // First, add the "searched for" location.
                // TODO: Replace with interface methods and add listeners as we do with queries.
                Address originAddress = ((ResultsActivity) getActivity()).getOriginAddress();
                
                // Get searched-for distance and convert to meters.
                short searchLocationDistance = ((ResultsActivity) getActivity()).getSearchLocationDistance();
                double searchLocationDistanceM =  searchLocationDistance * 1609.34;
                
                // Build up a list of Address1, Address2, Address3, if present.
                ArrayList<String> addressList = new ArrayList<String>(); 
                for(int i=0; i<=3; i++){
                    String line = originAddress.getAddressLine(i);
                    if(line != null && line.length() > 0){
                        addressList.add(line);
                    }
                }
                
                String addressDesc = TextUtils.join("\n", addressList);
                if(addressDesc == ""){
                    addressDesc = originAddress.getFeatureName();
                }
                if(addressDesc == ""){
                    addressDesc = "Searched Location";
                }
                
                // Create the LatLng and the map marker.
                LatLng originLatLng = new LatLng(
                        originAddress.getLatitude(), originAddress.getLongitude());
                Marker originMarker = map.addMarker(new MarkerOptions()
                    .position(originLatLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    .title(addressDesc));
                
                boundsBuilder.include(originLatLng);  // In case only one result :)
                
                // Translate into a Location for distance comparison.
                Location originLocation = new Location("");
                originLocation.setLatitude(originAddress.getLatitude());
                originLocation.setLongitude(originAddress.getLongitude());
                
                // Next, add the results waterfalls.
                // Use do...while since we're already at the first result.
                do{
                    // Get some data from the db
                    Long waterfallId = cursor.getLong(AttrDatabase.COLUMNS.indexOf("_id"));
                    double lat = cursor.getDouble(AttrDatabase.COLUMNS.indexOf("geo_lat"));
                    double lon = cursor.getDouble(AttrDatabase.COLUMNS.indexOf("geo_lon"));
                    
                    // Make sure this one's actually within our search radius. SQL only checked
                    // the bounding box.
                    Location waterfallLocation = new Location("");
                    waterfallLocation.setLatitude(lat);
                    waterfallLocation.setLongitude(lon);
                    
                    if(originLocation.distanceTo(waterfallLocation) <= searchLocationDistanceM){
                        // Within radius. Display on map.
                        String name = cursor.getString(AttrDatabase.COLUMNS.indexOf("name"));
                        
                        LatLng waterfallLatLng = new LatLng(lat, lon);
                        Marker waterfallMarker = map.addMarker(new MarkerOptions()
                            .position(waterfallLatLng)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            .title(name));
                        
                        // Save the id so we can retrieve it when clicked
                        mMarkersToIds.put(waterfallMarker, waterfallId);
                        boundsBuilder.include(waterfallLatLng);
                    }

                } while(cursor.moveToNext());
                
                // Zoom and center the map to bounds
                LatLngBounds bounds = boundsBuilder.build();
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 15));
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /*mAdapter.changeCursor(cursor);*/
    }

    // Marker info window click method
    @Override
    public void onInfoWindowClick(Marker marker) {
        long waterfallId = mMarkersToIds.get(marker);
        sSelectListener.onWaterfallSelected(waterfallId);
    }
}
