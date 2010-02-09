package com.elsdoerfer.android.autostarts;

import java.util.LinkedHashMap;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

/**
 * The broadcast actions/intents we know about. This allows us to show
 * some basic information and a prettyfied title to the user.
 *
 * TODO: Should this be versioned? I.e. if running an application using
 * new events on an old version of Android, we could hide those events,
 * or mark them as not active.
 */
final class Actions {

	static final Object[][] ALL = {
		// android.intent.*
		{ Intent.ACTION_BOOT_COMPLETED, R.string.act_boot_completed, R.string.act_boot_completed_detail },
		{ ConnectivityManager.CONNECTIVITY_ACTION, R.string.act_connectivity, R.string.act_connectivity_detail },
		{ Intent.ACTION_AIRPLANE_MODE_CHANGED, R.string.act_airplane_mode_changed, R.string.act_airplane_mode_changed_detail },
		{ Intent.ACTION_BATTERY_CHANGED, R.string.act_battery_changed, R.string.act_battery_changed_detail },
		{ Intent.ACTION_BATTERY_LOW, R.string.act_battery_low, R.string.act_battery_low_detail },
		{ Intent.ACTION_BATTERY_OKAY, R.string.act_battery_okay, R.string.act_battery_okay_detail },   // Added in 1.6
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
		{ Intent.ACTION_CAMERA_BUTTON, R.string.act_camera_button, R.string.act_camera_button_detail },
		{ Intent.ACTION_MEDIA_BUTTON, R.string.act_media_button, R.string.act_media_button_detail },
		{ Intent.ACTION_MEDIA_BAD_REMOVAL, R.string.act_media_bad_removal, R.string.act_media_bad_removal_detail },
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
		{ Intent.ACTION_PACKAGE_DATA_CLEARED, R.string.act_package_data_cleared, R.string.act_package_data_cleared_detail  },
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
		{ Intent.ACTION_POWER_CONNECTED, R.string.act_power_connected, R.string.act_power_connected_detail },
		{ Intent.ACTION_POWER_DISCONNECTED, R.string.act_power_disconnected, R.string.act_power_disconnected_detail },
		{ Intent.ACTION_SHUTDOWN, R.string.act_shutdown, R.string.act_shutdown_detail },
		{ Intent.ACTION_DOCK_EVENT, R.string.act_dock_event, R.string.act_dock_event_detail },

		// com.android.camera.*
		{ "com.android.camera.NEW_PICTURE", R.string.act_new_picture, R.string.act_new_picture_detail },

		// TelephonyManager
		{ ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED, R.string.act_background_data_setting_changed, R.string.act_background_data_setting_changed_detail },
		{ TelephonyManager.ACTION_PHONE_STATE_CHANGED, R.string.act_phone_state_changed, R.string.act_phone_state_changed_detail },

		// android.provider.Telephony.*
		{ "android.provider.Telephony.SIM_FULL", R.string.act_sim_full, R.string.act_sim_full_detail },
		{ "android.provider.Telephony.SMS_RECEIVED", R.string.act_sms_received, R.string.act_sms_received_detail },
		{ "android.provider.Telephony.SMS_REJECTED", R.string.act_sms_rejected, R.string.act_sms_rejected_detail },  // new in 2.0
		{ "android.provider.Telephony.WAP_PUSH_RECEIVED", R.string.act_wap_push_received, R.string.act_wap_push_received_detail },
		{ "android.provider.Telephony.SECRET_CODE", R.string.act_secret_code, R.string.act_secret_code_detail },   // not part of the public SDK
		{ "android.provider.Telephony.SPN_STRINGS_UPDATED", R.string.act_spn_strings_updated, R.string.act_spn_strings_updated_detail },  // not part of the public SDK

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

		// android.speech.tts.* (new in 1.6)
		{ "android.speech.tts.TTS_QUEUE_PROCESSING_COMPLETED", R.string.act_tts_queue_completed, R.string.act_tts_queue_completed_detail },
		{ "android.speech.tts.engine.TTS_DATA_INSTALLED", R.string.act_tts_data_installed, R.string.act_tts_data_installed_detail },

		// android.bluetooth.* (officially introduced in 2.0)
		{ "android.bluetooth.a2dp.action.SINK_STATE_CHANGED", R.string.act_sink_state_changed, R.string.act_sink_state_changed_detail },
		{ "android.bluetooth.adapter.action.DISCOVERY_FINISHED", R.string.act_bt_discovery_finished, R.string.act_bt_discovery_finished_detail },
		{ "android.bluetooth.adapter.action.DISCOVERY_STARTED", R.string.act_discovery_started, R.string.act_discovery_started_detail },
		{ "android.bluetooth.adapter.action.LOCAL_NAME_CHANGED", R.string.act_bt_local_name_changed, R.string.act_bt_local_name_changed_detail },
		{ "android.bluetooth.adapter.action.SCAN_MODE_CHANGED", R.string.act_bt_scan_mode_changed, R.string.act_bt_scan_mode_changed_detail }, // see android.bluetooth.intent.action.SCAN_MODE_CHANGED
		{ "android.bluetooth.adapter.action.STATE_CHANGED", R.string.act_bt_state_changed, R.string.act_bt_state_changed_detail },  // see android.bluetooth.intent.action.BLUETOOTH_STATE_CHANGED
		{ "android.bluetooth.device.action.ACL_CONNECTED", R.string.act_bt_acl_connected, R.string.act_bt_acl_connected_detail },
		{ "android.bluetooth.device.action.ACL_DISCONNECTED", R.string.act_bt_acl_disconnected, R.string.act_bt_acl_disconnected_detail },
		{ "android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED", R.string.act_bt_acl_disconnect_requested, R.string.act_bt_acl_disconnect_requested_detail },
		{ "android.bluetooth.device.action.BOND_STATE_CHANGED", R.string.act_bt_bond_state_changed, R.string.act_bt_bond_state_changed_detail }, // see android.bluetooth.intent.action.BOND_STATE_CHANGED_ACTION
		{ "android.bluetooth.device.action.CLASS_CHANGED", R.string.act_bt_class_changed, R.string.act_bt_class_changed_detail },
		{ "android.bluetooth.device.action.FOUND", R.string.act_bt_found, R.string.act_bt_found_detail },
		{ "android.bluetooth.device.action.NAME_CHANGED", R.string.act_bt_name_changed, R.string.act_bt_name_changed_detail },  // see android.bluetooth.intent.action.NAME_CHANGED
		{ "android.bluetooth.devicepicker.action.DEVICE_SELECTED", R.string.act_bt_device_selected, R.string.act_bt_device_selected_detail },
		{ "android.bluetooth.devicepicker.action.LAUNCH", R.string.act_bt_launch, R.string.act_bt_launch_detail },
		{ "android.bluetooth.headset.action.AUDIO_STATE_CHANGED", R.string.act_bt_audio_state_changed, R.string.act_bt_audio_state_changed_detail }, // see android.bluetooth.intent.action.HEADSET_ADUIO_STATE_CHANGED
		{ "android.bluetooth.headset.action.STATE_CHANGED", R.string.act_bt_state_changed, R.string.act_bt_state_changed_detail },
		// Old deprecated 1.5/1.6 events; they are no longer listed in 2.0's broadcast_events.txt,
		// though I haven't tested whether they are really no longer available as well.
		{ "android.bluetooth.a2dp.intent.action.SINK_STATE_CHANGED", R.string.act_sink_state_changed, R.string.act_sink_state_changed_detail },
		{ "android.bluetooth.intent.action.DISCOVERY_COMPLETED", R.string.act_discovery_completed, R.string.act_discovery_completed_detail },
		{ "android.bluetooth.intent.action.DISCOVERY_STARTED", R.string.act_discovery_started, R.string.act_discovery_started_detail },
		{ "android.bluetooth.intent.action.HEADSET_STATE_CHANGED", R.string.act_headset_state_changed, R.string.act_headset_state_changed_detail },
		{ "android.bluetooth.intent.action.NAME_CHANGED", R.string.act_bt_name_changed, R.string.act_bt_name_changed_detail },  // see android.bluetooth.device.action.NAME_CHANGED
		{ "android.bluetooth.intent.action.PAIRING_REQUEST", R.string.act_pairing_request, R.string.act_pairing_request_detail },
		{ "android.bluetooth.intent.action.PAIRING_CANCEL", R.string.act_pairing_cancel, R.string.act_pairing_cancel_detail },
		{ "android.bluetooth.intent.action.REMOTE_DEVICE_CONNECTED", R.string.act_remote_device_connected, R.string.act_remote_device_connected_detail },
		{ "android.bluetooth.intent.action.REMOTE_DEVICE_DISAPPEARED", R.string.act_remote_device_disappeared, R.string.act_remote_device_disappeared_detail },
		{ "android.bluetooth.intent.action.REMOTE_DEVICE_DISCONNECTED", R.string.act_remote_device_disconnected, R.string.act_remote_device_disconnected_detail },
		{ "android.bluetooth.intent.action.REMOTE_DEVICE_DISCONNECT_REQUESTED", R.string.act_remote_device_disconnect_requested, R.string.act_remote_device_disconnect_requested_detail },
		{ "android.bluetooth.intent.action.REMOTE_DEVICE_FOUND", R.string.act_remote_device_found, R.string.act_remote_device_found_detail },
		{ "android.bluetooth.intent.action.REMOTE_NAME_FAILED", R.string.act_remote_name_failed, R.string.act_remote_name_failed_detail },
		{ "android.bluetooth.intent.action.REMOTE_NAME_UPDATED", R.string.act_remote_name_updated, R.string.act_remote_name_updated_detail },
		{ "android.bluetooth.intent.action.BLUETOOTH_STATE_CHANGED", R.string.act_bt_state_changed, R.string.act_bt_state_changed_detail },  // see android.bluetooth.adapter.action.STATE_CHANGED
		{ "android.bluetooth.intent.action.BOND_STATE_CHANGED_ACTION", R.string.act_bt_bond_state_changed, R.string.act_bt_bond_state_changed_detail },  // see android.bluetooth.device.action.BOND_STATE_CHANGED
		{ "android.bluetooth.intent.action.HEADSET_ADUIO_STATE_CHANGED", R.string.act_bt_audio_state_changed, R.string.act_bt_audio_state_changed_detail },  // see android.bluetooth.headset.action.AUDIO_STATE_CHANGED
		{ "android.bluetooth.intent.action.SCAN_MODE_CHANGED", R.string.act_bt_scan_mode_changed, R.string.act_bt_scan_mode_changed },  // see android.bluetooth.adapter.action.SCAN_MODE_CHANGED
		// Bluetooth stuff we had collected from wherever in the time before 2.0, but which doesn't appear in the
		// broadcast_actions.txt files for any version; since 2.0 apparently redesigned/officially introduced the
		// Bluetooth SDKs, we need  to assume that those are probably gone as well - or some of them might still exist, untested.
		{ "android.bluetooth.intent.action.BONDING_CREATED", R.string.act_bonding_created, R.string.act_bonding_created_detail },
		{ "android.bluetooth.intent.action.BONDING_REMOVED", R.string.act_bonding_removed, R.string.act_bonding_removed_detail },
		{ "android.bluetooth.intent.action.DISABLED", R.string.act_disabled, R.string.act_disabled_detail },
		{ "android.bluetooth.intent.action.ENABLED", R.string.act_enabled, R.string.act_enabled_detail },
		{ "android.bluetooth.intent.action.MODE_CHANGED", R.string.act_mode_changed, R.string.act_mode_changed_detail },
		{ "android.bluetooth.intent.action.REMOTE_ALIAS_CHANGED", R.string.act_remote_alias_changed, R.string.act_remote_alias_changed_detail },
		{ "android.bluetooth.intent.action.REMOTE_ALIAS_CLEARED", R.string.act_remote_alias_cleared, R.string.act_remote_alias_cleared_detail },

		// This is a strange one, since there is literally no information
		// out there about it, in fact, GSERVICES_CHANGED has only 3 Google
		// hits, one of them being:
		//    http://www.netmite.com/android/mydroid/cupcake/out/target/common/docs/broadcast_actions.txt
		// { "com.google.gservices.intent.action.GSERVICES_CHANGED" null, null }
		{ "com.google.gservices.intent.action.GSERVICES_OVERRIDE", null, null},   // new in 1.6

		// android.appwidget.*
		// Note that except of UPDATE, the others aren't really sent using a
		// broadcast, or at least widgets usually don't handle them using a
		// broadcast receiver. We have them here anyway, just to be safe.
		{ AppWidgetManager.ACTION_APPWIDGET_UPDATE, R.string.act_appwidget_update, R.string.act_appwidget_update_detail },
		{ AppWidgetManager.ACTION_APPWIDGET_ENABLED, R.string.act_appwidget_enabled, R.string.act_appwidget_enabled_detail},
		{ AppWidgetManager.ACTION_APPWIDGET_DISABLED, R.string.act_appwidget_disabled, R.string.act_appwidget_disabled_detail },
		{ AppWidgetManager.ACTION_APPWIDGET_DELETED, R.string.act_appwidget_deleted, R.string.act_appwidget_deleted_detail },
    };

	static LinkedHashMap<String, Object[]> MAP;

	static {
		// Convert the list of available actions (and their data) into
		// a ordered hash map which we are than able to easily query by
		// action name.
		MAP = new LinkedHashMap<String, Object[]>();
		for (Object[] action : Actions.ALL)
			MAP.put((String)action[0], action);
	}
}