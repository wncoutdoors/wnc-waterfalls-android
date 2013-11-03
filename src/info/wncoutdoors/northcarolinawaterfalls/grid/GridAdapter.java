package info.wncoutdoors.northcarolinawaterfalls.grid;

import android.content.Context;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import info.wncoutdoors.northcarolinawaterfalls.AttrDatabase;
import info.wncoutdoors.northcarolinawaterfalls.R;

public class GridAdapter extends SimpleCursorAdapter {
    // Image loader will load a cached image, or load
    // image from disk, if not yet cached.
    private ImageLoader mImgLoader;
    private Context context;

    private static final String TAG = "GridAdapter";

    public GridAdapter(Context context, int layout, String[] from, int[] to) {        
        super(context, layout, null, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        mImgLoader = new ImageLoader(context);
        this.context = context; // TODO: Make sure this is not a memory leak
        Log.d(TAG, "Grid adapter constructor complete.");
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
            holder.thumbnail = (ScaleImageView) convertView.findViewById(R.id.scale_image_view);
            holder.caption = (TextView) convertView.findViewById(R.id.grid_text_view);
            
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.caption.setText(name);
        mImgLoader.displayImage(fnParts[0], holder.thumbnail, context);
        return convertView;
    }

    class ViewHolder {
        ScaleImageView thumbnail;
        TextView caption;
    }
    
}
