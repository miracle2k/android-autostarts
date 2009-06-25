package com.elsdoerfer.android.autostarts;

import java.util.ArrayList;
import java.util.List;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ListActivity extends ExpandableListActivity {

	static final String TAG = "Autostarts";

	// The broadcast intents we support. Read about why this needs to be a
	// predefined, static list below in the comments on where we do the
	// actual loading of the installed receivers.
	//
	// This currently is a list of all intents in the android.content.Intent
	// namespace that are marked as "broadcast intents" in their documentation.
	static final Object[][] supportedIntents = {
		{ Intent.ACTION_AIRPLANE_MODE_CHANGED, R.string.act_airplane_mode_changed, R.string.act_airplane_mode_changed_detail },
		{ Intent.ACTION_BATTERY_CHANGED, R.string.act_battery_changed, R.string.act_battery_changed_detail },
		{ Intent.ACTION_BATTERY_LOW, R.string.act_battery_low, R.string.act_battery_low_detail },
		{ Intent.ACTION_BOOT_COMPLETED, R.string.act_boot_completed, R.string.act_boot_completed_detail },
		{ Intent.ACTION_CAMERA_BUTTON, R.string.act_camera_button, R.string.act_camera_button_detail },
		{ Intent.ACTION_CLOSE_SYSTEM_DIALOGS, R.string.act_close_system_dialogs, R.string.act_close_system_dialogs_detail },
		{ Intent.ACTION_CONFIGURATION_CHANGED, R.string.act_configuration_changed, R.string.act_configuration_changed_detail },
		{ Intent.ACTION_DATE_CHANGED, R.string.act_date_changed, R.string.act_date_changed_detail },
		{ Intent.ACTION_DEVICE_STORAGE_LOW, R.string.act_device_storage_low, R.string.act_device_storage_low_detail },
		{ Intent.ACTION_DEVICE_STORAGE_OK, R.string.act_device_storage_ok, R.string.act_device_storage_ok_detail },
		{ Intent.ACTION_GTALK_SERVICE_CONNECTED, R.string.act_gtalk_service_connected, R.string.act_gtalk_service_connected_detail },
		{ Intent.ACTION_GTALK_SERVICE_DISCONNECTED, R.string.act_gtalk_service_disconnected, R.string.act_gtalk_service_disconnected_detail },
		{ Intent.ACTION_HEADSET_PLUG, R.string.act_headset_plug, R.string.act_headset_plug_detail },
		{ Intent.ACTION_INPUT_METHOD_CHANGED, R.string.act_input_method_changed, R.string.act_input_method_changed_detail },
		{ Intent.ACTION_MANAGE_PACKAGE_STORAGE, R.string.act_manage_package_storage, R.string.act_manage_package_storage_detail },
		{ Intent.ACTION_MEDIA_BAD_REMOVAL, R.string.act_media_bad_removal, R.string.act_media_bad_removal_detail },
		{ Intent.ACTION_MEDIA_BUTTON, R.string.act_media_button, R.string.act_media_button_detail },
		{ Intent.ACTION_MEDIA_CHECKING, R.string.act_media_checking, R.string.act_media_checking_detail },
		{ Intent.ACTION_MEDIA_EJECT, R.string.act_media_eject, R.string.act_media_eject_detail },
		{ Intent.ACTION_MEDIA_MOUNTED, R.string.act_media_mounted, R.string.act_media_mounted_detail },
		{ Intent.ACTION_MEDIA_NOFS, R.string.act_media_nofs, R.string.act_media_nofs_detail },
		{ Intent.ACTION_MEDIA_REMOVED, R.string.act_media_removed, R.string.act_media_removed_detail },
		{ Intent.ACTION_MEDIA_SCANNER_FINISHED, R.string.act_media_scanner_finished, R.string.act_media_scanner_finished_detail },
		{ Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, R.string.act_media_scanner_scan_file, R.string.act_media_scanner_scan_file_detail },
		{ Intent.ACTION_MEDIA_SCANNER_STARTED, R.string.act_media_scanner_started, R.string.act_media_scanner_started_detail },
		{ Intent.ACTION_MEDIA_SHARED, R.string.act_media_shared, R.string.act_media_shared_detail },
		{ Intent.ACTION_MEDIA_UNMOUNTABLE, R.string.act_media_unmountable, R.string.act_media_unmountable_detail },
		{ Intent.ACTION_MEDIA_UNMOUNTED, R.string.act_media_unmounted, R.string.act_media_unmounted_detail },
		{ Intent.ACTION_NEW_OUTGOING_CALL, R.string.act_new_outgoing_call, R.string.act_new_outgoing_call_detail },
		{ Intent.ACTION_PACKAGE_ADDED, R.string.act_package_added, R.string.act_package_added_detail },
		{ Intent.ACTION_PACKAGE_CHANGED, R.string.act_package_changed, R.string.act_package_changed_detail },
		{ Intent.ACTION_PACKAGE_DATA_CLEARED, R.string.act_package_data_cleared, R.string.act_package_data_cleared_detail },
		{ Intent.ACTION_PACKAGE_INSTALL, R.string.act_package_install, R.string.act_package_install_detail },
		{ Intent.ACTION_PACKAGE_REMOVED, R.string.act_package_removed, R.string.act_package_removed_detail },
		{ Intent.ACTION_PACKAGE_REPLACED, R.string.act_package_replaced, R.string.act_package_replaced_detail },
		{ Intent.ACTION_PACKAGE_RESTARTED, R.string.act_package_restarted, R.string.act_package_restarted_detail },
		{ Intent.ACTION_PROVIDER_CHANGED, R.string.act_provider_changed, R.string.act_provider_changed_detail },
		{ Intent.ACTION_REBOOT, R.string.act_reboot, R.string.act_reboot_detail },
		{ Intent.ACTION_SCREEN_OFF, R.string.act_screen_off, R.string.act_screen_off_detail },
		{ Intent.ACTION_SCREEN_ON, R.string.act_screen_on, R.string.act_screen_on_detail },
		{ Intent.ACTION_TIMEZONE_CHANGED, R.string.act_timezone_changed, R.string.act_timezone_changed_detail },
		{ Intent.ACTION_TIME_CHANGED, R.string.act_time_changed, R.string.act_time_changed_detail },
		{ Intent.ACTION_TIME_TICK, R.string.act_time_tick, R.string.act_time_tick_detail },           // not through manifest components?
		{ Intent.ACTION_UID_REMOVED, R.string.act_uid_removed, R.string.act_uid_removed_detail },
		{ Intent.ACTION_UMS_CONNECTED, R.string.act_ums_connected, R.string.act_ums_connected_detail },
		{ Intent.ACTION_UMS_DISCONNECTED, R.string.act_ums_disconnected, R.string.act_ums_disconnected_detail },
		{ Intent.ACTION_USER_PRESENT, R.string.act_user_present, R.string.act_user_present_detail },
		{ Intent.ACTION_WALLPAPER_CHANGED, R.string.act_wallpaper_changed, R.string.act_wallpaper_changed_detail }
    };

	static final private int MENU_FILTER_ID = 1;
	static final private int MENU_HELP_ID = 2;

	private Toast mInfoToast;
	private MyExpandableListAdapter mListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);

        // TODO: move this to an ASyncTask
        load();
    }

    @Override
	protected void onPause() {
		super.onPause();
		if (mInfoToast != null)
			mInfoToast.cancel();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_FILTER_ID, 0, R.string.menu_toggle_sys_apps).
			setIcon(R.drawable.ic_menu_view);
		menu.add(0, MENU_HELP_ID, 0, R.string.menu_help).
			setIcon(R.drawable.ic_menu_help);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_FILTER_ID:
			if (mListAdapter.toggleFilterSystemApps())
				((TextView)findViewById(android.R.id.empty)).setText(R.string.no_receivers_filtered);
			else
				((TextView)findViewById(android.R.id.empty)).setText(R.string.no_receivers);
			mListAdapter.notifyDataSetChanged();
		    return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
     * Load the broadcast receivers installed by applications. Unfortunately, this
     * is a lot more difficult than it sounds.
     *
     * There are multiple approaches one might consider taking, all flawed:
     *
     * 1) Loop through the list of installed packages and collect all receivers.
     *    Unfortunately, Android currently does not allow us to query the intents
     *    a receiver has registered for. See the unimplemented
     *    PackageManager.GET_INTENT_FILTERS, and the following URLs:
     *       http://groups.google.com/group/android-developers/browse_thread/thread/4502827143ea9b20
     *       http://groups.google.com/group/android-developers/browse_thread/thread/ef0e4b390552f2c/
     *
     * 2) Use PackageManager.queryBroadcastReceivers() to find all installed
     *    receivers. In the following thread, hackbod initially suggests that
     *    this could be done using a single call: By using an empty intent filter
     *    with no action, all receivers would be returned. Note however that this
     *    is retracted in a later post:
     *       http://groups.google.com/group/android-developers/browse_thread/thread/3ba4f419f0bec3aa/
     *    Even if that worked, it's still be an open question if we could retrieve
     *    the associated intents, or if we'd again run into the problem from 1).
     *
     *    As a result, using this method we would need a list of builtin, supported
     *    broadcast actions, for each of which we query installed receivers
     *    individually.
     *
     *    Unfortunately, this method has another huge downside: Disabled components
     *    are never returned. As a result, if we want to give the user the option
     *    to toggle receivers on and off, once one has been disabled, we need to
     *    employ various hackery to remember this new status, because a requery
     *    will no longer list the receiver. Considering that applications may be
     *    removed/updated, this is a slightly complex proposition.
     *
     * 3) We could parse the apps inside /data/app manually, potentially based on
     *    the OS Android Code (see for example PackageParser.java). However, this
     *    would be complex to write in any case.
     *
     * For now, we are going with 2).
     */
    private void load() {
        final PackageManager pm = getPackageManager();

        // The structure here is:
        //    [[intent1, [app1, app2]], [intent2, [apps...], intents...]]
        // In words, we have a list of 2-sized arrays, with the first element
        // being the intent, the second being another ArrayList of apps.
        ArrayList<Object[]> receiversByIntent = new ArrayList<Object[]>();

        for (Object[] intent : supportedIntents) {
            Intent query = new Intent();
            query.setAction((String)(intent[0]));
            Log.d(TAG, "Querying receivers for action: "+(String)(intent[0]));
	        List<ResolveInfo> receivers = pm.queryBroadcastReceivers(query,
	        		PackageManager.GET_INTENT_FILTERS);

	        if (receivers.size() <= 0)
	        	// Don't bother adding empty groups.
	        	continue;

	        ArrayList<ResolveInfo> currentAppList = new ArrayList<ResolveInfo>();
	        for (int i=receivers.size()-1; i>=0; i--) {
	        	ResolveInfo r = receivers.get(i);
	        	Log.d(TAG, "Found receiver: "+r.toString());
	        	if (r.activityInfo == null) {
	        		Log.d(TAG, "activityInfo is null?!");
	        		continue;
	        	}

	        	currentAppList.add(r);
	        }

	        receiversByIntent.add(new Object[] { intent, currentAppList });
        }

        mListAdapter = new MyExpandableListAdapter(
        		this, R.layout.group_row, R.layout.child_row, receiversByIntent);
        setListAdapter(mListAdapter);
    }

    // TODO: Instead of showing a toast, fade in a custom info bar, then fade out.
    // This would be an improvement because we could control it better: Show it longer,
    // but have it disappear when the user clicks on it (toasts don't receive clicks).
    public void showInfoToast(int titleRes, int msgRes) {
    	if (mInfoToast == null) {
    		LayoutInflater inflater = getLayoutInflater();
    		View layout = inflater.inflate(R.layout.detail_toast,
    				(ViewGroup) findViewById(R.id.root));
    		mInfoToast = new Toast(this);
    		mInfoToast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0);
    		mInfoToast.setDuration(Toast.LENGTH_LONG);
    		mInfoToast.setView(layout);
    	}
    	((TextView)mInfoToast.getView().findViewById(R.id.title)).setText(titleRes);
    	((TextView)mInfoToast.getView().findViewById(android.R.id.message)).setText(msgRes);
    	mInfoToast.show();
    }

    public class MyExpandableListAdapter extends BaseExpandableListAdapter {

    	private Context mContext;
    	private int mChildLayout;
    	private int mGroupLayout;
    	private ArrayList<Object[]> mDataAll;
    	private ArrayList<Object[]> mDataFiltered;

    	private boolean mShowSystemApps = true;

    	private LayoutInflater mInflater;
    	private PackageManager mPackageManager;

    	@SuppressWarnings("unchecked")
		public MyExpandableListAdapter(Context context, int groupLayout, int childLayout,
    			ArrayList<Object[]> data) {
    		mContext = context;
    		mChildLayout = childLayout;
    		mGroupLayout = groupLayout;
    		mDataAll = data;
    		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    		mPackageManager = context.getPackageManager();

    		// Create a cached copy of the data containing in a filtered manner.
    		// TODO: this should be optimized to support multiple filters, and created on demand?
    		// TODO: instead if if()ing on every access, a member field should point to whatever
    		// we are currently using.
    		mDataFiltered = new ArrayList<Object[]>();
    		for (Object[] row : mDataAll) {
    			Object[] filtered_row = row.clone();
    			filtered_row[1] = new ArrayList<ResolveInfo>();  // needs a new (filtered) list
    			for (ResolveInfo app : (ArrayList<ResolveInfo>)row[1]) {
    				if (!isSystemApp(app))
    					 ((ArrayList<ResolveInfo>)filtered_row[1]).add(app);
    			}
    			if (((ArrayList<ResolveInfo>)filtered_row[1]).size() > 0)
    				mDataFiltered.add(filtered_row);

    		}
		}

        @SuppressWarnings("unchecked")
		public Object getChild(int groupPosition, int childPosition) {
            return ((ArrayList<ResolveInfo>)getGroupData(groupPosition)[1]).get(childPosition);
        }

        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @SuppressWarnings("unchecked")
		public int getChildrenCount(int groupPosition) {
            return ((ArrayList<ResolveInfo>)getGroupData(groupPosition)[1]).size();
        }

        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
        	View v;
        	if (convertView == null)
        		v = mInflater.inflate(mChildLayout, parent, false);
        	else
        		v = convertView;
			ResolveInfo app = (ResolveInfo) getChild(groupPosition, childPosition);
			((ImageView)v.findViewById(R.id.icon)).
				setImageDrawable(app.loadIcon(mPackageManager));
			TextView title = ((TextView)v.findViewById(R.id.title));
			if (isSystemApp(app))
			    title.setTextColor(Color.YELLOW);
			else
				title.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
			CharSequence appTitle = app.activityInfo.applicationInfo.loadLabel(mPackageManager);
			CharSequence receiverTitle = app.loadLabel(mPackageManager);
			if (appTitle.equals(receiverTitle))
				title.setText(appTitle);
			else
				title.setText(appTitle + " ("+receiverTitle+")");
			return v;
        }

        public Object getGroup(int groupPosition) {
            return getGroupData(groupPosition)[0];
        }

        public int getGroupCount() {
            return getData().size();
        }

        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {
        	final View v;
        	if (convertView == null)
        		v = mInflater.inflate(mGroupLayout, parent, false);
        	else
        		v = convertView;
        	final Object[] intent = (Object[])getGroup(groupPosition);
        	((TextView)v.findViewById(R.id.title)).setText((Integer)intent[1]);
        	((View)v.findViewById(R.id.show_info)).setOnClickListener(new OnClickListener() {
				public void onClick(View _v) {
					showInfoToast((Integer)intent[1], (Integer)intent[2]);
				}
        	});
        	return v;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public boolean hasStableIds() {
            return false;
        }


        private ArrayList<Object[]>getData() {
        	if (mShowSystemApps)
        		return mDataAll;
        	else
        		return mDataFiltered;
        }

        /**
         *
         */
        private Object[] getGroupData(int groupPosition) {
        	if (mShowSystemApps)
        		return (Object[])mDataAll.get(groupPosition);
        	else
        		return (Object[])mDataFiltered.get(groupPosition);
        }

        /**
         * Allow owner to hide (and show) the system applications.
         *
         * Returns True if the list is filtered.
         *
         * Expects the caller to also call notifyDataSetChanged(), if neccessary.
         */
        public boolean toggleFilterSystemApps() {
        	mShowSystemApps = !mShowSystemApps;
        	return !mShowSystemApps;
        }
    }

    @Override
	public void onGroupCollapse(int groupPosition) {
		super.onGroupCollapse(groupPosition);
		// TODO: refactor with onGroupExand into one method?
		/*long packedGroupPos = ExpandableListView.
			getPackedPositionForGroup(groupPosition);
		ExpandableListView lv = this.getExpandableListView();
		lv.getChildAt(lv.getFlatListPosition(packedGroupPos)).
			findViewById(R.id.description).setVisibility(View.GONE);*/
	}

	@Override
	public void onGroupExpand(int groupPosition) {
		super.onGroupExpand(groupPosition);
		/*long packedGroupPos = ExpandableListView.
			getPackedPositionForGroup(groupPosition);
		ExpandableListView lv = this.getExpandableListView();
		lv.getChildAt(lv.getFlatListPosition(packedGroupPos)).
			findViewById(R.id.description).setVisibility(View.VISIBLE);*/
	}

	static boolean isSystemApp(ResolveInfo app) {
		return ((ApplicationInfo.FLAG_SYSTEM & app.activityInfo.applicationInfo.flags)
					== ApplicationInfo.FLAG_SYSTEM);
	}
}