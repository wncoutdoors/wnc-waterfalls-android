/*
 * Copyright 2014 WNCOutdoors.info
 * portions Copyright 2013 The Android Open Source Project
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
 * MemoryCache.java
 * A place to cache thumbnails so they're quicker to load in our scrolling
 * image views.
 */

package info.wncwaterfalls.app.grid;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

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
        // TODO: Yeah.
    }
}
