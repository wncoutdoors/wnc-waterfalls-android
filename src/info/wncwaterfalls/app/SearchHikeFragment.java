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
 * SearchHikeFragment.java
 * Fragment which controls the Hike search tab, which allows users to
 * find waterfalls based on parameters of the hike it takes to get to them.
 */
package info.wncwaterfalls.app;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

import info.wncwaterfalls.app.R;

public class SearchHikeFragment extends Fragment implements OnClickListener{
    
    private final String TAG = "SearchHikeFragment";
    OnHikeSearchListener sListener;
    
    private Spinner trailLengthSpinner;
    private Spinner trailDifficultySpinner;
    private Spinner trailClimbSpinner;
    
    // Interface for listening to our searches
    public interface OnHikeSearchListener{
        public void onHikeSearch(boolean onlyShared, short trailLength, short trailDifficulty, short trailClimb);
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
                R.array.hike_trail_length_labels, android.R.layout.simple_spinner_item);
        trailLengthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        trailLengthSpinner.setAdapter(trailLengthAdapter);
        
        // Trail Difficulty spinner
        trailDifficultySpinner = (Spinner) view.findViewById(R.id.search_hike_trail_difficulty_spinner);
        ArrayAdapter<CharSequence> trailDifficultyAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.hike_trail_difficulty_labels, android.R.layout.simple_spinner_item);
        trailDifficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        trailDifficultySpinner.setAdapter(trailDifficultyAdapter);
        
        // Trail Climb spinner
        trailClimbSpinner = (Spinner) view.findViewById(R.id.search_hike_trail_climb_spinner);
        ArrayAdapter<CharSequence> trailClimbingAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.hike_trail_climb_labels, android.R.layout.simple_spinner_item);
        trailClimbingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        trailClimbSpinner.setAdapter(trailClimbingAdapter);
    }
    
    public void performHikeSearch(){
        // Get the value of the selected items in the various spinners.
        Spinner trailLengthSpinner =
             (Spinner) getView().findViewById(R.id.search_hike_trail_length_spinner);
        short lengthSelected = (short) getResources().getIntArray(
                R.array.hike_trail_length_values)[trailLengthSpinner.getSelectedItemPosition()];
        Spinner trailDifficultySpinner =
             (Spinner) getView().findViewById(R.id.search_hike_trail_difficulty_spinner);
        short difficultySelected = (short) getResources().getIntArray(
                R.array.hike_trail_difficulty_values)[trailDifficultySpinner.getSelectedItemPosition()];
        Spinner trailClimbSpinner =
             (Spinner) getView().findViewById(R.id.search_hike_trail_climb_spinner);
        short climbSelected = (short) getResources().getIntArray(
                R.array.hike_trail_climb_values)[trailClimbSpinner.getSelectedItemPosition()];
        
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
            performHikeSearch();
        }
    }

}
