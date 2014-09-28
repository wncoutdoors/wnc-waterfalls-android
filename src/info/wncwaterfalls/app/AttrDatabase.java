/*
 * Copyright 2014 WNCOutdoors.info
 * portions Copyright 2014 The Android Open Source Project
 * portions Copyright (C) 2011 readyState Software Ltd
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
 * AttrDatabase.java
 * Class which controls access to the app's main attribute database, copying
 * it from the assets folder if needed.
 */
package info.wncwaterfalls.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.Arrays;
import java.util.List;

public class AttrDatabase extends SQLiteAssetHelper {
    private static final String TAG = "AttrDatabase";
    private static final String DATABASE_NAME = "attr";
    private static final int DATABASE_VERSION = 3;

    public static final List<String> COLUMNS = Arrays.asList(
        "_id", "attr_id", "geo_lat", "geo_lon", "name", "alt_names", "description", "height", "stream",
        "landowner", "elevation", "directions", "trail_directions", "trail_difficulty",
        "trail_difficulty_num", "trail_length", "trail_climb", "trail_climb_num",
        "trail_elevationlow", "trail_elevationhigh", "trail_elevationgain", "trail_tread",
        "trail_configuration", "photo", "photo_filename", "map_name", "shared");

    public AttrDatabase(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.setForcedUpgrade();
    }

    public Cursor rawQuery(String query, String[] args){
        // Proxy to db.rawQuery
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(query, null);
        c.moveToFirst();
        return c;
    }
    
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs){
        return getReadableDatabase().update(table, values, whereClause, whereArgs);
    }
}
