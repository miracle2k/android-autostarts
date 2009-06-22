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

	// The broadcast intents we support. Read about why this needs to be a
	// predefined, static list below in the comments on where we do the
	// actual loading of the installed receivers.
	//
	// This currently is a list of all intents in the android.content.Intent
	// namespace that are marked as "broadcast intents" in their documentation.
	static final Object[][] supportedIntents = {
		{ Intent.ACTION_AIRPLANE_MODE_CHANGED, R.string.act_airplane_mode_changed },
		{ Intent.ACTION_BATTERY_CHANGED, R.string.act_battery_changed },
		{ Intent.ACTION_BATTERY_LOW, R.string.act_battery_low },
		{ Intent.ACTION_BOOT_COMPLETED, R.string.act_boot_completed },
		{ Intent.ACTION_CAMERA_BUTTON, R.string.act_camera_button },
		{ Intent.ACTION_CLOSE_SYSTEM_DIALOGS, R.string.act_close_system_dialogs },
		{ Intent.ACTION_CONFIGURATION_CHANGED, R.string.act_configuration_changed },
		{ Intent.ACTION_DATE_CHANGED,  R.string.act_date_changed },
		{ Intent.ACTION_DEVICE_STORAGE_LOW, R.string.act_device_storage_low },
		{ Intent.ACTION_DEVICE_STORAGE_OK,  R.string.act_device_storage_ok },
		{ Intent.ACTION_GTALK_SERVICE_CONNECTED,  R.string.act_gtalk_service_connected },
		{ Intent.ACTION_GTALK_SERVICE_DISCONNECTED, R.string.act_gtalk_service_disconnected },
		{ Intent.ACTION_HEADSET_PLUG,  R.string.act_headset_plug },
		{ Intent.ACTION_INPUT_METHOD_CHANGED,  R.string.act_input_method_changed },
		{ Intent.ACTION_MANAGE_PACKAGE_STORAGE, R.string.act_manage_package_storage },
		{ Intent.ACTION_MEDIA_BAD_REMOVAL, R.string.act_media_bad_removal },
		{ Intent.ACTION_MEDIA_BUTTON, R.string.act_media_button },
		{ Intent.ACTION_MEDIA_CHECKING, R.string.act_media_checking },
		{ Intent.ACTION_MEDIA_EJECT, R.string.act_media_eject },
		{ Intent.ACTION_MEDIA_MOUNTED, R.string.act_media_mounted },
		{ Intent.ACTION_MEDIA_NOFS, R.string.act_media_nofs },
		{ Intent.ACTION_MEDIA_REMOVED, R.string.act_media_removed },
		{ Intent.ACTION_MEDIA_SCANNER_FINISHED, R.string.act_media_scanner_finished },
		{ Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, R.string.act_media_scanner_scan_file },
		{ Intent.ACTION_MEDIA_SCANNER_STARTED, R.string.act_media_scanner_started },
		{ Intent.ACTION_MEDIA_SHARED, R.string.act_media_shared },
		{ Intent.ACTION_MEDIA_UNMOUNTABLE, R.string.act_media_unmountable },
		{ Intent.ACTION_MEDIA_UNMOUNTED, R.string.act_media_unmounted },
		{ Intent.ACTION_NEW_OUTGOING_CALL, R.string.act_new_outgoing_call },
		{ Intent.ACTION_PACKAGE_ADDED, R.string.act_package_added },
		{ Intent.ACTION_PACKAGE_CHANGED, R.string.act_package_changed },
		{ Intent.ACTION_PACKAGE_DATA_CLEARED, R.string.act_package_data_cleared },
		{ Intent.ACTION_PACKAGE_INSTALL, R.string.act_package_install },
		{ Intent.ACTION_PACKAGE_REMOVED, R.string.act_package_removed },
		{ Intent.ACTION_PACKAGE_REPLACED, R.string.act_package_replaced },
		{ Intent.ACTION_PACKAGE_RESTARTED, R.string.act_package_restarted },
		{ Intent.ACTION_PROVIDER_CHANGED, R.string.act_provider_changed },
		{ Intent.ACTION_REBOOT, R.string.act_reboot },
		{ Intent.ACTION_SCREEN_OFF, R.string.act_screen_off },
		{ Intent.ACTION_SCREEN_ON, R.string.act_screen_on },
		{ Intent.ACTION_TIMEZONE_CHANGED, R.string.act_timezone_changed },
		{ Intent.ACTION_TIME_CHANGED, R.string.act_time_changed },
		{ Intent.ACTION_TIME_TICK, R.string.act_time_tick },           // not through manifest components?
		{ Intent.ACTION_UID_REMOVED, R.string.act_uid_removed },
		{ Intent.ACTION_UMS_CONNECTED, R.string.act_ums_connected },
		{ Intent.ACTION_UMS_DISCONNECTED, R.string.act_ums_disconnected },
		{ Intent.ACTION_USER_PRESENT, R.string.act_user_present },
		{ Intent.ACTION_WALLPAPER_CHANGED, R.string.act_wallpaper_changed }
    };

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

        for (Object[] intent : supportedIntents) {
            Intent query = new Intent();
            query.setAction((String)(intent[0]));

	        List<ResolveInfo> receivers = pm.queryBroadcastReceivers(query, PackageManager.GET_INTENT_FILTERS);
	        for (int i=receivers.size()-1; i>=0; i--) {
	        	ResolveInfo r = receivers.get(i);
	        	Log.d(TAG, "Found receiver: "+r.toString());

	        	if (r.activityInfo == null) {
	        		Log.d(TAG, "activityInfo is null?!");
	        		continue;
	        	}

	        	HashMap<String, String> dataset = new HashMap<String, String>();
				dataset.put("2", (String) r.activityInfo.loadLabel(pm));
				// determine label
				//String label =
				//  r.activityInfo.name - raw activity name
				dataset.put("1", getResources().getString((Integer)intent[1]));
				receiverList.add(dataset);
	        }
        }

        setListAdapter(new SimpleAdapter(this, receiverList, R.layout.row,
        		new String[] { "1", "2" }, new int[] { android.R.id.text1, android.R.id.text2 }));

    }
}