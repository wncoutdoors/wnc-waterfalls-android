package info.northcarolinawaterfalls;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

import info.northcarolinawaterfalls.R;

public class ExpansionDownloaderDialogFragment extends DialogFragment {
    
    private static String TAG = "ExpansionDownloaderDialogFragment";
    
    // Containing activity must implement
    public interface ExpansionDownloadDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
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
                        mListener.onDialogPositiveClick(ExpansionDownloaderDialogFragment.this);
                    }
        });
        builder.setNegativeButton(
                R.string.expansion_download_dialog_button_skip, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        mListener.onDialogNegativeClick(ExpansionDownloaderDialogFragment.this);
                    }
        });
        return builder.create();
    }
}
