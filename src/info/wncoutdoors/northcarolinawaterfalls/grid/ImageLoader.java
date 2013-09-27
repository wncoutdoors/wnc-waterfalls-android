/* Adapted from:
 * https://github.com/maurycyw/StaggeredGridViewDemo/
 */

package info.wncoutdoors.northcarolinawaterfalls.grid;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import info.wncoutdoors.northcarolinawaterfalls.R;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Create a cached image, or load image from disk into cache, if not yet cached.
 */
public class ImageLoader {
    private Context context;
    private FileCache mFileCache;
    private MemoryCache mMemoryCache;
    private Map<ImageView, String> imageViews=Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
    private boolean mFileCacheStarting = true;
    private final Object mFileCacheLock = new Object();
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
    private static final String DISK_CACHE_SUBDIR = "thumbnails";
    private static Bitmap mPlaceholderBitmap;
    
    private static final String TAG = "ImageLoader";

    /**
     * Constructor which creates the ImageLoader and initializes the Async file cache
     * creator.
     */
    public ImageLoader(Context context){
        this.context = context; // TODO: Make sure this is not a memory leak
        mMemoryCache = new MemoryCache(); // Yay! This can be on the main thread!
        new InitFileCacheTask().execute(context);
        mPlaceholderBitmap = BitmapFactory.decodeResource(
                context.getResources(), R.drawable.ic_launcher);
    }

    /**
     * An asynchronous task to create the file cache, since that cannot happen on the
     * main thread. Gets created and executed in the constructor.
     */
    class InitFileCacheTask extends AsyncTask<Context, Void, Void> {
        @Override
        protected Void doInBackground(Context... params) {
            synchronized (mFileCacheLock) {
                Context context = params[0];
                mFileCacheStarting = false; // Finished initialization
                mFileCacheLock.notifyAll(); // Wake any waiting threads
                mFileCache = new FileCache(context, DISK_CACHE_SUBDIR, DISK_CACHE_SIZE);
            }
            return null;
        }
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
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    /**
     * Main external interface. Displays the image named in the filename in the given imageView. 
     */
    public void displayImage(String fn, ImageView imageView, Context context){
        Log.d(TAG, "Displaying image: " + fn);
                
        // Get the resource id from filename
        int resId = context.getResources().getIdentifier(fn , "drawable", context.getPackageName());
        Log.d(TAG, "Resource id for said image: " + resId);
        
        // See if the image is already cached in memory
        final Bitmap memCachebitmap = mMemoryCache.getBitmap(fn);
        if(memCachebitmap != null) {
            // Display it and done.
            imageView.setImageBitmap(memCachebitmap);
        } else {
            // See if we've already got a task associated with this id for this ImageView
            if (cancelPotentialWork(resId, imageView)) {
                // Disk cached? Maybe. Either way we're loading from disk, so async.
                final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
                final AsyncDrawable asyncDrawable = new AsyncDrawable(
                        context.getResources(), mPlaceholderBitmap, task);
                imageView.setImageDrawable(asyncDrawable);
                task.execute(fn);
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
        private final WeakReference<ImageView> imageViewReference;
        private int resId;
        private String fn;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            fn = params[0];
            Log.d(TAG, "File name: " + fn);
            Log.d(TAG, "Package name: " + context.getPackageName());
            resId = context.getResources().getIdentifier(fn , "drawable", context.getPackageName());
            Log.d(TAG, "Resource id: " + resId);
            final Bitmap bitmap = decodeSampledBitmapFromResource(context.getResources(), resId, 100, 100);
            if(bitmap == null){
                Log.d(TAG, "Bitmap was null, o no!");
            } else {
                mMemoryCache.addBitmap(fn, bitmap);
                mFileCache.put(fn, bitmap);
            }
            return bitmap;
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask && imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    public void clearCache(){
        mMemoryCache.clear();
        mFileCache.clear();
    }
}
