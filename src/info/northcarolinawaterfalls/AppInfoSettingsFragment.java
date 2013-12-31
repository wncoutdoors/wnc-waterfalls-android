package info.northcarolinawaterfalls;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.android.vending.expansion.downloader.DownloadProgressInfo;
import com.google.android.vending.expansion.downloader.Helpers;
import com.google.android.vending.expansion.downloader.IDownloaderClient;
import com.google.android.vending.expansion.downloader.IDownloaderService;
import info.northcarolinawaterfalls.R;

public class AppInfoSettingsFragment extends SherlockFragment {

    private final String TAG = "AppInfoSettingsFragment";
    
    private ProgressBar mPB;
    private TextView mStatusText;
    private TextView mProgressFraction;
    private TextView mProgressPercent;
    private TextView mAverageSpeed;
    private TextView mTimeRemaining;

    private View mDashboard;
    private View mCellMessage;

    private Button mPauseButton;
    private Button mWiFiSettingsButton;
    
    private boolean mStatePaused;
    private int mState;
    
    private OnExpansionFilesDownloadListener sExpansionFilesDownloadListener;
    
    // TODO: Move these outside this class
    // Interface for listening to requests for expansion files downloads
    // Containing activity must implement this.
    public interface OnExpansionFilesDownloadListener{
        public boolean buildPendingDownloadIntent();
        public void serviceRequestContinueDownload();
        public void serviceRequestPauseDownload();
        public void serviceRequestSetDownloadFlags(int flags);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // Make sure the containing activity implements the downloader listener interface
        try {
            sExpansionFilesDownloadListener = (OnExpansionFilesDownloadListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnExpansionFilesDownloadListener");
        }
        
    }
    
    private void setState(int newState) {
        Log.d(TAG, "Setting settings state to " + newState);
        if (mState != newState) {
            mState = newState;
            mStatusText.setText(Helpers.getDownloaderStringResourceIDFromState(newState));
        }
    }

    private void setButtonPausedState(boolean paused) {
        Log.d(TAG, "Changing pause button to " + paused);
        mStatePaused = paused;
        int stringResourceID = paused ? R.string.text_button_resume : R.string.text_button_pause;
        mPauseButton.setText(stringResourceID);
    }
    
