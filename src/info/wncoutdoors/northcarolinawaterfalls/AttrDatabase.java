package info.wncoutdoors.northcarolinawaterfalls;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

public class AttrDatabase extends SQLiteAssetHelper {
    private static final String TAG = "AttrDatabase";
    private static final String DATABASE_NAME = "attr";
    private static final int DATABASE_VERSION = 1;
    
    public AttrDatabase(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    public Cursor getCount(){
        Log.d(TAG, "Getting count from waterfalls database.");
        
        // Get a count of records in the database.
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = db.rawQuery("SELECT COUNT(*) AS total FROM waterfalls", null);
        c.moveToFirst();
        return c;
    }
}
