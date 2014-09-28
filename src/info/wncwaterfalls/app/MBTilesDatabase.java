/*
 * Copyright 2014 WNCOutdoors.info
 * portions Copyright (C) 2011 readyState Software Ltd, 2007 The Android Open Source Project
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
 * MBTilesDatabase.java
 * Class which controls extraction of the MBTiles databases for offline maps
 * and provides access to the resulting file.
 */

package info.wncwaterfalls.app;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.android.vending.expansion.zipfile.APKExpansionSupport;
import com.android.vending.expansion.zipfile.ZipResourceFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MBTilesDatabase {
    private static final String TAG = "MBTilesDatabase";

    // Versions are pinned to the version at time of expansion file upload.
    private static final int MAIN_VERSION = 21;
    private static final int PATCH_VERSION = 21;

    private final Context mContext;

    private final String mDatabaseName;
    private final String mInternalMBTilesPath;

    private final String mExternalCacheDirPath;
    private final String mExternalCachedDatabasePath;
    
    private File mMBTilesDBFile;

    public MBTilesDatabase(Context context, String databaseName){
        mContext = context;
        mDatabaseName = databaseName;  // mbtiles export/database name (without extension)
        mInternalMBTilesPath = "mbtiles/" + databaseName + ".mbtiles";  // Path within expansion zip file
        // TODO: This returns null when external storage is not mounted.
        // Warn the user and stop.
        mExternalCacheDirPath = context.getExternalCacheDir() + "/mbtiles/";  // External dir we will unzip into
        mExternalCachedDatabasePath = mExternalCacheDirPath + databaseName + ".mbtiles";
        
        mMBTilesDBFile = new File(mExternalCachedDatabasePath);
    }

    public boolean dbFileExists(){
        return mMBTilesDBFile.exists();
    }

    public File getDBFile(){
        return mMBTilesDBFile;
    }

    public boolean extractDBFile(){
        // Run in an async task.
        if(!dbFileExists()){
            Log.d(TAG, "MBTiles db does not exist.");
            try {
                copyDatabaseFromExpansion();
                return true;
            } catch (MBTilesDatabaseException e) {
                Log.e(TAG, "Failed to copy from expansion file", e);               
                return false;
            }
        } else {
            Log.d(TAG, "MBTilesDatabase already unpacked.");
            return false;
        }
    }

    private void copyDatabaseFromExpansion() throws MBTilesDatabaseException{
        // To abort loading of MBTiles layer, throw MBTilesDatabaseException here,
        // which will result in a toast being displayed (but other map layers should
        // still load).
        Log.d(TAG, "Copying database from expansion file.");
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            Log.d(TAG, "External storage is accessible.");
            try{
                ZipResourceFile expansionFile = APKExpansionSupport.getAPKExpansionZipFile(
                    mContext, MAIN_VERSION, MAIN_VERSION);
                if(expansionFile == null){
                    MBTilesDatabaseException me = new MBTilesDatabaseException(
                            "getAPKExpansionZipFile returned null");
                    throw me;
                }
                Log.d(TAG, "Getting " + mInternalMBTilesPath + " from expansion file.");
                InputStream mbTileStream = expansionFile.getInputStream(mInternalMBTilesPath);
                if(mbTileStream == null){
                    MBTilesDatabaseException me = new MBTilesDatabaseException(
                            "expansionFile.getInputStream(" + mInternalMBTilesPath + ") returned null");
                    throw me;
                }

                // Create the external CACHE dir if it doesn't exist.
                File f = new File(mExternalCacheDirPath);
                if (!f.exists()) {
                    Log.d(TAG, "External cache dir does not exist; creating.");
                    f.mkdir();
                }
                FileOutputStream outStream = new FileOutputStream(mExternalCachedDatabasePath);
                writeExtractedFileToDisk(mbTileStream, outStream);
                Log.d(TAG, "Database copy complete.");
            } catch (FileNotFoundException fe) {
                MBTilesDatabaseException me = new MBTilesDatabaseException(
                        "Database file not found in expansion archive or destination not writeable.");
                me.setStackTrace(fe.getStackTrace());
                throw me;
            } catch (IOException ie){
                MBTilesDatabaseException me = new MBTilesDatabaseException(
                        "Unable to extract mbtiles db to external data dir.");
                me.setStackTrace(ie.getStackTrace());
                throw me;
            }
        } else {
            Log.d(TAG, "External storage is not mounted; cannot copy mbtiles db.");
        }
    }

    private void writeExtractedFileToDisk(InputStream zin, OutputStream outs) throws IOException {
        Log.d(TAG, "Writing extracted mbtiles file to external files dir.");
        byte[] buffer = new byte[1024];
        int length;
        while ((length = zin.read(buffer))>0){
            outs.write(buffer, 0, length);
        }
        outs.flush();
        outs.close();
        zin.close();
        Log.d(TAG, "Done extracting mbtiles file.");
    }
}