    // Tie UI controls into remote service calls for controlling the downloader.
    public void initializeDownloadUI(View view){
        Log.d(TAG, "Initializing download UI.");
        
        mPB = (ProgressBar) view.findViewById(R.id.progressBar);
        mStatusText = (TextView) view.findViewById(R.id.statusText);
        mProgressFraction = (TextView) view.findViewById(R.id.progressAsFraction);
        mProgressPercent = (TextView) view.findViewById(R.id.progressAsPercentage);
        mAverageSpeed = (TextView) view.findViewById(R.id.progressAverageSpeed);
        mTimeRemaining = (TextView) view.findViewById(R.id.progressTimeRemaining);
        mDashboard = view.findViewById(R.id.downloaderDashboard);
        mCellMessage = view.findViewById(R.id.approveCellular);
        mPauseButton = (Button) view.findViewById(R.id.pauseButton);
        mWiFiSettingsButton = (Button) view.findViewById(R.id.wifiSettingsButton);
        
        mPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mStatePaused) {
                    sExpansionFilesDownloadListener.serviceRequestContinueDownload();
                } else {
                    sExpansionFilesDownloadListener.serviceRequestPauseDownload();
                }
                setButtonPausedState(!mStatePaused);
            }
        });

        mWiFiSettingsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        });

        Button resumeOnCell = (Button) view.findViewById(R.id.resumeOverCellular);
        resumeOnCell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sExpansionFilesDownloadListener.serviceRequestSetDownloadFlags(
                        IDownloaderService.FLAGS_DOWNLOAD_OVER_CELLULAR);
                sExpansionFilesDownloadListener.serviceRequestContinueDownload();
                mCellMessage.setVisibility(View.GONE);
            }
        });
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_app_info_settings, container, false);
        if(!ExpansionDownloaderService.expansionFilesDownloaded(getActivity())){
            // Tell the activity to create download notification.
            boolean needsDownloadUI = sExpansionFilesDownloadListener.buildPendingDownloadIntent();
            Log.d(TAG, "Needs download ui: " + needsDownloadUI);
            if(needsDownloadUI){
                initializeDownloadUI(view);
            }
        }
        return view;
    }
    
    /**
     * The download state should trigger changes in the UI --- it may be useful
     * to show the state as being indeterminate at times.
     * 
     * We're going with the sample-as-guideline-is-fine until testing proves otherwise.
     * 
     * In our implementation, these are forwarded from the Activity to the Fragment
     */
    public void onDownloadStateChanged(int newState) {
        Log.d(TAG, "Download state changed: " + newState);
        
        setState(newState);
        boolean showDashboard = true;
        boolean showCellMessage = false;
        boolean paused;
        boolean indeterminate;
        switch (newState) {
            case IDownloaderClient.STATE_IDLE:
                // STATE_IDLE means the service is listening, so it's
                // safe to start making calls via mRemoteService.
                paused = false;
                indeterminate = true;
                break;
            case IDownloaderClient.STATE_CONNECTING:
            case IDownloaderClient.STATE_FETCHING_URL:
                showDashboard = true;
                paused = false;
                indeterminate = true;
                break;
            case IDownloaderClient.STATE_DOWNLOADING:
                paused = false;
                showDashboard = true;
                indeterminate = false;
                break;

            case IDownloaderClient.STATE_FAILED_CANCELED:
            case IDownloaderClient.STATE_FAILED:
            case IDownloaderClient.STATE_FAILED_FETCHING_URL:
            case IDownloaderClient.STATE_FAILED_UNLICENSED:
                paused = true;
                showDashboard = false;
                indeterminate = false;
                break;
            case IDownloaderClient.STATE_PAUSED_NEED_CELLULAR_PERMISSION:
            case IDownloaderClient.STATE_PAUSED_WIFI_DISABLED_NEED_CELLULAR_PERMISSION:
                showDashboard = false;
                paused = true;
                indeterminate = false;
                showCellMessage = true;
                break;

            case IDownloaderClient.STATE_PAUSED_BY_REQUEST:
                paused = true;
                indeterminate = false;
                break;
            case IDownloaderClient.STATE_PAUSED_ROAMING:
            case IDownloaderClient.STATE_PAUSED_SDCARD_UNAVAILABLE:
                paused = true;
                indeterminate = false;
                break;
            case IDownloaderClient.STATE_COMPLETED:
                showDashboard = false;
                paused = false;
                indeterminate = false;
                // TODO: validateXAPKZipFiles(); // Ugh. This should be done in the Activity
                return;
            default:
                paused = true;
                indeterminate = true;
                showDashboard = true;
        }
        int newDashboardVisibility = showDashboard ? View.VISIBLE : View.GONE;
        if (mDashboard.getVisibility() != newDashboardVisibility) {
            mDashboard.setVisibility(newDashboardVisibility);
        }
        int cellMessageVisibility = showCellMessage ? View.VISIBLE : View.GONE;
        if (mCellMessage.getVisibility() != cellMessageVisibility) {
            mCellMessage.setVisibility(cellMessageVisibility);
        }

        mPB.setIndeterminate(indeterminate);
        setButtonPausedState(paused);
    }
    
    /**
     * Sets the state of the various controls based on the progressinfo object
     * sent from the downloader service.
     */
    public void onDownloadProgress(DownloadProgressInfo progress) {
        Log.d(TAG, "Download progress received.");
        
        mAverageSpeed.setText(getString(R.string.kilobytes_per_second,
                Helpers.getSpeedString(progress.mCurrentSpeed)));
        mTimeRemaining.setText(getString(R.string.time_remaining,
                Helpers.getTimeRemaining(progress.mTimeRemaining)));

        progress.mOverallTotal = progress.mOverallTotal;
        mPB.setMax((int) (progress.mOverallTotal >> 8));
        mPB.setProgress((int) (progress.mOverallProgress >> 8));
        mProgressPercent.setText(Long.toString(progress.mOverallProgress
                * 100 /
                progress.mOverallTotal) + "%");
        mProgressFraction.setText(Helpers.getDownloadProgressString
                (progress.mOverallProgress,
                        progress.mOverallTotal));
    }
    
}