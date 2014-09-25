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
 * AppInfoLicenseFragment.java
 * Fragment which controls the Legal Notices tab under the App Info activity,
 * and provides this app's license information and attribution to other open
 * source projects.
 */
package info.wncwaterfalls.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.android.gms.common.GooglePlayServicesUtil;
import info.wncwaterfalls.app.R;

public class AppInfoLicenseFragment extends SherlockFragment {

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
