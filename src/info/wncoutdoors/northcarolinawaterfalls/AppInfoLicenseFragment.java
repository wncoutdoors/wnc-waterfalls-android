package info.wncoutdoors.northcarolinawaterfalls;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import info.northcarolinawaterfalls.R;

public class AppInfoLicenseFragment extends SherlockFragment {

    private final String TAG = "AppInfoLicenseFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_app_info_license, container, false);
        
        TextView licenseView = (TextView) view.findViewById(R.id.app_info_license_textview);
        licenseView.setText(((AppInfoActivity) getActivity()).readTxtFile(R.raw.license));
        return view;
    }

}
