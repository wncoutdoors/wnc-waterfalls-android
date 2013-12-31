package info.northcarolinawaterfalls;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Messenger;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.android.vending.expansion.downloader.DownloadProgressInfo;
import com.google.android.vending.expansion.downloader.DownloaderClientMarshaller;
import com.google.android.vending.expansion.downloader.DownloaderServiceMarshaller;
import com.google.android.vending.expansion.downloader.IDownloaderClient;
import com.google.android.vending.expansion.downloader.IDownloaderService;
import com.google.android.vending.expansion.downloader.IStub;

import info.northcarolinawaterfalls.R;
import info.northcarolinawaterfalls.AppInfoSettingsFragment.OnExpansionFilesDownloadListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AppInfoActivity extends SherlockFragmentActivity
        implements IDownloaderClient, OnExpansionFilesDownloadListener {
    
    private static String TAG = "AppInfoActivity";
    
    private ActionBar actionBar;
    private IDownloaderService mRemoteService;
    private IStub mDownloaderClientStub;
    private boolean mCancelValidation;

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
    }
    
    // Connect the stub to our service on start.
    @Override
    protected void onStart() {
        if (null != mDownloaderClientStub) {
            mDownloaderClientStub.connect(this);
        }
        super.onStart();
    }
    
    // Disconnect the stub from our service on stop
    @Override
    protected void onStop() {
        if (null != mDownloaderClientStub) {
            mDownloaderClientStub.disconnect(this);
        }
        super.onStop();
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
    
    // OnExpansionFilesDownloadListener methods
    public boolean buildPendingDownloadIntent(){
        mDownloaderClientStub = DownloaderClientMarshaller.CreateStub(
                (IDownloaderClient) this, ExpansionDownloaderService.class);
        try {
            // Build the PendingIntent with which to open this activity from the notification
            Intent launchIntent = getIntent();
            Intent notificationRelaunchIntent = new Intent(this, this.getClass());
            notificationRelaunchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            notificationRelaunchIntent.setAction(launchIntent.getAction());
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, notificationRelaunchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            
            // Request to start the download
            int startResult = DownloaderClientMarshaller.startDownloadServiceIfRequired(
                    this, pendingIntent, ExpansionDownloaderService.class);
            
            if (startResult != DownloaderClientMarshaller.NO_DOWNLOAD_REQUIRED) {
                // Return to tell the Fragment to build its download UI
                return true;
            }
            
        } catch (NameNotFoundException e) {
            Log.d(TAG, "Could not find package.");
            e.printStackTrace();
            return false;
        }
        return false;
    }
   
    public void serviceRequestContinueDownload(){
        mRemoteService.requestContinueDownload();
    }
    
    public void serviceRequestPauseDownload(){
        mRemoteService.requestPauseDownload();
    }
    
    public void serviceRequestSetDownloadFlags(int flags){
        mRemoteService.setDownloadFlags(flags);
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
        
        mRemoteService = DownloaderServiceMarshaller.CreateProxy(m);
        mRemoteService.onClientUpdated(mDownloaderClientStub.getMessenger());
    }
    
    @Override
    public void onDownloadStateChanged(int newState) {
        // Forward state changes to the fragment containing the download UI
        // TODO: Only bother if the fraggy is visible
        AppInfoSettingsFragment fragmentWithDownloadUI = getSettingsFragment();
        if(fragmentWithDownloadUI != null){
            fragmentWithDownloadUI.onDownloadStateChanged(newState);
        }
    }
    
    @Override
    public void onDownloadProgress(DownloadProgressInfo progress) {
        // Forward progress to the fragment containing the download UI
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
