package info.wncoutdoors.northcarolinawaterfalls;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.actionbarsherlock.app.SherlockActivity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import info.wncoutdoors.northcarolinawaterfalls.grid.ImageLoader;

public class FullScreenImageActivity extends SherlockActivity 
        implements ShareActionProvider.OnShareTargetSelectedListener{

    private final String TAG = "FullScreenActivity";

    private ShareActionProvider mShareActionProvider;
    private ImageLoader mImgLoader;
    private int mImageWidth;
    private String mImgFileName;
    private int mImgResourceId;
    private String mWaterfallName;
    private long mWaterfallId;
    
    private static AttrDatabase mDb = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDb = new AttrDatabase(getApplicationContext());
        
        setContentView(R.layout.activity_full_screen_image);

        Log.d(TAG, "Inside FullScreenActivity onCreate");

        Intent intent = getIntent();
        mImgFileName = intent.getStringExtra("info.northcarolinawaterfalls.IMAGE_FN");
        mWaterfallName = intent.getStringExtra("info.northcarolinawaterfalls.WF_NAME");
        mWaterfallId = intent.getLongExtra("info.northcarolinawaterfalls.WF_ID", 0);
        Log.d(TAG, "Image filename to display: " + mImgFileName);
        ImageView full_screen_imageview = (ImageView) findViewById(R.id.full_screen_image);
        if(full_screen_imageview == null){
            Log.d(TAG, "Image view is null!");
        }

        mImageWidth = getResources().getDisplayMetrics().widthPixels;
        mImgResourceId = getResources().getIdentifier(mImgFileName, "drawable", getPackageName());

        // Create an image loader. Turn off caching.
        mImgLoader = new ImageLoader(this, false);
        mImgLoader.displayImage(mImgFileName, full_screen_imageview, this, mImageWidth, mImageWidth);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        getSupportMenuInflater().inflate(R.menu.fullscreen_image_actions, menu);
        MenuItem item = menu.findItem(R.id.menu_item_share);
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();
        mShareActionProvider.setOnShareTargetSelectedListener(this);
        Intent intent = getDefaultShareIntent();
        if(intent!=null){
            mShareActionProvider.setShareIntent(intent);
        }
        return super.onCreateOptionsMenu(menu);
    }
 
    private Intent getDefaultShareIntent(){
        // Copy the image to the media store for sharing.
        // http://stackoverflow.com/questions/3997229/sending-png-attachment-via-android-gmail-app?rq=1
        Log.d(TAG, "Inside getDefaultShareIntent");
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, mWaterfallName);
        values.put(MediaStore.Images.Media.DESCRIPTION, "This is " + mWaterfallName + ".");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        Uri media_store_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        // Open bitmap and recompress directly to media store.
        Log.d(TAG, "Copying image to media store");
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), mImgResourceId);
        boolean failed = false;
        OutputStream stream;
        try {
            stream = getContentResolver().openOutputStream(media_store_uri);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            failed = true;
        } catch (IOException e){
            e.printStackTrace();
            failed = true;
        }
        Log.d(TAG, "Image copied to media store.");

        // Create intent and add image to it
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpeg");
        
        intent.putExtra(Intent.EXTRA_SUBJECT, "Photo from NorthCarolinaWaterfalls.info");
        intent.putExtra(Intent.EXTRA_TEXT, "This is " + mWaterfallName + ".");
        if(!failed){
            intent.putExtra(Intent.EXTRA_STREAM, media_store_uri);
            Log.d(TAG, "Intent created with image as stream.");
        } else {
            Context context = getApplicationContext();
            CharSequence text = "Oops...couldn't add image attachment :(";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show(); 
        }
        return intent;
    }

    public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
        // Shared. Save our share to the db
        Log.d(TAG, "Saving share to DB");
        String table = "waterfalls";
        ContentValues values = new ContentValues(1);
        values.put("shared", 1);
        String whereClause = "_id = ?";
        String[] whereArgs = {String.valueOf(mWaterfallId)};
        int rowsUpdated = mDb.update(table, values, whereClause, whereArgs);
        Log.d(TAG, rowsUpdated + "rows updated.");
        return false;
    }
}
