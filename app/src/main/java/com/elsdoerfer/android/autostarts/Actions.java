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
 * TODO: This is becoming hard to  manage. Convert this to an XML file,
 * with additional information like version added etc. When running an
 * application using new events on an old version of Android, we could
 * hide those events, or mark them as not active.
 *
 * I am using these key words in a fourth column:
 *   - registered (dynamically registered receivers only: "only registered receiver" in docs,
 *     FLAG_RECEIVER_REGISTERED_ONLY, e.g. ACTION_SCREEN_ON, https://groups.google.com/forum/#!topic/android-platform/gQI-RN1fODw)
 */
final class Actions {

	static final Object[][] ALL = {
		// Those our users care most about, we'd like to have those in front.
		{ "android.intent.action.PRE_BOOT_COMPLETED", R.string.act_pre_boot_completed, R.string.act_pre_boot_completed_detail },
		{ Intent.ACTION_BOOT_COMPLETED, R.string.act_boot_completed, R.string.act_boot_completed_detail },
		// Added to broadcast_actions in 18 only, but exists since API Level 1
		{ ConnectivityManager.CONNECTIVITY_ACTION, R.string.act_connectivity, R.string.act_connectivity_detail },

		// android.intent.*
		{ Intent.ACTION_AIRPLANE_MODE_CHANGED, R.string.act_airplane_mode_changed, R.string.act_airplane_mode_changed_detail },
		{ Intent.ACTION_BATTERY_CHANGED, R.string.act_battery_changed, R.string.act_battery_changed_detail },
		{ Intent.ACTION_BATTERY_LOW, R.string.act_battery_low, R.string.act_battery_low_detail },
		{ Intent.ACTION_BATTERY_OKAY, R.string.act_battery_okay, R.string.act_battery_okay_detail },   // Added in 1.6
		{ Intent.ACTION_CLOSE_SYSTEM_DIALOGS, R.string.act_close_system_dialogs, R.string.act_close_system_dialogs_detail },
		{ Intent.ACTION_CONFIGURATION_CHANGED, R.string.act_configuration_changed, R.string.act_configuration_changed_detail },
		{ Intent.ACTION_LOCALE_CHANGED, R.string.act_locale_changed, R.string.act_locale_changed_detail },  // new in api level 7.
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
		// Now deprecated, supposedly was never used:
		{ "android.content.Intent.ACTION_PACKAGE_INSTALL", R.string.act_package_install, R.string.act_package_install_detail },
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
		// Deprecated and removed from broadcast_actions as of level 14 (but maybe still sent):
		{ "android.content.Intent.ACTION_UMS_CONNECTED", R.string.act_ums_connected, R.string.act_ums_connected_detail },
		// Deprecated and removed from broadcast_actions as of level 14 (but maybe still sent):
		{ "android.content.Intent.ACTION_UMS_DISCONNECTED", R.string.act_ums_disconnected, R.string.act_ums_disconnected_detail },
		{ Intent.ACTION_USER_PRESENT, R.string.act_user_present, R.string.act_user_present_detail },
		// Deprecated as of level 16 (but maybe still sent?):
		{ "android.content.Intent.ACTION_WALLPAPER_CHANGED", R.string.act_wallpaper_changed, R.string.act_wallpaper_changed_detail },
		{ Intent.ACTION_POWER_CONNECTED, R.string.act_power_connected, R.string.act_power_connected_detail },
		{ Intent.ACTION_POWER_DISCONNECTED, R.string.act_power_disconnected, R.string.act_power_disconnected_detail },
		{ Intent.ACTION_SHUTDOWN, R.string.act_shutdown, R.string.act_shutdown_detail },
		{ Intent.ACTION_DOCK_EVENT, R.string.act_dock_event, R.string.act_dock_event_detail },
		{ "android.intent.action.ANR", R.string.act_anr, R.string.act_anr_detail },
		{ "android.intent.action.EVENT_REMINDER", R.string.act_event_reminder, R.string.act_event_reminder_detail },
		{ "android.accounts.LOGIN_ACCOUNTS_CHANGED", R.string.act_login_accounts_changed, R.string.act_login_accounts_changed_detail },
		{ "android.intent.action.STATISTICS_REPORT", R.string.act_statistics_report, R.string.act_statistics_report_detail },
		{ "android.intent.action.MASTER_CLEAR", R.string.act_master_clear, R.string.act_master_clear_detail },
		{ "com.android.sync.SYNC_CONN_STATUS_CHANGED", R.string.act_sync_connection_setting_changed, R.string.act_sync_connection_setting_changed_detail }, // SYNC_CONNECTION_SETTING_CHANGED_INTENT
		{ "android.bluetooth.headset.action.STATE_CHANGED", R.string.act_bt_state_changed, R.string.act_bt_state_changed_detail },
		// New in API Level 11:
		{ "android.intent.action.PROXY_CHANGE", R.string.act_proxy_changed, R.string.act_proxy_changed_detail },
		// Added in API Level 4:
		{ "android.search.action.SETTINGS_CHANGED", R.string.act_search_settings_changed, R.string.act_search_settings_changed_detail },
		{ "android.search.action.SEARCHABLES_CHANGED", R.string.act_searchables_changed, R.string.act_searchables_changed_detail },
		// Added in API level 9:
		{ "android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED", R.string.act_download_notification_clicked, R.string.act_download_notification_clicked_detail },  // added to broadcast_actions in 19
		{ "android.intent.action.DOWNLOAD_COMPLETE", R.string.act_download_completed, R.string.act_download_completed_detail },   // added to broadcast_actions in 19
		{ "android.location.PROVIDERS_CHANGED", R.string.act_location_providers_changed, R.string.act_location_providers_changed_detail },
		{ "android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION", R.string.act_open_audio_effect_session, R.string.act_open_audio_effect_session_detail },
		{ "android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION", R.string.act_close_audio_effect_session, R.string.act_close_audio_effect_session_detail },
		// New in API Level 8:
		{ Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE, R.string.act_external_apps_available, R.string.act_external_apps_available_detail },
		{ Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE, R.string.act_external_apps_unavailable, R.string.act_external_apps_unavailable_detail },
		{ "android.app.action.ACTION_PASSWORD_CHANGED" , R.string.act_password_changed, R.string.act_password_changed_detail },
		{ "android.app.action.ACTION_PASSWORD_FAILED", R.string.act_password_failed, R.string.act_password_failed_detail },
		{ "android.app.action.ACTION_PASSWORD_SUCCEEDED", R.string.act_password_succeeded, R.string.act_password_succeeded_detail },
		{ "android.app.action.DEVICE_ADMIN_DISABLED", R.string.act_device_admin_disabled, R.string.act_device_admin_disabled_detail },
		{ "android.app.action.DEVICE_ADMIN_DISABLE_REQUESTED", R.string.act_device_admin_disable_req, R.string.act_device_admin_disable_req_detail },
		{ "android.app.action.DEVICE_ADMIN_ENABLED", R.string.act_device_admin_enabled, R.string.act_device_admin_enabled_detail },
		// New in API Level 11:
		{ "android.app.action.ACTION_PASSWORD_EXPIRING", R.string.act_password_expiring, R.string.act_password_expiring_detail },
		// New in API Level 16:
		{ "android.hardware.input.action.QUERY_KEYBOARD_LAYOUTS", R.string.act_query_keyboard_layouts, R.string.act_query_keyboard_layouts_detail },
		{ "android.net.nsd.STATE_CHANGED", R.string.act_nsd_state_changed, R.string.act_nsd_state_changed_detail },
		// New in API Level 21:
		{ "android.os.action.POWER_SAVE_MODE_CHANGED", R.string.act_power_save_mode_changed, R.string.act_power_save_mode_changed_detail, "registered" },
		{ "android.app.action.LOCK_TASK_ENTERING", R.string.act_task_locked, R.string.R_string_act_task_locked_detail, "targeted" },
		{ "android.app.action.LOCK_TASK_EXITING", R.string.act_task_unlocked, R.string.act_task_unlocked_detail, "targeted" },
		{ "android.app.action.NEXT_ALARM_CLOCK_CHANGED", R.string.act_next_alarm_changed, R.string.act_next_alarm_changed_detail, "registered" },
		{ "android.app.action.PROFILE_PROVISIONING_COMPLETE", R.string.act_provisioning_complete, R.string.act_provisioning_complete_detail },
		{ "android.intent.action.APPLICATION_RESTRICTIONS_CHANGED", R.string.act_app_restrictions_changed, R.string.act_app_restrictions_changed_detail },
		{ "android.media.action.HDMI_AUDIO_PLUG", R.string.act_hdmi_plugged, R.string.act_hdmi_plugged_detail },
		// sent to a specific user? { "android.hardware.hdmi.action.OSD_MESSAGE", "", "" },
		// ?? android.net.scoring.SCORER_CHANGED
		// ?? android.net.scoring.SCORE_NETWORKS


		// com.android.launcher.*
		{ "com.android.launcher.action.INSTALL_SHORTCUT", R.string.act_install_shortcut, R.string.act_install_shortcut_detail },
		{ "com.android.launcher.action.UNINSTALL_SHORTCUT", R.string.act_uninstall_shortcut, R.string.act_uninstall_shortcut_detail },

		// com.android.camera.*
		{ "com.android.camera.NEW_PICTURE", R.string.act_new_picture, R.string.act_new_picture_detail },

		// Added in API Level 17, enabled only in 4.2
		{ "android.intent.action.DREAMING_STARTED", R.string.act_dreaming_started, R.string.act_dreaming_started_detail },
		{ "android.intent.action.DREAMING_STOPPED", R.string.act_dreaming_stopped, R.string.act_dreaming_stopped_detail },

		// Added in API Level 17, requires android.Manifest.permission.PACKAGE_VERIFICATION_AGENT to be received.
		{ "android.intent.action.PACKAGE_VERIFIED", R.string.act_package_verified, R.string.act_package_verified_detail },  // https://android.googlesource.com/platform/frameworks/base/+/d1b5cfc94ae940f42be352e7ed98c21c973471b2%5E!/
		// Added in broadcast_actions 14
		{ "android.intent.action.PACKAGE_FULLY_REMOVED", R.string.act_package_fully_removed, R.string.act_package_fully_removed_detail },  // https://android.googlesource.com/platform/frameworks/base/+/e09cd7914c117e84bf78676d0e760c51aa147eb8%5E1..e09cd7914c117e84bf78676d0e760c51aa147eb8/
		{ "android.intent.action.PACKAGE_NEEDS_VERIFICATION", R.string.act_package_needs_verification, R.string.act_package_needs_verification_detail },  // https://android.googlesource.com/platform/frameworks/base/+/5ab2157bf1f105b02d3e2913cd3a33f9765b74ca%5E!/   // Requires android.Manifest.permission.PACKAGE_VERIFICATION_AGENT  // Ordered Broadcast

		// Added in broadcast_actions 18
		{ "android.nfc.action.ADAPTER_STATE_CHANGED", R.string.act_nfc_adapter_state_changed, R.string.act_nfc_adapter_state_changed_detail },

		// Added in broadcast_actions 19
		{ "android.intent.action.CONTENT_CHANGED", R.string.act_content_changed, R.string.act_content_changed_detail },

		// Added in broadcast_actions 14
		{ "android.intent.action.CONTENT_CHANGED", R.string.act_content_changed, R.string.act_content_changed_detail },
		{ "android.intent.action.NEW_VOICEMAIL", R.string.act_new_voicemail, R.string.act_new_voicemail_detail },
		{ "android.intent.action.FETCH_VOICEMAIL", R.string.act_fetch_voicemail, R.string.act_fetch_voicemail_detail },
		{ "android.hardware.action.NEW_VIDEO", R.string.act_new_video, R.string.act_new_video_detail },
		{ "android.hardware.action.NEW_PICTURE", R.string.act_new_picture, R.string.act_new_picture_detail },

		// Added in broadcast_actions 12
		{ "android.intent.action.MY_PACKAGE_REPLACED", R.string.act_my_package_replaced, R.string.act_my_package_replaced_detail },
		{ "android.intent.action.PACKAGE_FIRST_LAUNCH", R.string.act_package_first_launch, R.string.act_package_first_launch_detail },

		// TelephonyManager
		// Now longer sent as of level 16.
		{ "android.net.ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED", R.string.act_background_data_setting_changed, R.string.act_background_data_setting_changed_detail },
		{ TelephonyManager.ACTION_PHONE_STATE_CHANGED, R.string.act_phone_state_changed, R.string.act_phone_state_changed_detail },

		// telephony/TelephonyIntents.java
		{ "android.intent.action.SERVICE_STATE", R.string.act_service_state, R.string.act_service_state_detail },
		{ "android.intent.action.ANY_DATA_STATE", R.string.act_any_data_state, R.string.act_any_data_state_detail },
		{ "android.intent.action.SIG_STR", R.string.act_signal_strength, R.string.act_signal_strength_detail },
		{ "android.intent.action.DATA_CONNECTION_FAILED", R.string.act_data_connection_failed, R.string.act_data_connection_failed_detail },
		{ "android.intent.action.NETWORK_SET_TIME", R.string.act_network_set_time, R.string.act_network_set_time_detail },
		{ "ndroid.intent.action.NETWORK_SET_TIMEZONE", R.string.act_network_set_timezone, R.string.act_network_set_timezone_detail },
		{ "android.intent.action.SIM_STATE_CHANGED", R.string.act_sim_state_changed, R.string.act_sim_state_changed_detail },

		// android.provider.Telephony.*
		{ "android.provider.Telephony.SECRET_CODE", R.string.act_secret_code, R.string.act_secret_code_detail },   // not part of the public SDK
		{ "android.provider.Telephony.SPN_STRINGS_UPDATED", R.string.act_spn_strings_updated, R.string.act_spn_strings_updated_detail },  // not part of the public SDK
		// Added to broadcast_actions in 14, removed in 17, re-added in 19:
		{ "android.provider.Telephony.SMS_EMERGENCY_CB_RECEIVED", R.string.act_sms_emergency_cb_received, R.string.act_sms_emergency_cb_received_detail },
		{ "android.provider.Telephony.SMS_CB_RECEIVED", R.string.act_sms_cb_received, R.string.act_sms_cb_received_detail },
		// Removed in broadcast_actions 17, re-added in 18.
		{ "android.intent.action.DATA_SMS_RECEIVED", R.string.act_data_sms_received, R.string.act_data_sms_received_detail },   // diff namespace, but fits here
		{ "android.provider.Telephony.SIM_FULL", R.string.act_sim_full, R.string.act_sim_full_detail },
		{ "android.provider.Telephony.WAP_PUSH_RECEIVED", R.string.act_wap_push_received, R.string.act_wap_push_received_detail },
		{ "android.provider.Telephony.SMS_RECEIVED", R.string.act_sms_received, R.string.act_sms_received_detail },
		{ "android.provider.Telephony.SMS_REJECTED", R.string.act_sms_rejected, R.string.act_sms_rejected_detail },  // new in 2.0
		// New in broadcast_actions 16, removed in 17, officially added in 19.
		{ "android.provider.Telephony.SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED", R.string.act_sms_service_category_pdr, R.string.act_sms_service_category_pdr_detail },
		// Added in broadcast_actions 19
		{ "android.provider.Telephony.SMS_DELIVER", R.string.act_sms_deliver, R.string.act_sms_deliver_detail },  // http://android-developers.blogspot.de/2013/10/getting-your-sms-apps-ready-for-kitkat.html
		{ "android.provider.Telephony.WAP_PUSH_DELIVER", R.string.act_wap_push_deliver, R.string.act_wap_push_deliver_detail },  // http://android-developers.blogspot.de/2013/10/getting-your-sms-apps-ready-for-kitkat.html

		// android.net.wifi.*
		{ WifiManager.WIFI_STATE_CHANGED_ACTION, R.string.act_wifi_state_changed, R.string.act_wifi_state_changed_detail },
		{ WifiManager.NETWORK_IDS_CHANGED_ACTION, R.string.act_network_ids_changed, R.string.act_network_ids_changed_detail },
		{ WifiManager.RSSI_CHANGED_ACTION, R.string.act_rssi_changed, R.string.act_rssi_changed_detail },
		{ WifiManager.SCAN_RESULTS_AVAILABLE_ACTION, R.string.act_scan_results_available, R.string.act_scan_results_available_detail },
		{ WifiManager.NETWORK_STATE_CHANGED_ACTION, R.string.act_network_state_changed, R.string.act_network_state_changed_detail },
		{ WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION, R.string.act_supplicant_connection_change, R.string.act_supplicant_connection_change_detail },
		{ WifiManager.SUPPLICANT_STATE_CHANGED_ACTION, R.string.act_suplicant_state_changed, R.string.act_suplicant_state_changed_detail },
		// New in API Level 16.
		{ "android.net.wifi.p2p.DISCOVERY_STATE_CHANGE", R.string.act_wifi_p2p_discovery_state_change, R.string.act_wifi_p2p_discovery_state_change_detail },
		// New in API Level 14.
		{ "android.net.wifi.p2p.CONNECTION_STATE_CHANGE", R.string.act_wifi_p2p_connection_state_change, R.string.act_wifi_p2p_connection_state_change_detail },
		{ "android.net.wifi.p2p.PEERS_CHANGED", R.string.act_wifi_p2p_peers_changed, R.string.act_wifi_p2p_peers_changed_detail },
		{ "android.net.wifi.p2p.STATE_CHANGED", R.string.act_wifi_p2p_state_changed, R.string.act_wifi_p2p_state_changed_detail },
		{ "android.net.wifi.p2p.THIS_DEVICE_CHANGED", R.string.act_wifi_p2p_this_device_changed, R.string.act_wifi_p2p_this_device_changed_detail },

		// android.media.*
		{ AudioManager.RINGER_MODE_CHANGED_ACTION, R.string.act_ringer_mode_changed, R.string.act_ringer_mode_changed_detail },
		// Deprecated with level 16 (but maybe still sent?)
		{ "android.media.AudioManager.VIBRATE_SETTING_CHANGED_ACTION", R.string.act_vibrate_setting_changed, R.string.act_vibrate_setting_changed_detail },
		{ AudioManager.ACTION_AUDIO_BECOMING_NOISY, R.string.act_audio_becoming_noisy, R.string.act_audio_becoming_noisy_detail },  // POTENTIALLY NOT IN "broadcast_actions.txt"!
		// New in broadcast_actions 14
		{ "android.media.ACTION_SCO_AUDIO_STATE_UPDATED", R.string.act_sco_audio_state_changed, R.string.act_sco_audio_state_changed_detail },

		// android.speech.tts.* (new in 1.6)
		{ "android.speech.tts.TTS_QUEUE_PROCESSING_COMPLETED", R.string.act_tts_queue_completed, R.string.act_tts_queue_completed_detail },
		{ "android.speech.tts.engine.TTS_DATA_INSTALLED", R.string.act_tts_data_installed, R.string.act_tts_data_installed_detail },

		// android.bluetooth.* (officially introduced in 2.0)
		{ "android.bluetooth.adapter.action.DISCOVERY_FINISHED", R.string.act_bt_discovery_finished, R.string.act_bt_discovery_finished_detail },
		{ "android.bluetooth.adapter.action.DISCOVERY_STARTED", R.string.act_discovery_started, R.string.act_discovery_started_detail },
		{ "android.bluetooth.adapter.action.LOCAL_NAME_CHANGED", R.string.act_bt_local_name_changed, R.string.act_bt_local_name_changed_detail },
		{ "android.bluetooth.adapter.action.SCAN_MODE_CHANGED", R.string.act_bt_scan_mode_changed, R.string.act_bt_scan_mode_changed_detail }, // see android.bluetooth.intent.action.SCAN_MODE_CHANGED
		{ "android.bluetooth.adapter.action.STATE_CHANGED", R.string.act_bt_state_changed, R.string.act_bt_state_changed_detail },  // see android.bluetooth.intent.action.BLUETOOTH_STATE_CHANGED
		{ "android.bluetooth.device.action.PAIRING_REQUEST", R.string.act_pairing_request, R.string.act_pairing_request_detail },   // see android.bluetooth.intent.action.PAIRING_REQUEST; added to broadcast_actions in 19.
		{ "android.bluetooth.device.action.PAIRING_CANCEL", R.string.act_pairing_cancel, R.string.act_pairing_cancel },   // see android.bluetooth.intent.action.PAIRING_CANCEL
		{ "android.bluetooth.device.action.ACL_CONNECTED", R.string.act_bt_acl_connected, R.string.act_bt_acl_connected_detail },
		{ "android.bluetooth.device.action.ACL_DISCONNECTED", R.string.act_bt_acl_disconnected, R.string.act_bt_acl_disconnected_detail },
		{ "android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED", R.string.act_bt_acl_disconnect_requested, R.string.act_bt_acl_disconnect_requested_detail },
		{ "android.bluetooth.device.action.BOND_STATE_CHANGED", R.string.act_bt_bond_state_changed, R.string.act_bt_bond_state_changed_detail }, // see android.bluetooth.intent.action.BOND_STATE_CHANGED_ACTION
		{ "android.bluetooth.device.action.CLASS_CHANGED", R.string.act_bt_class_changed, R.string.act_bt_class_changed_detail },
		{ "android.bluetooth.device.action.FOUND", R.string.act_bt_found, R.string.act_bt_found_detail },
		{ "android.bluetooth.device.action.NAME_CHANGED", R.string.act_bt_name_changed, R.string.act_bt_name_changed_detail },  // see android.bluetooth.intent.action.NAME_CHANGED
		{ "android.bluetooth.devicepicker.action.DEVICE_SELECTED", R.string.act_bt_device_selected, R.string.act_bt_device_selected_detail },
		{ "android.bluetooth.devicepicker.action.LAUNCH", R.string.act_bt_launch, R.string.act_bt_launch_detail },
		// Potentially deprecated in API Level 11 (no longer in broadcast_events.txt).
		{ "android.bluetooth.headset.action.AUDIO_STATE_CHANGED", R.string.act_bt_audio_state_changed, R.string.act_bt_audio_state_changed_detail }, // see android.bluetooth.intent.action.HEADSET_ADUIO_STATE_CHANGED
		{ "android.bluetooth.headset.action.STATE_CHANGED", R.string.act_bt_state_changed, R.string.act_bt_state_changed_detail },
		{ "android.bluetooth.a2dp.action.SINK_STATE_CHANGED", R.string.act_sink_state_changed, R.string.act_sink_state_changed_detail },
		// New in API Level 11
		{ "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGE", R.string.act_bt_a2dp_connection_state_changed, R.string.act_bt_a2dp_connection_state_changed_detail },
		{ "android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED", R.string.act_bt_a2dp_playing_state_changed, R.string.act_bt_a2dp_playing_state_changed_detail },
		{ "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED", R.string.act_bt_connection_state_changed, R.string.act_bt_connection_state_changed_detail },
		{ "android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED", R.string.act_bt_headset_audio_state_changed, R.string.act_bt_headset_audio_state_changed_detail },
		{ "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED", R.string.act_bt_headset_connection_state_changed, R.string.act_bt_headset_connection_state_changed_detail },
		{ "android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT", R.string.act_bt_headset_vendor_event, R.string.act_bt_headset_vendor_event_detail },
		// Added in API Level 15
		{ "android.bluetooth.device.action.UUID", R.string.act_bt_uuid, R.string.act_bt_uuid_detail },
		// Old deprecated 1.5/1.6 events; they are no longer listed in 2.0's broadcast_events.txt,
		// though I haven't tested whether they are really no longer available as well.
		{ "android.bluetooth.a2dp.intent.action.SINK_STATE_CHANGED", R.string.act_sink_state_changed, R.string.act_sink_state_changed_detail },
		{ "android.bluetooth.intent.action.DISCOVERY_COMPLETED", R.string.act_discovery_completed, R.string.act_discovery_completed_detail },
		{ "android.bluetooth.intent.action.DISCOVERY_STARTED", R.string.act_discovery_started, R.string.act_discovery_started_detail },
		{ "android.bluetooth.intent.action.HEADSET_STATE_CHANGED", R.string.act_headset_state_changed, R.string.act_headset_state_changed_detail },
		{ "android.bluetooth.intent.action.NAME_CHANGED", R.string.act_bt_name_changed, R.string.act_bt_name_changed_detail },  // see android.bluetooth.device.action.NAME_CHANGED
		{ "android.bluetooth.intent.action.PAIRING_REQUEST", R.string.act_pairing_request, R.string.act_pairing_request_detail },  // see android.bluetooth.device.action.PAIRING_REQUEST
		{ "android.bluetooth.intent.action.PAIRING_CANCEL", R.string.act_pairing_cancel, R.string.act_pairing_cancel_detail },  // see android.bluetooth.device.action.PAIRING_CANCEL
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
		// Added in broadcast_actions 12
		{ "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED", R.string.act_bt_input_connection_state_changed, R.string.act_bt_input_connection_state_changed_detail },
		{ "android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED", R.string.act_bt_pan_connection_state_changed, R.string.act_bt_pan_connection_state_changed_detail },
		// Removed in broadcast_actions 12
		{ "android.bluetooth.inputdevice.action.INPUT_DEVICE_STATE_CHANGED", R.string.act_bt_input_connection_state_changed, R.string.act_bt_input_connection_state_changed_detail },
		{ "android.bluetooth.pan.action.STATE_CHANGED", R.string.act_bt_pan_connection_state_changed, R.string.act_bt_pan_connection_state_changed_detail },

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

	/**
	 * Helper to sort actions based on the order in our map.
	 */
	static public int compare(String action1, String action2) {
		int idx1 = Utils.getHashMapIndex(Actions.MAP, action1);
		int idx2 = Utils.getHashMapIndex(Actions.MAP, action2);
		// Make sure that unknown intents (-1) are sorted at the bottom.
		if (idx1 == -1 && idx2 == -1)
			return action1.compareTo(action2);
		else if (idx1 == -1)
			return +1;
		else if (idx2 == -1)
			return -1;
		else
			return ((Integer)idx1).compareTo(idx2);
	}
}