package info.northcarolinawaterfalls.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.android.gms.common.GooglePlayServicesUtil;

import info.northcarolinawaterfalls.app.R;

public class AppInfoLicenseFragment extends SherlockFragment {

    private final String TAG = "AppInfoLicenseFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_app_info_license, container, false);
        AppInfoActivity theActivity = (AppInfoActivity) getActivity();
        TextView licenseView = (TextView) view.findViewById(R.id.app_info_license_textview);
        licenseView.setText(theActivity.readTxtFile(R.raw.license));
        
        TextView attributionView = (TextView) view.findViewById(R.id.app_info_attribution_textview);
        String attributionInfo = theActivity.readTxtFile(R.raw.attribution);
        attributionView.setText(attributionInfo);
        
        String playServicesLicense = GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(theActivity);
        if(playServicesLicense != null){
            TextView playServicesView = (TextView) view.findViewById(R.id.app_info_play_services_license_textview);
            playServicesView.setText(playServicesLicense);
        }

        return view;
    }

}
