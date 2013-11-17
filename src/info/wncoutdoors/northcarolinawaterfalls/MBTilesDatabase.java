package info.wncoutdoors.northcarolinawaterfalls;

/*
 * Portions of this file adapted from android-sqlite-asset-helper
 * Copyright (C) 2011 readyState Software Ltd, 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.android.vending.expansion.zipfile.APKExpansionSupport;
import com.android.vending.expansion.zipfile.ZipResourceFile;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipInputStream;

public class MBTilesDatabase {
    private static final String TAG = "MBTilesDatabase";
    private static final int DATABASE_VERSION = 1;

    private final Context mContext;

    private final String mDatabaseName;
    private final String mInternalMBTilesPath;

    private final String mExternalFilesDirPath;
    private final String mExternalFilesDatabasePath;

    public MBTilesDatabase(Context context, String databaseName){
        mContext = context;
        mDatabaseName = databaseName;  // mbtiles export/database name (without extension)
        mInternalMBTilesPath = "mbtiles/" + databaseName + ".mbtiles";  // Path within expansion zip file
        mExternalFilesDirPath = context.getExternalFilesDir(null) + "/mbtiles/";  // External dir we will unzip into
        mExternalFilesDatabasePath = mExternalFilesDirPath + databaseName + ".mbtiles";
    }

    public File getDBFile(){
        // TODO: Make this async since extracting/copying may not be fast enough...
        File mbTilesDBFile = new File(mExternalFilesDatabasePath);
        if(!mbTilesDBFile.exists()){
            Log.d(TAG, "MBTiles db does not exist. Creating...");
            try {
                copyDatabaseFromExpansion();
            } catch (MBTilesDatabaseException e) {
                Log.e(TAG, "Failed to copy from expansion file", e);
                
                // Also notify - this impacts user experience.
                CharSequence error = "Failed to display offline map :(";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(mContext, error, duration);
                toast.show();
            }
        } else {
            Log.d(TAG, "MBTilesDatabase already unpacked.");
        }
        return mbTilesDBFile;
    }

    private void copyDatabaseFromExpansion() throws MBTilesDatabaseException{
        /* To abort loading of MBTiles layer, throw MBTilesDatabaseException here,
         * which will result in a toast being displayed (but other map layers should
         * still load).
         */
        Log.d(TAG, "Copying database from expansion file.");
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            Log.d(TAG, "External storage is accessible.");
            try{
                ZipResourceFile expansionFile = APKExpansionSupport.getAPKExpansionZipFile(
                    mContext, DATABASE_VERSION, DATABASE_VERSION);
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

                // Create the external files dir if it doesn't exist.
                File f = new File(mExternalFilesDirPath);
                if (!f.exists()) {
                    Log.d(TAG, "External files dir does not exist; creating.");
                    f.mkdir();
                }
                FileOutputStream outStream = new FileOutputStream(mExternalFilesDatabasePath);
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
