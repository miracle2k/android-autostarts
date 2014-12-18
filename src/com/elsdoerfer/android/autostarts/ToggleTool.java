package com.elsdoerfer.android.autostarts;

import android.Manifest.permission;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.elsdoerfer.android.autostarts.db.ComponentInfo;
import com.stericson.RootTools.execution.Shell;

/**
How we toggle a component's states. Beware: This is a long comment.

Android has setComponentEnabledState(), but to use it on other apps
requires the CHANGE_COMPONENT_ENABLED_STATE permission, which is
signature-protected. Rarely will we be signed with the system
certificate, but for what it's worth, if we do have the permission,
we'll use it and make the call directly.

Normally we don't, and that means we need to use root to make a su
call to the "pm" command. "pm" is included in Android and is a command
line interface to the PackageManager service. Called as root, the
PackageManager will skip the permissions check.

Ideally, here's where the story would end. Unfortunately, there seems
to be a bug *somewhere* (in Dalvik? In the Superuser Whitelist app?)
that causes Process.waitFor() to hang if we run any process that involves
the "app_process" executable while "USB Debugging" is disabled.
"app_process" is a tool included in Android that will spawn a Dalvik VM
and execute Java code (i.e., the actual pm Command is a wrapper script
around app_process, the actual command is implemented as a Java class).

Note that the guilty party indeed is "app_process", not the "pm" command
in particular. Other commands like "ime" exhibit the same behavior.

The fact that the problem only occurs when ADB is disabled makes this
particularly hard to debug.

I've tried all kinds of things to find a solution, running "app_process"
directly, replacing Java's "Process" with a custom native library
calling "system" to run the command, but to no avail. Further options
begin to run thin, but here's some things we could still try:

  a) Try executing "app_process" directly, but through the native
     library. This is very unlikely to make a difference though.

  b) Try to track down the problem and fix it. For starters, is it really
     Superuser Whitelist, or does the standard su also have the same
     problem. In the latter case, we might even get Google to fix it.

  c) Try to write a tool that interacts with the PackageManager service in
     C, bypassing "app_process". For reference, have a look at
     'frameworks/base.git/servicemanager/binder.c.' Note also the
     /dev/binder device.

As a sidenote, a custom replacement for "pm" would also allow us to change
the component state without requirement the restart flag, which would
presumably be a lot faster.

In the meantime, we need to essentially require the user to have USB
Debugging enabled. There are however a few things we can do to help:

   - Again in the rare case that we are installed on the system
     partition, we can enable ADB (and re-disable it) automatically,
     because we have the right to write to the secure settings area.

   - If we don't have WRITE_SECURE_SETTINGS, we can use a "su setprop"
     call to change the ADB Debugging setting.

   - We make it clear to the user that ADB needs to be enabled for
     optional functioning via a bar on the top (this is currently not
     done, since auto-enabling ADB works well enough).

   - We use a timeout when running in action, so that when we can see
     that the state has changed, we simply stop waiting for the process
     to finish. This at least improves the user experience.
*/

/**
 * Takes care of toggling a component's state. This may take a
 * couple of seconds, so we use a thread.
 */
class ToggleTool {

	static interface ToggleToolListener {
		void onComplete(boolean success);
	}

