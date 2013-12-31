
package info.northcarolinawaterfalls;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;

import com.google.android.vending.expansion.downloader.DownloaderClientMarshaller;
import com.google.android.vending.expansion.downloader.Helpers;
import com.google.android.vending.expansion.downloader.IDownloaderClient;
import com.google.android.vending.expansion.downloader.IStub;
import com.google.android.vending.expansion.downloader.impl.DownloaderService;

public class ExpansionDownloaderService extends DownloaderService {
    public static final String TAG = "ExpansionDownloaderService";
    public static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmjMxW8pCinwe50Q4fdlKgDbHErjOfozh2yXzhgN3sBGmwGP/ZtkfRc61Y+NJ46lyWeTNiLChJLlsaP8gQsmA/1F0bJoK2jUsz2r8AFBBcvvComRfvw1KDGUl6X0fTmYshGlSfA7r5kGk8/08xLxYWK/o2C4tzv54VcLUAfbcVYLtN9QnYsE/GtAw/Ctb5bxERINB5L5U6Evfo+MUHHlfogx3Tnhmu1hXIfwzUwYQ5cMzi3HJFJFoNI5Ym7UPofMASB4snjDPmm047NUuhRGcICRHI/z53j35wZ7WpQKRDfHJRsNjBE1HBWfREl/5la4Rtn4vq8aouWpROkRldN0i6QIDAQAB";
    public static final byte[] SALT = new byte[] { 64, 16, -82, 2, 1, 22,
        73, 91, 9, -48, 11, -69, -19, 82, 90, -107, 38, -61, -70, 83
    };
    
    @Override
    public String getPublicKey(){
        return BASE64_PUBLIC_KEY;
    }
    
    @Override
    public byte[] getSALT(){
        // mmmm...get some salt
        return SALT;
    }
    
    @Override
    public String getAlarmReceiverClassName() {
        return ExpansionAlarmReceiver.class.getName();
    }
      
    // Simple class to hold description of an expansion file.
    private static class XAPKFile {
        private final boolean mIsMain;
        private final int mFileVersion;
        private final long mFileSize;

        XAPKFile(boolean isMain, int fileVersion, long fileSize) {
            mIsMain = isMain;
            mFileVersion = fileVersion;
            mFileSize = fileSize;
        }
        
        public boolean isMain(){
            return mIsMain;
        }
        
        public int getFileVersion(){
            return mFileVersion;
        }
        
        public long getFileSize(){
            return mFileSize;
        }
    }
    
    private static final XAPKFile[] xAPKS = {
        new XAPKFile(
            true, // true signifies a main file
            1, // the version of the APK that the file was uploaded against
            287540566L // the length of the file in bytes
        )
    };
    
    public static boolean expansionFilesDownloaded(Context c){
        for (XAPKFile expansionFile : xAPKS) {
            String fileName = Helpers.getExpansionAPKFileName(
                    c, expansionFile.isMain(), expansionFile.getFileVersion());
            if(!Helpers.doesFileExist(c, fileName, expansionFile.getFileSize(), false)){
                return false;
            }
        }
        return true;
    }
    
    public XAPKFile[] getAPKFiles(){
        return xAPKS;
    }
}
