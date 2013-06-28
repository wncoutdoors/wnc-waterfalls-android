package info.wncoutdoors.northcarolinawaterfalls;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockFragment;

public class SearchLocationFragment extends SherlockFragment implements OnClickListener{
    
    private final String TAG = "SearchHikeFragment";
    private OnLocationSearchListener sListener;
    
    private Spinner locationDistanceSpinner;
    private Spinner locationReltoSpinner;
    
    public interface OnLocationSearchListener{
        public void onLocationSearch(boolean onlyShared, short distance, String relTo, String relToText);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // Make sure the containing activity implements the search listener interface
        try {
            sListener = (OnLocationSearchListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnHikeSearchListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View locationSearchFragmentView = inflater.inflate(R.layout.fragment_location_search, container, false);
        setupSpinners(locationSearchFragmentView);
        
        // Up-set the Find button.
        Button b = (Button) locationSearchFragmentView.findViewById(R.id.search_location_find_button);
        b.setOnClickListener(this);
        
        return locationSearchFragmentView;
    }
    
    private void setupSpinners(View view){
        locationDistanceSpinner = (Spinner) view.findViewById(R.id.search_location_distance_spinner);
        ArrayAdapter<CharSequence> locationDistanceAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.location_distance_options, android.R.layout.simple_spinner_item);
        locationDistanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationDistanceSpinner.setAdapter(locationDistanceAdapter);
        
        locationReltoSpinner = (Spinner) view.findViewById(R.id.search_location_relto_spinner);
        ArrayAdapter<CharSequence> locationReltoAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.location_relto_options, android.R.layout.simple_spinner_item);
        locationReltoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationReltoSpinner.setAdapter(locationReltoAdapter);
        locationReltoSpinner.setOnItemSelectedListener(new SpinnerSelectionListener());
    }
    
    class SpinnerSelectionListener implements OnItemSelectedListener {
        // Listener for the Relto spinner to modify the EditText control appropriately
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id){
            // An item was selected
            EditText searchLocationReltoTextbox =
                (EditText) getView().findViewById(R.id.search_location_relto_txt);
            String reltoSelected = parent.getItemAtPosition(pos).toString();
            Log.d(TAG, "Relto spinner item selected: " + reltoSelected);
            searchLocationReltoTextbox.setText("");
            searchLocationReltoTextbox.setEnabled(true);
            searchLocationReltoTextbox.setFocusable(true);
            searchLocationReltoTextbox.setFocusableInTouchMode(true); // Because just focus isn't enough
            
            if(reltoSelected.equals("Current Location")){
                // Disable it
                searchLocationReltoTextbox.setInputType(InputType.TYPE_NULL);
                searchLocationReltoTextbox.setEnabled(false);
                searchLocationReltoTextbox.setFocusable(false);
            } else if(reltoSelected.equals("Address")) {
                searchLocationReltoTextbox.setInputType(InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
            } else if(reltoSelected.equals("City")) {
                searchLocationReltoTextbox.setInputType(InputType.TYPE_CLASS_TEXT);
            } else if(reltoSelected.equals("Zip")) {
                searchLocationReltoTextbox.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
        }
        
        public void onNothingSelected(AdapterView<?> parent){
            // Another interface callback
            Log.d(TAG, "Somehow, nothing was selected...");
        }
    }
    
    @Override
    public void onClick(View button) {
        switch (button.getId()) {
        case R.id.search_location_find_button:
            Log.d(TAG, "Button clicked: " + button.toString());
            
            // TODO: Collapse these
            // Get the text of the selected item in the Within (Distance) spinner
            Spinner locationDistanceSpinner =
                 (Spinner) getView().findViewById(R.id.search_location_distance_spinner);
            String distanceSelectedStr = locationDistanceSpinner.getSelectedItem().toString();
            
            // Get the length out of the spinner. TODO: Make this suck less
            short distanceSelected = -1;
            if(!distanceSelectedStr.equals("All")){
                String[] splitResult = distanceSelectedStr.split(" ");
                distanceSelected = Short.valueOf(splitResult[0]);
            }
            
            // Get the text of the selected item in the Of (Relto) spinner
            Spinner locationReltoSpinner =
                 (Spinner) getView().findViewById(R.id.search_location_relto_spinner);
            String reltoSelected = locationReltoSpinner.getSelectedItem().toString();

            // Get the text of the entry below Relto spinner
            EditText searchLocationReltoTextbox =
                 (EditText) getView().findViewById(R.id.search_location_relto_txt);
            String locationSearched = searchLocationReltoTextbox.getText().toString();
            
            // Get the state of the "Only falls I've Shared" checkbox
            CheckBox searchLocationSharedCheckbox =
                    (CheckBox) getView().findViewById(R.id.search_location_shared_checkbox);
            boolean isChecked = searchLocationSharedCheckbox.isChecked();
                       
            sListener.onLocationSearch(isChecked,  distanceSelected,  reltoSelected,  locationSearched);
            break;
        }
    }
    
}
