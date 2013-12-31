package info.northcarolinawaterfalls;

import android.util.Log;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class TabListener<T extends SherlockFragment> implements ActionBar.TabListener {
    // A generic tab listener to handle swapping of fragments within our outer Activities
    private static final String TAG = "TabListener";

    private SherlockFragment aFragment;
    private final SherlockFragmentActivity anActivity;
    private final String aFragTag;
    private final Class<T> aClass;

    public TabListener(SherlockFragmentActivity activity, String tag, Class<T> cls) {
        anActivity = activity;
        aFragTag = tag;
        aClass = cls;
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction transaction) {
        Log.d(TAG, "Inside onTabReselected");
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction transaction) {
        Log.d(TAG, "Inside onTabSelected, and transaction is " + transaction);
        SherlockFragment aFragment = (SherlockFragment) anActivity.getSupportFragmentManager().findFragmentByTag(aFragTag);
        // Check if the fragment is already initialized
        if(aFragment == null){
            // Create a new one
            aFragment = (SherlockFragment) SherlockFragment.instantiate(anActivity, aClass.getName());
            transaction.add(android.R.id.content, aFragment, aFragTag);
            Log.d(TAG, "Created new fragment: " + aFragment);
        } else {
            // Attach existing one
            transaction.attach(aFragment);
            Log.d(TAG, "Attached " + this.aFragment + " to transaction.");
        }
        this.aFragment = aFragment;
        Log.d(TAG, "aFragement was: " + (this.aFragment == null ? "null": "NOT null"));
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction transaction) {
        Log.d(TAG, "Inside onTabUnselected");
        if(aFragment != null){
            Log.d(TAG, "Removing fragment.");
            transaction.detach(aFragment);
        }
    }
}
