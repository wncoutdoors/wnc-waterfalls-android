package info.wncoutdoors.northcarolinawaterfalls;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.io.File;

public class MBTilesDatabase extends SQLiteAssetHelper {
    private static final String TAG = "MBTilesDatabase";
    private static final int DATABASE_VERSION = 1;

    private String mDatabaseName;

    public MBTilesDatabase(Context context, String databaseName){
        super(context, databaseName, null, DATABASE_VERSION);
        mDatabaseName = databaseName;
    }

    public File getDBFile(){
        SQLiteDatabase db = getReadableDatabase();
        String filePath = db.getPath();
        Log.d(TAG, "Database file path: " + filePath);
        return new File(filePath);
    }
    
    // TODO: force upgrades in all cases. Probably not an issue, since we'll need to use
    // extras files.
}
