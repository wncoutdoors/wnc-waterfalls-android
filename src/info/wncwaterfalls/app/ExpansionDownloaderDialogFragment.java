/*
 * Copyright 2014 WNCOutdoors.info
 * portions Copyright 2014 The Android Open Source Project
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
 * ExpansionDownloaderDialogFragment.java
 * Class which creates a dialog to allow the user to choose whether to
 * proceed with expansion file download.
 */
package info.wncwaterfalls.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import info.wncwaterfalls.app.R;

public class ExpansionDownloaderDialogFragment extends DialogFragment {
    
    private static String TAG = "ExpansionDownloaderDialogFragment";
    
    // Containing activity must implement
    public interface ExpansionDownloadDialogListener {
        public void onExpansionDownloaderDialogPositiveClick(DialogFragment dialog);
        public void onExpansionDownloaderDialogNegativeClick(DialogFragment dialog);
    }
    
    // Use this instance of the interface to deliver action events once initiated in onAttach
    ExpansionDownloadDialogListener mListener;
    
    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        // Verify that activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (ExpansionDownloadDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString() + " must implement ExpansionDownloaderDialogFragment");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        Log.d(TAG, "Creating download option dialog.");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.expansion_download_dialog_title);
        builder.setMessage(R.string.expansion_download_dialog_message);
        builder.setPositiveButton(
                R.string.expansion_download_dialog_button_download, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onExpansionDownloaderDialogPositiveClick(ExpansionDownloaderDialogFragment.this);
                    }
        });
        builder.setNegativeButton(
                R.string.expansion_download_dialog_button_skip, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        mListener.onExpansionDownloaderDialogNegativeClick(ExpansionDownloaderDialogFragment.this);
                    }
        });
        return builder.create();
    }
}
