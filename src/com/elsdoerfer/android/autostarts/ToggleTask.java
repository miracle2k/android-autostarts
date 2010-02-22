package com.elsdoerfer.android.autostarts;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.util.Log;

import com.elsdoerfer.android.autostarts.ReceiverReader.ReceiverData;

/**
 * Takes care of toggling a component's state. This may take a
 * couple of seconds, so we use a thread.
 */
class ToggleTask extends ActivityAsyncTask<ListActivity, Object, Object, Boolean> {

	private ProgressDialog mPg;
	private Boolean mDoEnable;
	private ReceiverData mApp;

	public ToggleTask(ListActivity wrapActivity) {
		super(wrapActivity);
	}

	public void connectTo(ListActivity wrappedObject) {
		super.connectTo(wrappedObject);

		// We are being unconnected from the current activity. Make sure
		// we reset any current progress dialog, so we don't think later
		// on that we have to cancel it; we don't need to bother canceling
		// it here either, because as the old activity is destroyed, so
		// is the progress dialog.
		if (wrappedObject == null) {
			mPg = null;
		}
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		mPg = new ProgressDialog(mWrapped);
		mPg.setIndeterminate(true);
		mPg.setMessage(mWrapped.getResources().getString(R.string.please_wait));
		mPg.setCancelable(false);
		mPg.show();
	}

	protected void processPostExecute(Boolean result) {
		if (mPg != null)
			mPg.cancel();

		if (!result) {
			// TODO: Use showDialog() so it's managed by Activity.
			new AlertDialog.Builder(mWrapped)
				.setMessage(R.string.state_change_failed)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.error)
				.setPositiveButton(android.R.string.ok, null).show();
		}
		else {
			// We can reasonably expect that the component state
			// changed, so refresh the list.
			mWrapped.mListAdapter.notifyDataSetInvalidated();
		}
	}

	@Override
	protected Boolean doInBackground(Object... params) {
		// Cache the object locally, since the member might be reset
		// when the Activity disconnects.
		final ListActivity activity = mWrapped;

		mApp = (ReceiverData)params[0];
		// We could also read this right now, but we want to ensure
		// we always do the state change that we announced to the user
		// through the menu item caption (it's unlikely but possible
		// that the component state changed in the background while
		// the user decided what to do).
		mDoEnable = (Boolean)params[1];

		// All right. So we can't use the PackageManager to disable
		// components, since the permission required to do so has a
		// "protectLevel" of "signature", meaning essentially that we
		// need to be signed with the system certificate to be granted
		// it.
		//
		// Fortunately, there is a different way. Android includes a
		// "pm" command to communicate with the package manager, including
		// the ability to tell it enable disable a component. We need to
		// do so as root, so that the PackageManager will skip the
		// permission check (otherwise, it'll still look at the UID of
		// the calling process); so it will only work for root users, but
		// that's better than nothing.
		//
		// Unfortunately, we don't get off that easily. Apparently there
		// is a bug **somewhere** (in Dalvik? In Superuser Whitelist?)
		// that causes Process.waitFor() to hang if we run any process
		// that involves the "app_process" executable. "app_process" is
		// a tool included in Android that will spawn a Dalvik VM and
		// execute Java code (i.e., the actual pm Command is a wrapper
		// script around app_process, the actual command is implemented
		// as a Java class).
		//
		// Note that the guilty party indeed is "app_process", not the
		// "pm" command in particular. Other commands like "ime" exhibit
		// the same behavior.
		//
		// We've tried all kinds of things to find a solution, running
		// "app_process" directly, replacing Java's "Process" with a
		// custom native library calling "system" to run the command, but
		// to no avail. Our further options begin to run thin, but here's
		// some things we could still try:
		//
		// a) Try executing "app_process" directly, but through the native
		//    library. This is very unlikely to work, but we are growing
		//    desperate.
		// b) Try to track down the problem and fix it. For starters, is
		//    it really Superuser Whitelist, or does the standard su also
		//    have the same problem. In the latter case, we might even get
		//    Google to fix it.
		// c) Try to write a tool that interacts with the PackageManager
		//    service in C, bypassing "app_process". For reference, have
		//    a look at frameworks/base.git/servicemanager/binder.c. Note
		//    also the /dev/binder device.
		// d) Hack it: Run the process in a thread, and simply wait until
		//    we determined the state changed occurred, checking every few
		//    seconds; possibly using a timeout.
		// e) Give up. Find a way to determine whether USB debugging is
		//    enabled and enforce this more clearly to the user.
		//
		// Note that a custom replacement for "pm" would also allow us to
		// change the component state without requirement the restart flag,
		// which would presumably be a lot faster.

		Log.i(ListActivity.TAG, "Asking package manger to "+
				"change component state to "+
				(mDoEnable ? "enabled": "disabled"));

		// Run the command; we are only happy if both the command itself
		// succeed (proper return code) ...
		if (!Utils.runRootCommand(String.format("pm %s %s/%s",
						(mDoEnable ? "enable": "disable"),
						mApp.packageName, mApp.componentName)))
			return false;

		// ...and the state should now actually is what we expect.
		final PackageManager pm = activity.getPackageManager();
		ComponentName c = new ComponentName(
				mApp.packageName, mApp.componentName);
		mApp.currentEnabled = pm.getComponentEnabledSetting(c);
		return (mApp.isCurrentlyEnabled() == mDoEnable);
	}
}