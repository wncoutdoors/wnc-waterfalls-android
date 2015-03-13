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
 * TabListener.java
 * A generic tab listener to handle swapping of fragments within our
 * Activities.
 */
package info.wncwaterfalls.app;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;

public class TabListener<T extends Fragment> implements ActionBar.TabListener {
    private static final String TAG = "TabListener";

    private Fragment aFragment;
    private final FragmentActivity anActivity;
    private final String aFragTag;
    private final Class<T> aClass;

    public TabListener(FragmentActivity activity, String tag, Class<T> cls) {
        anActivity = activity;
        aFragTag = tag;
        aClass = cls;
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction transaction) {
        //Log.d(TAG, "Inside onTabReselected");
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction transaction) {
        Fragment aFragment = (Fragment) anActivity.getSupportFragmentManager().findFragmentByTag(aFragTag);
        // Check if the fragment is already initialized
        if(aFragment == null){
            // Create a new one
            aFragment = (Fragment) Fragment.instantiate(anActivity, aClass.getName());
            transaction.add(android.R.id.content, aFragment, aFragTag);
        } else {
            // Attach existing one
            transaction.attach(aFragment);
        }
        this.aFragment = aFragment;
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction transaction) {
        if(aFragment != null){
            transaction.detach(aFragment);
        }
    }
}
