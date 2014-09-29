/*
 * Copyright 2014 WNCOutdoors.info
 * portions Copyright 2014 The Android Open Source Project
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
 * GridAdapter.java
 * Adapter for scrolling grid with ViewHolder pattern for recycling views.
 */
package info.wncwaterfalls.app.grid;

import android.content.Context;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import info.wncwaterfalls.app.AttrDatabase;
import info.wncwaterfalls.app.R;

public class GridAdapter extends SimpleCursorAdapter {
    // Image loader will load a cached image, or load
    // image from disk, if not yet cached.
    private ImageLoader mImgLoader;
    private Context context;
    private int mImageWidth;

    private static final String TAG = "GridAdapter";

    public GridAdapter(Context context, int layout, String[] from, int[] to) {        
        super(context, layout, null, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        mImgLoader = new ImageLoader(context);
        this.context = context; // TODO: Make sure this is not a memory leak
    }
    
    public void setImageWidth(int imageWidth){
        // Use an image width other than the default 150dp
        mImageWidth = imageWidth;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        getCursor().moveToPosition(position);
        String name = getCursor().getString(AttrDatabase.COLUMNS.indexOf("name"));
        String fileName = getCursor().getString(AttrDatabase.COLUMNS.indexOf("photo_filename"));
        String[] fnParts = fileName.split("\\.(?=[^\\.]+$)");
        
        // Called when the framework is ready to get a view for the grid.
        ViewHolder holder;
        if(convertView == null){
            // if it's not recycled, initialize some attributes
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(R.layout.grid_element, parent, false);
            holder = new ViewHolder();
            
            // See if scaling is adequate using these params
            holder.thumbnail = (ImageView) convertView.findViewById(R.id.grid_image_view);
            holder.caption = (TextView) convertView.findViewById(R.id.grid_text_view);
            
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.caption.setText(name);
        mImgLoader.displayImage(fnParts[0], holder.thumbnail, context, mImageWidth, mImageWidth);
        return convertView;
    }

    class ViewHolder {
        ImageView thumbnail;
        TextView caption;
    }
    
}
