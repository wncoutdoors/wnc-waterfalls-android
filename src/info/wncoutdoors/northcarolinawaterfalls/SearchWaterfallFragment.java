package info.wncoutdoors.northcarolinawaterfalls;

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
            
            Log.d(TAG, "Button wuz clicked");
            Log.d(TAG, "Textbox contents: " + waterfallSearched );
            Log.d(TAG, "Only shared: " + String.valueOf(isChecked));
            break;
        }
    }
}
