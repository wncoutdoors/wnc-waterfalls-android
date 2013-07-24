package info.wncoutdoors.northcarolinawaterfalls;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

public class AttrDatabase extends SQLiteAssetHelper {
    private static final String TAG = "AttrDatabase";
    private static final String DATABASE_NAME = "attr";
    private static final int DATABASE_VERSION = 1;
    
    public AttrDatabase(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    public Cursor rawQuery(String query){
        Log.d(TAG, "Getting count from waterfalls database.");
        
        // Get a count of records in the database.
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery(query, null);
        c.moveToFirst();
        return c;
    }
}