package info.wncoutdoors.northcarolinawaterfalls;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockFragment;

public class SearchLocationFragment extends SherlockFragment implements OnClickListener{
    
    private final String TAG = "SearchHikeFragment";
    
    private Spinner locationDistanceSpinner;
    private Spinner locationReltoSpinner;

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
            String distanceSelected = locationDistanceSpinner.getSelectedItem().toString();
            
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
                       
            Log.d(TAG, "Button wuz clicked");
            Log.d(TAG, "Within: " + distanceSelected );
            Log.d(TAG, "Of: " + reltoSelected );
            Log.d(TAG, "Which is: " + locationSearched );
            Log.d(TAG, "Only shared: " + String.valueOf(isChecked));
            break;
        }
    }
    
}
