/* Adapted from:
 * https://github.com/maurycyw/StaggeredGridViewDemo/
 * and:
 * http://developer.android.com/training/displaying-bitmaps/cache-bitmap.html
 */

package info.northcarolinawaterfalls.grid;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

public class MemoryCache {
    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    final int cacheSize = maxMemory / 8;
    private LruCache<String, Bitmap> mMemoryCache;
    private static final String TAG = "MemoryCache";

    public MemoryCache(){
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return getByteCount(bitmap) / 1024;
            }
        };
        Log.d(TAG, "Cache created with size: " + String.valueOf(cacheSize));
    }

    public static final int getByteCount(Bitmap bitmap) {
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    public Bitmap getBitmap(String key){
        return mMemoryCache.get(key);
    }

    public void addBitmap(String key, Bitmap bitmap){
        if(getBitmap(key) == null){
            mMemoryCache.put(key, bitmap);
        }
    }
    
    public void clear(){
        
    }
}
