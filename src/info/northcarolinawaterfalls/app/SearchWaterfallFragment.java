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
 * SearchWaterfallFragment.java
 * Fragment which controls the Waterfall search tab, which allows users to
 * find waterfalls by name.
 */
package info.northcarolinawaterfalls.app;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import info.northcarolinawaterfalls.app.R;

public class SearchWaterfallFragment extends SherlockFragment implements OnClickListener{
    private final String TAG = "SearchWaterfallFragment";
    OnWaterfallSearchListener sListener;

    // Interface for listening to our searches
    public interface OnWaterfallSearchListener{
        public void onWaterfallSearch(boolean onlyShared, String searchTerm);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // Make sure the containing activity implements the search listener interface
        try {
            sListener = (OnWaterfallSearchListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnWaterfallSearchListener");
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View waterfallSearchFragmentView = inflater.inflate(R.layout.fragment_waterfall_search, container, false);
        
        // Up-set the Find button.
        Button b = (Button) waterfallSearchFragmentView.findViewById(R.id.search_waterfall_find_button);
        b.setOnClickListener(this);
        
        // Set up edit text for search button in keyboard
        EditText waterfalltextBox = (EditText) waterfallSearchFragmentView.findViewById(
            R.id.search_waterfall_name_textbox);
        waterfalltextBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performLocationSearch();
                    return true;
                }
                return false;
            }
        });
        
        return waterfallSearchFragmentView;
    }
  
    public void performLocationSearch(){
        // Get the text of the Name field
        EditText searchWaterfallTextbox =
             (EditText) getView().findViewById(R.id.search_waterfall_name_textbox);
        String waterfallSearched = searchWaterfallTextbox.getText().toString();
        
        // Get the state of the "Only falls I've Shared" checkbox
        CheckBox searchWaterfallSharedCheckbox =
                (CheckBox) getView().findViewById(R.id.search_waterfall_shared_checkbox);
        boolean isChecked = searchWaterfallSharedCheckbox.isChecked();
        
        // Call the search listener on parent activity
        sListener.onWaterfallSearch(isChecked, waterfallSearched);
    }
    
    @Override
    public void onClick(View button) {
        int buttonId = button.getId();
        if(buttonId == R.id.search_waterfall_find_button){
            Log.d(TAG, "Button clicked: " + button.toString());
            performLocationSearch();
        }
    }
}
