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
 * ExpansionDownloaderService.java
 * Class which implements the Expansion File Downloader Service.
 */
package info.northcarolinawaterfalls.app;

import android.content.Context;
import android.util.Log;

import com.google.android.vending.expansion.downloader.Helpers;
import com.google.android.vending.expansion.downloader.impl.DownloaderService;

public class ExpansionDownloaderService extends DownloaderService {
    public static final String TAG = "ExpansionDownloaderService";
    public static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmVZ4Lb5NyBNxw0sFz5WRBAGLnYmkdw0zU1TS4DRB7LXHRGSwzIfaVgYP2h8zJEfYxrX9dlysPVpVsJrz310lpIQTSjIhPIoPc7mZ1tWXk1wYxpnMSNnOXW8wG/Cb54x97onU7T5C9xQSoccuS84vV6OxqGt8ryyv8NedEbbOjYngURW2EVtTn06ShLCKQItQk/uSGzagRzhRBVfdptgjjx4HsO1O04aLYOP3gqwnsru4hzp2ZIrIe2sI+7Yx1LE6CX5uZT/c/wZXMpCyDCvVsIX8rlvnWSky03P+ii0Tjwb8mb11Xcj7Vz+zYweB68bl/IHzFmCVa3kZ/naCAkP8KwIDAQAB";
    public static final byte[] SALT = new byte[] { 64, 16, -82, 2, 1, 22, 73, 91, 9, -48, 11, -69,
        -19, 82, 90, -107, 38, -61, -70, 83
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
            14, // the version of the APK that the file was uploaded against
            64336158L // the length of the file in bytes
        )
    };
    
    public static boolean expansionFilesDownloaded(Context c){
        for (XAPKFile expansionFile : xAPKS) {
            String fileName = Helpers.getExpansionAPKFileName(
                    c, expansionFile.isMain(), expansionFile.getFileVersion());
            Log.d(TAG, "Expansion file name should be: " + fileName);
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
