package com.elsdoerfer.android.autostarts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.SimpleAdapter;

public class ListActivity extends android.app.ListActivity {
	
	static final String TAG = "Autostarts";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);    
       
        PackageManager pm = getPackageManager();
        

        // Unfortunately, looping through all packages/receivers currently doesn't allow us to 
        // retrieve the actual intents a receiver listens to. See the unimplemented 
        //     PackageManager.GET_INTENT_FILTERS
        // and: 
        //     http://groups.google.com/group/android-developers/browse_thread/thread/4502827143ea9b20
        //	   http://groups.google.com/group/android-developers/browse_thread/thread/ef0e4b390552f2c/  
        // 	   http://groups.google.com/group/android-developers/browse_thread/thread/3ba4f419f0bec3aa/ (note how the first answer by hackbod about finding all activities with no action is later retracted)
        /*ArrayList<HashMap<String, String>> receiverList = new ArrayList<HashMap<String, String>>();
        
        List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_RECEIVERS);
        for (int i=packages.size()-1; i>= 0; i--) {
        	PackageInfo p = packages.get(i);
        	Log.d(TAG, "Processing package "+p.packageName);
        	if (p.receivers == null) {
        		Log.d(TAG, "Package has no receivers.");
        		continue;
        	}
        	for (int j=p.receivers.length-1; j>=0; j--) {
        		 ActivityInfo info = p.receivers[j];
        		 if (info == null) {
        			 Log.v(TAG, "info is null (?), skipping");
        			 continue;
        		 }
        		 Log.d(TAG, "Found receiver: "+info.name);
        		 HashMap<String, String> dataset = new HashMap<String, String>();
        		 dataset.put("1", info.name);        
        		 dataset.put("2", (p.applicationInfo.name == null) ? p.applicationInfo.packageName : p.applicationInfo.name);
        		 receiverList.add(dataset);
        	}
        }
        
        setListAdapter(new SimpleAdapter(this, receiverList, R.layout.row, 
        		new String[] { "1", "2" }, new int[] { android.R.id.text1, android.R.id.text2 }));
        */
        
        ArrayList<HashMap<String, String>> receiverList = new ArrayList<HashMap<String, String>>();
                 
        String[] intents = { 
        		Intent.ACTION_AIRPLANE_MODE_CHANGED,
        		Intent.ACTION_BATTERY_CHANGED,
        		Intent.ACTION_BATTERY_LOW,         		
        		Intent.ACTION_BOOT_COMPLETED,
        		Intent.ACTION_CAMERA_BUTTON,
        		Intent.ACTION_CLOSE_SYSTEM_DIALOGS,
        		Intent.ACTION_CONFIGURATION_CHANGED,
        		Intent.ACTION_DATE_CHANGED, 
        		Intent.ACTION_DEVICE_STORAGE_LOW,
        		Intent.ACTION_DEVICE_STORAGE_OK, 
        		Intent.ACTION_GTALK_SERVICE_CONNECTED, 
        		Intent.ACTION_GTALK_SERVICE_DISCONNECTED,
        		Intent.ACTION_HEADSET_PLUG, 
        		Intent.ACTION_INPUT_METHOD_CHANGED, 
        		Intent.ACTION_MANAGE_PACKAGE_STORAGE,
        		Intent.ACTION_MEDIA_BAD_REMOVAL,
        		Intent.ACTION_MEDIA_BUTTON,
        		Intent.ACTION_MEDIA_CHECKING,
        		Intent.ACTION_MEDIA_EJECT,
        		Intent.ACTION_MEDIA_MOUNTED,
        		Intent.ACTION_MEDIA_NOFS,
        		Intent.ACTION_MEDIA_REMOVED,
        		Intent.ACTION_MEDIA_SCANNER_FINISHED,
        		Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
        		Intent.ACTION_MEDIA_SCANNER_STARTED,
        		Intent.ACTION_MEDIA_SHARED,
        		Intent.ACTION_MEDIA_UNMOUNTABLE,
        		Intent.ACTION_MEDIA_UNMOUNTED,
        		Intent.ACTION_NEW_OUTGOING_CALL,
        		Intent.ACTION_PACKAGE_ADDED,
        		Intent.ACTION_PACKAGE_CHANGED,
        		Intent.ACTION_PACKAGE_DATA_CLEARED,
        		Intent.ACTION_PACKAGE_INSTALL,
        		Intent.ACTION_PACKAGE_REMOVED,
        		Intent.ACTION_PACKAGE_REPLACED,
        		Intent.ACTION_PACKAGE_RESTARTED,
        		Intent.ACTION_PROVIDER_CHANGED,
        		Intent.ACTION_REBOOT,
        		Intent.ACTION_SCREEN_OFF,
        		Intent.ACTION_SCREEN_ON,
        		Intent.ACTION_TIMEZONE_CHANGED,
        		Intent.ACTION_TIME_CHANGED,
        		Intent.ACTION_TIME_TICK,           // not through manifest components?
        		Intent.ACTION_UID_REMOVED,
        		Intent.ACTION_UMS_CONNECTED,
        		Intent.ACTION_UMS_DISCONNECTED,
        		Intent.ACTION_USER_PRESENT,
        		Intent.ACTION_WALLPAPER_CHANGED,        		        		
        		};
        
        for (String intent : intents) {
            Intent query = new Intent();
            query.setAction(intent);
            
	        List<ResolveInfo> receivers = pm.queryBroadcastReceivers(query, PackageManager.GET_INTENT_FILTERS);
	        for (int i=receivers.size()-1; i>=0; i--) {
	        	ResolveInfo r = receivers.get(i);	        	
	        	Log.d(TAG, "Found receiver: "+r.toString());
	        	
	        	if (r.activityInfo == null) {
	        		Log.d(TAG, "activityInfo is null?!");
	        		continue;
	        	}
		   		 
	        	HashMap<String, String> dataset = new HashMap<String, String>();
				dataset.put("2", (String) r.activityInfo.loadLabel(pm) + " (" + intent + ")"); 				
				// determine label
				//String label =
				dataset.put("1", r.activityInfo.name);
				receiverList.add(dataset);        	
	        }
        }
        
        setListAdapter(new SimpleAdapter(this, receiverList, R.layout.row, 
        		new String[] { "1", "2" }, new int[] { android.R.id.text1, android.R.id.text2 }));
             
    }
}