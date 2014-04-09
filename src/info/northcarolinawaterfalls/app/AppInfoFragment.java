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
 * AppInfoFragment.java 
 * Fragment which controls the Support tab in the App Info activity.
 */
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
