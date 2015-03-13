/*
 * Copyright 2014 WNCOutdoors.info
 * portions Copyright 2014 Dan Dyer - Stack Overflow
 * http://stackoverflow.com/questions/3997229/sending-png-attachment-via-android-gmail-app?rq=1
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
 * FullScreenImageActivity.java
 * Class which displays a waterfall image full screen when tapped in the
 * information activity.
 */

package info.wncwaterfalls.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import info.wncwaterfalls.app.grid.ImageLoader;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

public class FullScreenImageActivity extends ActionBarActivity 
        implements ShareActionProvider.OnShareTargetSelectedListener{

    private final String TAG = "FullScreenActivity";

    private ShareActionProvider mShareActionProvider;
    private ImageLoader mImgLoader;
    private int mImageWidth;
    private String mImgFileName;
    private int mImgResourceId;
    private String mWaterfallName;
    private long mWaterfallId;
    private ActionBar actionBar;
    
    private static AttrDatabase mDb = null;
    
    public static final String APP_PREFS_NAME = "AppSettingsPreferences";
    public static final String USER_PREF_SHARED_WF = "SharedWaterfalls";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDb = new AttrDatabase(getApplicationContext());
        
        setContentView(R.layout.activity_full_screen_image);

        Intent intent = getIntent();
        mImgFileName = intent.getStringExtra("info.wncwaterfalls.app.IMAGE_FN");
        mWaterfallName = intent.getStringExtra("info.wncwaterfalls.app.WF_NAME");
        mWaterfallId = intent.getLongExtra("info.wncwaterfalls.app.WF_ID", 0);
        
        // Set title
        setTitle(mWaterfallName);
        
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
    }
    
    @Override
    public void onResume(){
        super.onResume();

        if(mImgLoader == null){
            mImageWidth = getResources().getDisplayMetrics().widthPixels;
            mImgResourceId = getResources().getIdentifier(mImgFileName, "drawable", getPackageName());
    
            // Create an image loader. Turn off memory caching.
            mImgLoader = new ImageLoader(this, false);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        getMenuInflater().inflate(R.menu.fullscreen_image_actions, menu);
        MenuItem item = menu.findItem(R.id.menu_item_waterfall_image_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        mShareActionProvider.setOnShareTargetSelectedListener(this);
        Intent intent = getDefaultShareIntent();
        if(intent != null){
            mShareActionProvider.setShareIntent(intent);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private Intent getDefaultShareIntent(){
        // Copy the image to the media store for sharing.
        Uri media_store_uri;
        boolean failed = false;
        
        // See if image is already in media store
        String[] projection = {MediaStore.Images.Media._ID};
        String selection = MediaStore.Images.Media.TITLE + "=?";
        String [] selectionArgs = {mWaterfallName};
        Cursor c = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);
        
        if(c != null && c.getCount() > 0){
            c.moveToFirst();
            media_store_uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getString(0));
        } else {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, mWaterfallName);
            values.put(MediaStore.Images.Media.DESCRIPTION, "This is " + mWaterfallName + ".");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            
            media_store_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            // Open bitmap and recompress directly to media store.
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), mImgResourceId);
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
            
            // Done copying image to media store. Recycle it.
            bitmap.recycle();
            bitmap = null;
        }

        // Safe to display image now
        populateImageView();

        // Create intent and add image to it
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpeg");

        intent.putExtra(Intent.EXTRA_SUBJECT, "Photo from WNCWaterfalls.info");
        intent.putExtra(Intent.EXTRA_TEXT, "This is " + mWaterfallName + ".");
        if(!failed){
            intent.putExtra(Intent.EXTRA_STREAM, media_store_uri);
        } else {
            Context context = getApplicationContext();
            CharSequence text = "Oops...couldn't add image attachment :(";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show(); 
        }

        return intent;
    }
    
    private void populateImageView(){
        ImageView full_screen_imageview = (ImageView) findViewById(R.id.full_screen_image);
        if(full_screen_imageview == null){
            //Log.d(TAG, "Image view is null!");
        } else {
            mImgLoader.displayImage(mImgFileName, full_screen_imageview, this, mImageWidth, mImageWidth);
        }
    }

    public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
        // Shared. Save our share to preferences & to the db
        // Create a new array to hold the prefs.
        JSONArray sharedWfs = new JSONArray();

        SharedPreferences appPrefs = getSharedPreferences(APP_PREFS_NAME, 0);
        
        // Load existing shares.
        String sharedWfJson = appPrefs.getString(USER_PREF_SHARED_WF, "[]");
        if(sharedWfJson != null){
            try {
                sharedWfs = new JSONArray(sharedWfJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        // Add this id and commit
        sharedWfs.put(mWaterfallId);

        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putString(USER_PREF_SHARED_WF, sharedWfs.toString());
        editor.commit();

        String table = "waterfalls";
        ContentValues values = new ContentValues(1);
        values.put("shared", 1);
        String whereClause = "_id = ?";
        String[] whereArgs = {String.valueOf(mWaterfallId)};
        int rowsUpdated = mDb.update(table, values, whereClause, whereArgs);
        return false;
    }
}
