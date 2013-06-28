package info.wncoutdoors.northcarolinawaterfalls;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockFragment;

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
        
        return waterfallSearchFragmentView;
    }
    
    @Override
    public void onClick(View button) {
        switch (button.getId()) {
        case R.id.search_waterfall_find_button:
            Log.d(TAG, "Button clicked: " + button.toString());
            
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
            break;
        }
    }
}
