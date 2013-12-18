package info.wncoutdoors.northcarolinawaterfalls;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.Arrays;
import java.util.List;

public class AttrDatabase extends SQLiteAssetHelper {
    private static final String TAG = "AttrDatabase";
    private static final String DATABASE_NAME = "attr";
    private static final int DATABASE_VERSION = 1;

    public static final List<String> COLUMNS = Arrays.asList(
        "_id", "attr_id", "geo_lat", "geo_lon", "name", "alt_names", "description", "height", "stream",
        "landowner", "elevation", "directions", "trail_directions", "trail_difficulty",
        "trail_difficulty_num", "trail_length", "trail_climb", "trail_climb_num",
        "trail_elevationlow", "trail_elevationhigh", "trail_elevationgain", "trail_tread",
        "trail_configuration", "photo", "photo_filename", "map_name", "shared");

    public AttrDatabase(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
