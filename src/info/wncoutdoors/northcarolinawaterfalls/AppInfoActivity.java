package info.wncoutdoors.northcarolinawaterfalls;

import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AppInfoActivity extends SherlockFragmentActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_info);
        
        TextView supportView = (TextView) findViewById(R.id.app_info_support_textview);
        supportView.setText(readTxtFile(R.raw.support_info));

        TextView attributionView = (TextView) findViewById(R.id.app_info_attribution_textview);
        attributionView.setText(readTxtFile(R.raw.attribution));
        
        TextView licenseView = (TextView) findViewById(R.id.app_info_license_textview);
        licenseView.setText(readTxtFile(R.raw.license));
    }
    
    private String readTxtFile(int resourceId){
        InputStream inputStream = getResources().openRawResource(resourceId);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int i;
        try {
            i = inputStream.read();
            while (i != -1){
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();
        } catch (IOException e) {
             e.printStackTrace();
        }        
        return byteArrayOutputStream.toString();
    }
}
