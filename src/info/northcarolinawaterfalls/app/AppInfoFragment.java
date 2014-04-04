package info.northcarolinawaterfalls.app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import info.northcarolinawaterfalls.app.R;

public class AppInfoFragment extends SherlockFragment {

    private final String TAG = "AppInfoFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        AppInfoActivity theActivity = (AppInfoActivity) getActivity();
        View view = inflater.inflate(R.layout.fragment_app_info, container, false);
        TextView supportView = (TextView) view.findViewById(R.id.app_info_support_textview);
        if(supportView == null){
            Log.d(TAG, "supportView was null.");
        }
        String supportInfo = theActivity.readTxtFile(R.raw.support_info);
        supportView.setText(supportInfo);
        return view;
    }
}