	static protected Boolean toggleState(Context context, ComponentInfo component, boolean doEnable) {
		Log.i(Utils.TAG, "Asking package manger to "+
				"change component state to "+
				(doEnable ? "enabled": "disabled"));

		// As described above, in the rare case we are allowed to use
		// setComponentEnabledSetting(), we should do so.
		if (context.checkCallingOrSelfPermission(permission.CHANGE_COMPONENT_ENABLED_STATE)
				     == PackageManager.PERMISSION_GRANTED) {
			Log.i(Utils.TAG, "Calling setComponentEnabledState() directly");
			PackageManager pm = context.getPackageManager();
			ComponentName c = new ComponentName(component.packageInfo.packageName, component.componentName);
			pm.setComponentEnabledSetting(
					c, doEnable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
						: PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
			component.currentEnabledState = pm.getComponentEnabledSetting(c);
			return (component.isCurrentlyEnabled() == doEnable);
		}
		else {
			Log.i(Utils.TAG, "Changing state by employing root access");

			ContentResolver cr = context.getContentResolver();
			boolean adbNeedsRedisable = false;
			boolean adbEnabled;
			try {
				adbEnabled = (Settings.Secure.getInt(cr, Settings.Secure.ADB_ENABLED) == 1);
			} catch (SettingNotFoundException e) {
				// This started to happen at times on the ICS emulator
				// (and possibly one user reported it).
				Log.w(Utils.TAG,
						"Failed to read adb_enabled setting, assuming no", e);
				adbEnabled = false;
			}

			// If adb is disabled, try to enable it, temporarily. This will
			// make our root call go through without hanging.
			// TODO: It seems this might no longer be required under ICS.
			if (!adbEnabled) {
				Log.i(Utils.TAG, "Switching ADB on for the root call");
				if (setADBEnabledState(context, cr, true)) {
					adbEnabled = true;
					adbNeedsRedisable = true;
					// Let's be extra sure we don't run into any timing-related hiccups.
					Utils.sleep(1000);
				}
			}

			try {
				// Run the command; we have different invocations we can try, but
				// we'll stop at the first one we succeed with.
				//
				// On ICS, it became necessary to set a library path (which is
				// cleared for suid programs, for obvious reasons). It can't hurt
				// on older versions. See also  https://github.com/ChainsDD/su-binary/issues/6
				final String libs = "LD_LIBRARY_PATH=\"$LD_LIBRARY_PATH:/system/lib\" ";
				boolean success = false;
				for (String[] set : new String[][] {
						{ libs+"pm %s '%s/%s'", null },
						{ libs+"sh /system/bin/pm %s '%s/%s'", null },
						{ libs+"app_process /system/bin com.android.commands.pm.Pm %s '%s/%s'", "CLASSPATH=/system/framework/pm.jar" },
						{ libs+"/system/bin/app_process /system/bin com.android.commands.pm.Pm %s '%s/%s'", "CLASSPATH=/system/framework/pm.jar" },
				})
				{
					try {
						if (Utils.runRootCommand(String.format(set[0],
								(doEnable ? "enable": "disable"),
								component.packageInfo.packageName, component.componentName),
								(set[1] != null) ? new String[] { set[1] } : null,
								// The timeout shouldn't really be needed ever, since
								// we now automatically enable ADB, which should work
								// around any freezing issue. However, in rare, hard
								// to reproduce cases, it still occurs, and in those
								// cases the timeout will improve the user experience.
								25000, Shell.ShellContext.UNTRUSTED_APP)) {
							success = true;
							break;
						}
					} catch (Utils.ShellFailedException e) {
						// The shell failed, no reason to try more commands.
						break;
					}
				}

				// We are happy if both the command itself succeed (return code)...
				if (!success)
					return false;

				// ...and the state should now actually be what we expect.
				// TODO: It would be more stable if we would reload
				// getComponentEnabledSetting() regardless of the return code.
				final PackageManager pm = context.getPackageManager();
				ComponentName c = new ComponentName(
						component.packageInfo.packageName, component.componentName);
				component.currentEnabledState = pm.getComponentEnabledSetting(c);

				success = component.isCurrentlyEnabled() == doEnable;
				if (success)
					Log.i(Utils.TAG, "State successfully changed");
				else
					Log.i(Utils.TAG, "State change failed");
				return success;
			}
			finally {
				if (adbNeedsRedisable) {
					Log.i(Utils.TAG, "Switching ADB off again");
					setADBEnabledState(context, cr, false);
					// Delay releasing the GUI for a while, there seems to
					// be a mysterious problem of repeating this process multiple
					// times causing it to somehow lock up, no longer work.
					// I'm hoping this might help.
					Utils.sleep(5000);
				}
			}
		}
	}

	/**
	 * Enable/Disable the "ADB Debugging" setting. We do this either by employing
	 * the WRITE_SECURE_SETTINGS permission, if we have it, or by using a root call.
	 */
	private static boolean setADBEnabledState(Context context, ContentResolver cr, boolean enable) {
		if (context.checkCallingOrSelfPermission(permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED) {
			Log.i(Utils.TAG, "Using secure settings API to touch ADB setting");
			return Settings.Secure.putInt(cr, Settings.Secure.ADB_ENABLED, enable ? 1 : 0);
		}
		else {
			Log.i(Utils.TAG, "Using setprop call to touch ADB setting");
			try {
				return Utils.runRootCommand(
						String.format("setprop persist.service.adb.enable %s", enable ? 1 : 0),
						null, 0, null);
			} catch (Utils.ShellFailedException e) {
				return false;
			}
		}
	}
}