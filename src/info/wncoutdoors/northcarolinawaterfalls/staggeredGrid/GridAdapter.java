package info.wncoutdoors.northcarolinawaterfalls.staggeredGrid;

import android.content.Context;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import info.wncoutdoors.northcarolinawaterfalls.R;

public class GridAdapter extends SimpleCursorAdapter {
    // Image loader will load a cached image, or load
    // image from disk, if not yet cached.
    private ImageLoader mImgLoader;
    private Context context;

    private static final String TAG="GridAdapter";

    public GridAdapter(Context context, int layout, String[] from, int[] to) {        
        super(context, layout, null, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        mImgLoader = new ImageLoader(context);
        this.context = context; // TODO: Make sure this is not a memory leak
        Log.d(TAG, "Grid adapter constructor complete.");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        ViewHolder holder;
        if(convertView==null){
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(R.layout.staggered_grid_element, null);
            holder = new ViewHolder();
            holder.thumbnail = (ScaleImageView) convertView.findViewById(R.id.scale_image_view);
            holder.caption = (TextView) convertView.findViewById(R.id.staggered_text_view);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.caption.setText(getCursor().getString(1));

        //mImgLoader.displayImage(getItem(position), holder.thumbnail);
        return convertView;
    }

    static class ViewHolder {
        ScaleImageView thumbnail;
        TextView caption;
    }
}
