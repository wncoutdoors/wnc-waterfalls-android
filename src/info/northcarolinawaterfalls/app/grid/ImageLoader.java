/* Adapted from:
 * https://github.com/maurycyw/StaggeredGridViewDemo/
 */

package info.northcarolinawaterfalls.app.grid;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

import info.northcarolinawaterfalls.app.R;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Load an image, optionally caching it to memory
 */
public class ImageLoader {
    private Context mContext;
    private MemoryCache mMemoryCache;
    private boolean mUseCache = true;
    private static Bitmap mPlaceholderBitmap;

    private static final String TAG = "ImageLoader";   

    /**
     * Constructor which creates the ImageLoader with optional caching.
     */
    public ImageLoader(Context context, boolean useCache){
        mContext = context; // TODO: Make sure this is not a memory leak
        mUseCache = useCache;
        if(useCache){
            mMemoryCache = new MemoryCache(); // Yay! This can be on the main thread!
            mPlaceholderBitmap = BitmapFactory.decodeResource(
                    context.getResources(), R.drawable.ic_launcher);
        }
    }

    /**
     * Constructor which creates the ImageLoader
     */
    public ImageLoader(Context context){
        this(context, true);
    }

    /**
     * Get an asynchronous task to load the bitmap
     */
    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
         }
         return null;
     }
    
    /**
     * Checks if another running task is already associated with the ImageView.
     * If we're lucky the ID it's loading will already be the one we want, and
     * we can just let it finish.
     */    
    public static boolean cancelPotentialWork(int resId, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final int bitmapId = bitmapWorkerTask.resId;
            if (bitmapId != resId) {
                // ID's don't match. Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // ID's match. The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    /**
     * Main external method. Displays the image named in the filename in the given imageView. 
     */
    public void displayImage(String fn, ImageView imageView, Context context, int reqWidth, int reqHeight){
        // Get the resource id from filename
        int resId = context.getResources().getIdentifier(fn , "drawable", context.getPackageName());
        Log.d(TAG, "Displaying image with resource id: " + resId);
        boolean memoryCacheMiss = false;
        if(mUseCache){
            Log.d(TAG, "Checking memory cache.");
            // See if the image is already cached in memory; if so, we can use it directly
            // TODO: Check image view dimensions against cached image. If cached image is too
            // small, skip.
            final Bitmap memCachebitmap = mMemoryCache.getBitmap(fn);
            if(memCachebitmap != null) {
                // Display it and done.
                Log.d(TAG, "Image was in memory cache; displaying");
                imageView.setImageBitmap(memCachebitmap);
            } else {
                Log.d(TAG, "Cache miss; decoding image");
                memoryCacheMiss = true;
            }
        }
        
        if(!mUseCache || memoryCacheMiss){
            Log.d(TAG, "Not checking memory cache for image..");
            // See if we've already got a task associated with this id for this ImageView
            if (cancelPotentialWork(resId, imageView)) {
                // Disk cached? Maybe. Either way we're loading from disk, so do it async.
                final BitmapWorkerTask task = new BitmapWorkerTask(imageView, mUseCache);
                
                // Go ahead and set the drawable to our placeholder bitmap
                final AsyncDrawable asyncDrawable = new AsyncDrawable(
                        context.getResources(), mPlaceholderBitmap, task);
                imageView.setImageDrawable(asyncDrawable);
                
                // Now start our real task, which decodes from disc (cache or resources).
                // TODO: See if we can avoid stringifying and un-stringifying these
                task.execute(fn, String.valueOf(reqWidth), String.valueOf(reqHeight));
            }
        }
    }

    /**
     * Helper method to calculate an appropriate sample size ratio. 
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
    
        if (height > reqHeight || width > reqWidth) {
            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
    
            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
    
        return inSampleSize;
    }

    /**
     * Create an actual bitmap(!) from a resource id with requested size
     */
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * An asynchronous task to get the bitmap data from disk. 
     */
    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> mImageViewReference;
        private int resId;
        private int reqWidth;
        private int reqHeight;
        private String fn;
        private boolean mUseCache;

        public BitmapWorkerTask(ImageView imageView, boolean useCache) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            mImageViewReference = new WeakReference<ImageView>(imageView);
            mUseCache = useCache;
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            fn = params[0];
            String reqWidthStr = params[1];
            String reqHeightStr = params[2];
            reqWidth = Integer.parseInt(reqWidthStr);
            reqHeight = Integer.parseInt(reqHeightStr);
            resId = mContext.getResources().getIdentifier(fn , "drawable", mContext.getPackageName());
            Log.d(TAG, "File name: " + fn);
            Log.d(TAG, "Package name: " + mContext.getPackageName());
            Log.d(TAG, "Resource id: " + resId);
            Log.d(TAG, "Executing image display with requested width: " + reqWidth + " and height: " + reqHeight);
            
            final Bitmap bitmap = decodeSampledBitmapFromResource(mContext.getResources(), resId, reqWidth, reqHeight);
            if(bitmap == null){
                Log.d(TAG, "Bitmap was null, o no!");
            } else {
                if(mUseCache){
                    Log.d(TAG, "Cache enabled; putting image in cache.");
                    mMemoryCache.addBitmap(fn, bitmap);
                }
            }
            return bitmap;
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (mImageViewReference != null && bitmap != null) {
                final ImageView imageView = mImageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask && imageView != null) {
                    Log.d(TAG, "Displaying image: " + fn);
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    public void clearCache(){
        mMemoryCache.clear();
    }
}
