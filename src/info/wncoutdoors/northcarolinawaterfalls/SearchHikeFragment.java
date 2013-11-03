package info.wncoutdoors.northcarolinawaterfalls;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockFragment;

public class SearchHikeFragment extends SherlockFragment implements OnClickListener{
    
    private final String TAG = "SearchHikeFragment";
    OnHikeSearchListener sListener;
    
    private Spinner trailLengthSpinner;
    private Spinner trailDifficultySpinner;
    private Spinner trailClimbSpinner;
    
    // Interface for listening to our searches
    public interface OnHikeSearchListener{
        public void onHikeSearch(boolean onlyShared, short trailLength, String trailDifficulty, String trailClimb);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // Make sure the containing activity implements the search listener interface
        try {
            sListener = (OnHikeSearchListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnHikeSearchListener");
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View hikeSearchFragmentView = inflater.inflate(R.layout.fragment_hike_search, container, false);
        setupSpinners(hikeSearchFragmentView);
        
        // Up-set the Find button.
        Button b = (Button) hikeSearchFragmentView.findViewById(R.id.search_hike_find_button);
        b.setOnClickListener(this);
        
        return hikeSearchFragmentView;
    }
    
    // Set up the spinners on this fragment with their drop-down items
    private void setupSpinners(View view){
        // Trail Length spinner
        trailLengthSpinner = (Spinner) view.findViewById(R.id.search_hike_trail_length_spinner);
        ArrayAdapter<CharSequence> trailLengthAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.hike_trail_lenth_options, android.R.layout.simple_spinner_item);
        trailLengthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        trailLengthSpinner.setAdapter(trailLengthAdapter);
        
        // Trail Difficulty spinner
        trailDifficultySpinner = (Spinner) view.findViewById(R.id.search_hike_trail_difficulty_spinner);
        ArrayAdapter<CharSequence> trailDifficultyAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.hike_trail_difficulty_options, android.R.layout.simple_spinner_item);
        trailDifficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        trailDifficultySpinner.setAdapter(trailDifficultyAdapter);
        
        // Trail Climb spinner
        trailClimbSpinner = (Spinner) view.findViewById(R.id.search_hike_trail_climb_spinner);
        ArrayAdapter<CharSequence> trailClimbingAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.hike_trail_climb_options, android.R.layout.simple_spinner_item);
        trailClimbingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        trailClimbSpinner.setAdapter(trailClimbingAdapter);
    }
    
    public void performHikeSearch(){
        // TODO: Collapse these
        // Get the text of the selected item in the Trail Length spinner
        Spinner trailLengthSpinner =
             (Spinner) getView().findViewById(R.id.search_hike_trail_length_spinner);
        String lengthSelectedStr = trailLengthSpinner.getSelectedItem().toString();
        
        // Get the length out of the spinner. TODO: Make this suck less
        short lengthSelected = -1;
        if(!lengthSelectedStr.equals("All")){
            String[] splitResult = lengthSelectedStr.split(" ");
            lengthSelected = Short.valueOf(splitResult[0]);
        }
        
        // Get the text of the selected item in the Trail Difficulty spinner
        Spinner trailDifficultySpinner =
             (Spinner) getView().findViewById(R.id.search_hike_trail_difficulty_spinner);
        String difficultySelected = trailDifficultySpinner.getSelectedItem().toString();

        // Get the text of the selected item in the Trail Climbing spinner
        Spinner trailClimbSpinner =
             (Spinner) getView().findViewById(R.id.search_hike_trail_climb_spinner);
        String climbSelected = trailClimbSpinner.getSelectedItem().toString();
        
        // Get the state of the "Only falls I've Shared" checkbox
        CheckBox searchTrailSharedCheckbox =
                (CheckBox) getView().findViewById(R.id.search_hike_shared_checkbox);
        boolean isChecked = searchTrailSharedCheckbox.isChecked();                      
      
        // Call the search listener on parent activity
        sListener.onHikeSearch(isChecked, lengthSelected, difficultySelected, climbSelected);
    }
    
    @Override
    public void onClick(View button) {
        int buttonId = button.getId();
        if(buttonId == R.id.search_hike_find_button){
            Log.d(TAG, "Button clicked: " + button.toString());
            performHikeSearch();
        }
    }

}
