package info.northcarolinawaterfalls.app;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Messenger;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.android.vending.expansion.downloader.DownloadProgressInfo;
import com.google.android.vending.expansion.downloader.DownloaderClientMarshaller;
import com.google.android.vending.expansion.downloader.DownloaderServiceMarshaller;
import com.google.android.vending.expansion.downloader.IDownloaderClient;
import com.google.android.vending.expansion.downloader.IDownloaderService;
import com.google.android.vending.expansion.downloader.IStub;

import info.northcarolinawaterfalls.app.R;
import info.northcarolinawaterfalls.app.AppInfoSettingsFragment.OnExpansionFilesDownloadListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AppInfoActivity extends SherlockFragmentActivity
        implements IDownloaderClient, OnExpansionFilesDownloadListener {
    
    private static String TAG = "AppInfoActivity";
    public static final String PREFS_NAME = "AppSettingsPreferences";
    private static final String USER_PREF_PAUSE_DOWNLOAD = "UserPrefPauseDownload";
    private ActionBar actionBar;
    private IDownloaderService mRemoteService;
    private IStub mDownloaderClientStub;
    private boolean mCancelValidation;
    private boolean mNeedsDownload;
    private boolean mUserPrefPauseDownload;
    
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Sherlock);
        super.onCreate(savedInstanceState);
        
        actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        ActionBar.Tab tab1 = actionBar.newTab();
        tab1.setText("SETTINGS");
        tab1.setTabListener(new TabListener<AppInfoSettingsFragment>(
                                this,
                                "AppInfoSettings",
                                AppInfoSettingsFragment.class));
        actionBar.addTab(tab1, true);

        ActionBar.Tab tab2 = actionBar.newTab();
        tab2.setText("INFO");
        tab2.setTabListener(new TabListener<AppInfoFragment>(
                                this,
                                "AppInfo",
                                AppInfoFragment.class));
        actionBar.addTab(tab2);

        ActionBar.Tab tab3 = actionBar.newTab();
        tab3.setText("LICENSE");
        tab3.setTabListener(new TabListener<AppInfoLicenseFragment>(
                                this, "AppInfoLicense",
                                AppInfoLicenseFragment.class));
        actionBar.addTab(tab3);
        
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        mUserPrefPauseDownload = settings.getBoolean(USER_PREF_PAUSE_DOWNLOAD, false);
        
        if(!mUserPrefPauseDownload && !ExpansionDownloaderService.expansionFilesDownloaded(this)){
            // Tell the activity to create download notification.
            // TODO: Only if the download is not already in progress.
            mNeedsDownload = buildPendingDownloadIntent();
        }
    }

    public boolean buildPendingDownloadIntent(){
        try {
            Log.d(TAG, "Building download pending intent.");
            // Build the PendingIntent with which to open this activity from the notification
            Intent launchIntent = AppInfoActivity.this.getIntent();
            Intent notificationRelaunchIntent = new Intent(AppInfoActivity.this, AppInfoActivity.this.getClass());
            notificationRelaunchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            notificationRelaunchIntent.setAction(launchIntent.getAction());
            
            if (launchIntent.getCategories() != null) {
                for (String category : launchIntent.getCategories()) {
                    notificationRelaunchIntent.addCategory(category);
                }
            }
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    AppInfoActivity.this, 0, notificationRelaunchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            
            // Request to start the download
            int startResult = DownloaderClientMarshaller.startDownloadServiceIfRequired(
                    AppInfoActivity.this, pendingIntent, ExpansionDownloaderService.class);
            
            Log.d(TAG, "Started download service (if required); result was: " + startResult);
            
            if (startResult != DownloaderClientMarshaller.NO_DOWNLOAD_REQUIRED) {
                Log.d(TAG, "Expansion file download required!");
                mDownloaderClientStub = DownloaderClientMarshaller.CreateStub(this, ExpansionDownloaderService.class);
                // Return to tell the Fragment to build its download UI
                return true;
            }
        } catch (NameNotFoundException e) {
            Log.d(TAG, "Could not find package.");
            e.printStackTrace();
            return false;
        }
        Log.d(TAG, "Expansion file download not required.");
        return false;
    }

    // Connect the stub to our service on resume.
    @Override
    protected void onResume() {
        if (null != mDownloaderClientStub) {
            Log.d(TAG, "Connecting downloader client stub.");
            mDownloaderClientStub.connect(this);
        } else {
            Log.d(TAG, "Downloader client stub was still null!");
        }
        super.onResume();
    }

    // Disconnect the stub from our service on stop
    @Override
    protected void onStop() {
        if (null != mDownloaderClientStub) {
            mDownloaderClientStub.disconnect(this);
        }
        super.onStop();
        
        // Save user download pause preference.
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(USER_PREF_PAUSE_DOWNLOAD, mUserPrefPauseDownload);
        editor.commit();
    }
    
    protected String readTxtFile(int resourceId){
        // TODO: Move this somewhere that makes sense.
        InputStream inputStream = getResources().openRawResource(resourceId);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int i;
        try {
            i = inputStream.read();
            while (i != -1){
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();
        } catch (IOException e) {
             e.printStackTrace();
        }
        return byteArrayOutputStream.toString();
    }
    
    private AppInfoSettingsFragment getSettingsFragment(){
        return (AppInfoSettingsFragment) getSupportFragmentManager().findFragmentByTag("AppInfoSettings");
    }
      
    public boolean getUserPrefPauseDownload(){
        Log.d(TAG, "Download pause preference is: " + mUserPrefPauseDownload);
        return mUserPrefPauseDownload;
    }
    
    public void setUserPrefPauseDownload(boolean paused){
        // true to stop download; false to allow
        Log.d(TAG, "Setting pause download preference to " + paused);
        mUserPrefPauseDownload = paused;
    }
    
    // OnExpansionFilesDownloadListener methods
    public boolean getNeedsDownload(){
        // Return whether we need download so UI can be crafted accordingly.
        Log.d(TAG, "Needs download: " + mNeedsDownload);
        return mNeedsDownload;
    }

    public void serviceRequestContinueDownload(){
        if(mRemoteService != null){
            mRemoteService.requestContinueDownload();
        } else {
            Toast.makeText(getApplicationContext(), "Download service not connected.", Toast.LENGTH_LONG).show();
        }
    }

    public void serviceRequestPauseDownload(){
        if(mRemoteService != null){
            mRemoteService.requestPauseDownload();
        } else {
            Toast.makeText(getApplicationContext(), "Download service not connected.", Toast.LENGTH_LONG).show();
        }
    }

    public void serviceRequestSetDownloadFlags(int flags){
        if(mRemoteService != null){
            mRemoteService.setDownloadFlags(flags);
        } else {
            Toast.makeText(getApplicationContext(), "Download service not connected.", Toast.LENGTH_LONG).show();
        }
    }
       
    // IDownloaderClient interface methods
    
    /**
     * Critical implementation detail. In onServiceConnected we create the
     * remote service and marshaler. This is how we pass the client information
     * back to the service so the client can be properly notified of changes. We
     * must do this every time we reconnect to the service.
     */
    @Override
    public void onServiceConnected(Messenger m){
        Log.d(TAG, "Download service connected.");
        mRemoteService = DownloaderServiceMarshaller.CreateProxy(m);
        mRemoteService.onClientUpdated(mDownloaderClientStub.getMessenger());
    }
    
    @Override
    public void onDownloadStateChanged(int newState) {
        // Forward state changes to the fragment containing the download UI
        // TODO: Only bother if the fraggy is visible
        Log.d(TAG, "Download state changed.");
        AppInfoSettingsFragment fragmentWithDownloadUI = getSettingsFragment();
        if(fragmentWithDownloadUI != null){
            fragmentWithDownloadUI.onDownloadStateChanged(newState);
        }
    }
    
    @Override
    public void onDownloadProgress(DownloadProgressInfo progress) {
        // Forward progress to the fragment containing the download UI
        Log.d(TAG, "Download progress received.");
        AppInfoSettingsFragment fragmentWithDownloadUI = getSettingsFragment();
        if(fragmentWithDownloadUI != null){
            fragmentWithDownloadUI.onDownloadProgress(progress);
        }
    }
    
    @Override
    protected void onDestroy() {
        this.mCancelValidation = true; // Where was this again?
        super.onDestroy();
    }

}
