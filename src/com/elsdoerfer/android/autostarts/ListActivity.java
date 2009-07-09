package com.elsdoerfer.android.autostarts;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ListActivity extends ExpandableListActivity {

	static final String TAG = "Autostarts";

	// The broadcast intents we support. Read about why this needs to be a
	// predefined, static list below in the comments on where we do the
	// actual loading of the installed receivers.
	//
	// When doing a custom Android build, apparently a file
	// "common/docs/broadcast_actions.txt" is generated, containing a list
	// of all (?) available system broadcast actions.
	//
	// Unless otherwise noted, all the actions listed below are included
	// in that file, though note that the order may have changed to
	// prioritize more important broadcasts.
	// TODO: Would it be better to sort the list by the number of apps
	// registered for each broadcast, rather than have a manual order?
	static final Object[][] supportedIntents = {
		// android.intent.*
		{ Intent.ACTION_BOOT_COMPLETED, R.string.act_boot_completed, R.string.act_boot_completed_detail },
		{ Intent.ACTION_AIRPLANE_MODE_CHANGED, R.string.act_airplane_mode_changed, R.string.act_airplane_mode_changed_detail },
		{ Intent.ACTION_BATTERY_CHANGED, R.string.act_battery_changed, R.string.act_battery_changed_detail },
		{ Intent.ACTION_BATTERY_LOW, R.string.act_battery_low, R.string.act_battery_low_detail },
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
		{ Intent.ACTION_WALLPAPER_CHANGED, R.string.act_wallpaper_changed, R.string.act_wallpaper_changed_detail },

		// android.provider.Telephony.*
		{ "android.provider.Telephony.SIM_FULL", R.string.act_sim_full, R.string.act_sim_full_detail },
		{ "android.provider.Telephony.SMS_RECEIVED", R.string.act_sms_received, R.string.act_sms_received_detail },
		{ "android.provider.Telephony.WAP_PUSH_RECEIVED", R.string.act_wap_push_received, R.string.act_wap_push_received_detail },

		// android.net.wifi.*
		{ WifiManager.NETWORK_IDS_CHANGED_ACTION, R.string.act_network_ids_changed, R.string.act_network_ids_changed_detail },
		{ WifiManager.RSSI_CHANGED_ACTION, R.string.act_rssi_changed, R.string.act_rssi_changed_detail },
		{ WifiManager.SCAN_RESULTS_AVAILABLE_ACTION, R.string.act_scan_results_available, R.string.act_scan_results_available_detail },
		{ WifiManager.NETWORK_STATE_CHANGED_ACTION, R.string.act_network_state_changed, R.string.act_network_state_changed_detail },
		{ WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION, R.string.act_supplicant_connection_change, R.string.act_supplicant_connection_change_detail },
		{ WifiManager.SUPPLICANT_STATE_CHANGED_ACTION, R.string.act_suplicant_state_changed, R.string.act_suplicant_state_changed_detail },

		// android.media.*
		{ AudioManager.RINGER_MODE_CHANGED_ACTION, R.string.act_ringer_mode_changed, R.string.act_ringer_mode_changed_detail },
		{ AudioManager.VIBRATE_SETTING_CHANGED_ACTION, R.string.act_vibrate_setting_changed, R.string.act_vibrate_setting_changed_detail },
		{ AudioManager.ACTION_AUDIO_BECOMING_NOISY, R.string.act_audio_becoming_noisy, R.string.act_audio_becoming_noisy_detail },  // POTENTIALLY NOT IN "broadcast_actions.txt"!

		// android.bluetooth.*
		{ "android.bluetooth.a2dp.intent.action.SINK_STATE_CHANGED", R.string.act_sink_state_changed, R.string.act_sink_state_changed_detail },
		{ "android.bluetooth.intent.action.BONDING_CREATED", R.string.act_bonding_created, R.string.act_bonding_created_detail },
		{ "android.bluetooth.intent.action.BONDING_REMOVED", R.string.act_bonding_removed, R.string.act_bonding_removed_detail },
		{ "android.bluetooth.intent.action.DISABLED", R.string.act_disabled, R.string.act_disabled_detail },
		{ "android.bluetooth.intent.action.DISCOVERY_COMPLETED", R.string.act_discovery_completed, R.string.act_discovery_completed_detail },
		{ "android.bluetooth.intent.action.DISCOVERY_STARTED", R.string.act_discovery_started, R.string.act_discovery_started_detail },
		{ "android.bluetooth.intent.action.ENABLED", R.string.act_enabled, R.string.act_enabled_detail },
		{ "android.bluetooth.intent.action.HEADSET_STATE_CHANGED", R.string.act_headset_state_changed, R.string.act_headset_state_changed_detail },
		{ "android.bluetooth.intent.action.MODE_CHANGED", R.string.act_mode_changed, R.string.act_mode_changed_detail },
		{ "android.bluetooth.intent.action.NAME_CHANGED", R.string.act_name_changed, R.string.act_name_changed_detail },
		{ "android.bluetooth.intent.action.PAIRING_REQUEST", R.string.act_pairing_request, R.string.act_pairing_request_detail },
		{ "android.bluetooth.intent.action.PAIRING_CANCEL", R.string.act_pairing_cancel, R.string.act_pairing_cancel_detail },
		{ "android.bluetooth.intent.action.REMOTE_ALIAS_CHANGED", R.string.act_remote_alias_changed, R.string.act_remote_alias_changed_detail },
		{ "android.bluetooth.intent.action.REMOTE_ALIAS_CLEARED", R.string.act_remote_alias_cleared, R.string.act_remote_alias_cleared_detail },
		{ "android.bluetooth.intent.action.REMOTE_DEVICE_CONNECTED", R.string.act_remote_device_connected, R.string.act_remote_device_connected_detail },
		{ "android.bluetooth.intent.action.REMOTE_DEVICE_DISAPPEARED", R.string.act_remote_device_disappeared, R.string.act_remote_device_disappeared_detail },
		{ "android.bluetooth.intent.action.REMOTE_DEVICE_DISCONNECTED", R.string.act_remote_device_disconnected, R.string.act_remote_device_disconnected_detail },
		{ "android.bluetooth.intent.action.REMOTE_DEVICE_DISCONNECT_REQUESTED", R.string.act_remote_device_disconnect_requested, R.string.act_remote_device_disconnect_requested_detail },
		{ "android.bluetooth.intent.action.REMOTE_DEVICE_FOUND", R.string.act_remote_device_found, R.string.act_remote_device_found_detail },
		{ "android.bluetooth.intent.action.REMOTE_NAME_FAILED", R.string.act_remote_name_failed, R.string.act_remote_name_failed_detail },
		{ "android.bluetooth.intent.action.REMOTE_NAME_UPDATED", R.string.act_remote_name_updated, R.string.act_remote_name_updated_detail },

		// This is a strange one, since there is literally no information
		// out there about it, in fact, GSERVICES_CHANGED has only 3 google
		// hits, one of them being:
		//    http://www.netmite.com/android/mydroid/cupcake/out/target/common/docs/broadcast_actions.txt
		// { "com.google.gservices.intent.action.GSERVICES_CHANGED" null, null }

		// NOTE: The actions below ARE NOT LISTED in "broadcast_actions.txt",
		// but were collected manually.
		{ ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED, R.string.act_background_data_setting_changed, R.string.act_background_data_setting_changed_detail },
		// android.appwidget.*
		// Note that except of UPDATE, the others aren't really sent using a
		// broadcast, or at least widgets usually don't handle them using a
		// broadcast receiver. We have them here anyway, just to be safe.
		{ AppWidgetManager.ACTION_APPWIDGET_UPDATE, R.string.act_appwidget_update, R.string.act_appwidget_update_detail },
		{ AppWidgetManager.ACTION_APPWIDGET_ENABLED, R.string.act_appwidget_enabled, R.string.act_appwidget_enabled_detail},
		{ AppWidgetManager.ACTION_APPWIDGET_DISABLED, R.string.act_appwidget_disabled, R.string.act_appwidget_disabled_detail },
		{ AppWidgetManager.ACTION_APPWIDGET_DELETED, R.string.act_appwidget_deleted, R.string.act_appwidget_deleted_detail },
    };

	static final private int MENU_FILTER = 1;
	static final private int MENU_EXPAND_COLLAPSE = 2;
	static final private int MENU_RELOAD = 3;
	static final private int MENU_HELP = 4;
	static final private int RECEIVER_DETAIL = 1;

	private Toast mInfoToast;
	private MyExpandableListAdapter mListAdapter;
	private MenuItem mExpandCollapseToggleItem;

	private Integer[] mLastSelectedItem = { -1, -1 };
	private Boolean mExpandSuggested = true;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);

        Object retained = getLastNonConfigurationInstance();
        if (retained != null)
        	mLastSelectedItem = (Integer[]) retained;

        mListAdapter = new MyExpandableListAdapter(
        		this, R.layout.group_row, R.layout.child_row);
        setListAdapter(mListAdapter);

        load();
    }

    @Override
	public Object onRetainNonConfigurationInstance() {
		return mLastSelectedItem;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mInfoToast != null)
			mInfoToast.cancel();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_FILTER, 0, R.string.toggle_sys_apps).
			setIcon(R.drawable.ic_menu_view);
		mExpandCollapseToggleItem =
			menu.add(0, MENU_EXPAND_COLLAPSE, 0, R.string.expand_all).
				setIcon(R.drawable.ic_collapse_expand);
		menu.add(0, MENU_RELOAD, 0, R.string.reload).
			setIcon(R.drawable.ic_menu_refresh);
		menu.add(0, MENU_HELP, 0, R.string.help).
			setIcon(R.drawable.ic_menu_help);
		return true;
	}

    @Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// Decide whether we want to offer the option to collapse, or
		// expand, depending on the current group expansion count.
    	ExpandableListView lv = getExpandableListView();
    	int expandCount = 0;
    	int numGroups = mListAdapter.getGroupCount();
		for (int i=numGroups-1; i>=0; i--) {
			if (lv.isGroupExpanded(i))
				expandCount += 1;
		}
		if (expandCount / (float)numGroups >= 0.5) {
			mExpandCollapseToggleItem.setTitle(R.string.collapse_all);
			mExpandSuggested = false;
		}
		else {
			mExpandCollapseToggleItem.setTitle(R.string.expand_all);
			mExpandSuggested = true;
		}
		return super.onPrepareOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_FILTER:
			if (mListAdapter.toggleFilterSystemApps())
				((TextView)findViewById(android.R.id.empty)).setText(R.string.no_receivers_filtered);
			else
				((TextView)findViewById(android.R.id.empty)).setText(R.string.no_receivers);
			mListAdapter.notifyDataSetChanged();
		    return true;
		case MENU_EXPAND_COLLAPSE:
			ExpandableListView lv = getExpandableListView();
			for (int i=mListAdapter.getGroupCount()-1; i>=0; i--)
				if (mExpandSuggested)
					lv.expandGroup(i);
				else
					lv.collapseGroup(i);
			return true;
		case MENU_RELOAD:
			load();
			return true;
		case MENU_HELP:
			startActivity(new Intent(this, HelpActivity.class));
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == RECEIVER_DETAIL) {
			View v = getLayoutInflater().inflate(
				R.layout.receiver_info_panel, null, false);
			Dialog d = new AlertDialog.Builder(this).setItems(
				new CharSequence[] {
						getResources().getString(R.string.appliation_info),
						getResources().getString(R.string.find_in_market),
						getResources().getString(R.string.disable)},
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						ResolveInfo app = (ResolveInfo) mListAdapter.getChild(
								mLastSelectedItem[0], mLastSelectedItem[1]);
						switch (which) {
						case 0:
							Intent infoIntent = new Intent();
							// From android-cookbook/GroupHome - it notes:
							// "we shouldnt rely on this entrance into the settings app"
							infoIntent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
							infoIntent.putExtra("com.android.settings.ApplicationPkgName",
			                	app.activityInfo.applicationInfo.packageName);
			                startActivity(infoIntent);
							break;
						case 1:
							try {
								Intent marketIntent = new Intent(Intent.ACTION_VIEW);
								marketIntent.setData(Uri.parse("market://search?q=pname:"+
										app.activityInfo.applicationInfo.packageName));
								startActivity(marketIntent);
							}
							catch (ActivityNotFoundException e) {}
							break;
						case 2:
							// All right. So we can't use the PackageManager
							// to disable comonents, since the permission
							// required to do so has a protectLevel of
							// "signature", meaning essentially that we need
							// to be signed with the system certificate to
							// be able to get it.
							//
							// Fortunately, there is a sort-of workaround.
							// We can use the "pm" executable to communicate
							// with the package manager, tell it to enable
							// or disable a component. We need to do so as
							// root, so that the PackageManager will skip the
							// permission check (otherwise, it'll still look
							// at the UID of the calling process), but it
							// would work for root users.
							//
							// However, there is another complication. Rooted
							// devices these days often use custom firmware
							// mods, and those often come with a special "su"
							// executable, that asks the user for permission
							// whenever a program wants to use su. This
							// special su does support "Always allow", but
							// considers the command arguments as well. In
							// other words, since we need to use different
							// arguments to "pm" every time, the user would
							// be asked to confirm us being allowed to use
							// "su" everytime.
							//
							// The workaround here is to write our command to
							// a shell file, then in turn ask su to run that
							// file.
							final String scriptFile = "pm-call.sh";
							FileOutputStream f;
							try {
								f = openFileOutput(
										scriptFile, MODE_PRIVATE);
								try {
									f.write(String.format("pm disable %s/%s",
											app.activityInfo.packageName,
											app.activityInfo.name).getBytes());
									f.close();

									Runtime r = Runtime.getRuntime();
									Log.i(TAG, "Asking package manger to "+
											"change component state.");
									Process p = r.exec(new String[] {
										"su", "-c", "sh "+getFileStreamPath(scriptFile).getAbsolutePath() });
									p.waitFor();
									Log.d(TAG, "Process returned with "+
											p.exitValue()+"; stdout: "+
											readStream(p.getInputStream())+
											"; stderr: "+
											readStream(p.getErrorStream()));
								}
								finally {
									deleteFile(scriptFile);
								}
							} catch (FileNotFoundException e) {
								throw new RuntimeException(e);
							} catch (IOException e) {
								throw new RuntimeException(e);
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
							break;
						}
						dialog.dismiss();
					}
				})
				// Setting a dummy title is necessary for the title
				// control to be activated in the first place,
				// apparently. We assign the actual title in
				// onPrepareDialog().
				.setTitle("dummy").setView(v).create();

			// Due to a bug in Android, onPrepareDialog() is not called
			// when an existing dialog is restored on orientation change.
			// We therefore need to make sure ourselfs that the dialog
			// is initialized correctly in this case as well. Note that
			// our current implementation means that the prepare code
			// will be run twice when the dialog is created for the first
			// time under normal circumstances.
			prepareReceiverDetailDialog(d, v);
			return d;
		}
		else
			return super.onCreateDialog(id);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		prepareReceiverDetailDialog(dialog, dialog.getWindow().getDecorView());
	}

	/**
	 * We cannot just rename this to "onPrepareDialog()", since the
	 * dialog isn't fully created while processing "onCreateDialog()".
	 *
	 * As a result, the view objects needs to be accessed differently,
	 * and is thus handled via an argument here.
	 */
	private void prepareReceiverDetailDialog(Dialog dialog, View view) {
		Object[] intent = (Object[]) mListAdapter.getGroup(mLastSelectedItem[0]);
		ResolveInfo app = (ResolveInfo) mListAdapter.getChild(
			mLastSelectedItem[0], mLastSelectedItem[1]);

		dialog.setTitle(app.loadLabel(getPackageManager()));

		int t = 0;
		SpannableStringBuilder b = new SpannableStringBuilder();
		b.append("Receiver ");
		t = b.length();
		b.append(app.activityInfo.name);
		b.setSpan(new StyleSpan(Typeface.BOLD), t, b.length(), 0);
		b.append(" handles action ");
		t = b.length();
		b.append((String)(intent[0]));
		b.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), t, b.length(), 0);
		b.append(" with priority ");
		t = b.length();
		b.append(Integer.toString(app.priority));
		b.setSpan(new StyleSpan(Typeface.BOLD), t, b.length(), 0);
		b.append(".");

		((TextView)view.findViewById(R.id.message)).setText(b);
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		mLastSelectedItem[0] = groupPosition;
		mLastSelectedItem[1] = childPosition;
		showDialog(RECEIVER_DETAIL);
		return super.onChildClick(parent, v, groupPosition, childPosition, id);
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
    // TODO: move this to an ASyncTask
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

        mListAdapter.setData(receiversByIntent);
        mListAdapter.notifyDataSetChanged();
    }

    // TODO: Instead of showing a toast, fade in a custom info bar, then fade out.
    // This would be an improvement because we could control it better: Show it longer,
    // but have it disappear when the user clicks on it (toasts don't receive clicks).
    public void showInfoToast(Object[] intent) {
    	if (mInfoToast == null) {
    		LayoutInflater inflater = getLayoutInflater();
    		View layout = inflater.inflate(R.layout.detail_toast,
    				(ViewGroup) findViewById(R.id.root));
    		mInfoToast = new Toast(this);
    		mInfoToast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0);
    		mInfoToast.setDuration(Toast.LENGTH_LONG);
    		mInfoToast.setView(layout);
    	}
    	((TextView)mInfoToast.getView().findViewById(R.id.title)).setText(getIntentName(intent));
    	TextView message = ((TextView)mInfoToast.getView().findViewById(android.R.id.message));
    	CharSequence info = "";
    	if (intent[2] != null)  // Hide info text both for null and empty string values.
    		info = getResources().getText((Integer)intent[2]);
    	if (!info.equals("")) {
    		message.setText(info);
    		message.setVisibility(View.VISIBLE);
    	} else {
    		message.setVisibility(View.GONE);
    	}
    	mInfoToast.show();
    }

	public class MyExpandableListAdapter extends BaseExpandableListAdapter {

    	private int mChildLayout;
    	private int mGroupLayout;
    	private ArrayList<Object[]> mDataAll;
    	private ArrayList<Object[]> mDataFiltered;

    	private boolean mShowSystemApps = true;

    	private LayoutInflater mInflater;
    	private PackageManager mPackageManager;

		public MyExpandableListAdapter(Context context, int groupLayout, int childLayout) {
    		mChildLayout = childLayout;
    		mGroupLayout = groupLayout;
    		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    		mPackageManager = context.getPackageManager();
    		setData(new ArrayList<Object[]>());
    	}

    	@SuppressWarnings("unchecked")
		public void setData(ArrayList<Object[]> data) {
    		mDataAll = data;

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
            return ((ResolveInfo)getChild(groupPosition, childPosition)).activityInfo.name.hashCode();
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
        	return getGroupData(groupPosition)[0].hashCode();
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {
        	final View v;
        	if (convertView == null)
        		v = mInflater.inflate(mGroupLayout, parent, false);
        	else
        		v = convertView;
        	final Object[] intent = (Object[])getGroup(groupPosition);
        	((TextView)v.findViewById(R.id.title)).setText(getIntentName(intent));
        	((View)v.findViewById(R.id.show_info)).setOnClickListener(new OnClickListener() {
				public void onClick(View _v) {
					showInfoToast(intent);
				}
        	});
        	return v;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public boolean hasStableIds() {
            return true;
        }


        /**
         * Return the dataset currently used.
         */
        private ArrayList<Object[]>getData() {
        	if (mShowSystemApps)
        		return mDataAll;
        	else
        		return mDataFiltered;
        }

        /**
         * Return the data record for the given group.
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
         * Expects the caller to also call notifyDataSetChanged(), if necessary.
         */
        public boolean toggleFilterSystemApps() {
        	mShowSystemApps = !mShowSystemApps;
        	return !mShowSystemApps;
        }
    }

    /**
     * Return a name for the given intent; tries the pretty name,
     * if available, and falls back to the raw class name.
     */
	private String getIntentName(Object[] intent) {
		return (intent[1] != null) ?
				getResources().getString((Integer)intent[1]) :
				(String)intent[0];
	}

	/**
	 * True if this app is installed on the system partition.
	 */
	static boolean isSystemApp(ResolveInfo app) {
		return ((ApplicationInfo.FLAG_SYSTEM & app.activityInfo.applicationInfo.flags)
					== ApplicationInfo.FLAG_SYSTEM);
	}

	/**
	 * It's unbelievable how difficult it is in Java to read a stupid
	 * stream into a string.
	 *
	 * From:
	 * 	 http://stackoverflow.com/questions/309424/in-java-how-do-a-read-an-input-stream-in-to-a-string
	 */
	static String readStream(InputStream stream) throws IOException {
		final char[] buffer = new char[0x10000];
		StringBuilder out = new StringBuilder();
		Reader in = new InputStreamReader(stream, "UTF-8");
		int read;
		do {
			read = in.read(buffer, 0, buffer.length);
			if (read>0)
				out.append(buffer, 0, read);
		} while (read>=0);
		return out.toString();
	}
}