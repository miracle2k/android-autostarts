package com.elsdoerfer.android.autostarts;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

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

		// All right. So we can't use the PackageManager
		// to disable components, since the permission
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
			f = activity.openFileOutput(
					scriptFile, ListActivity.MODE_PRIVATE);
			try {
				f.write(String.format("pm %s %s/%s",
						(mDoEnable ? "enable": "disable"),
						mApp.packageName, mApp.componentName).getBytes());
				f.close();

				// TODO: Temporary migration code, remove again.
				// In the future, we could use this utility function to
				// test for root; to make sure it won't look users out,
				// during a test period let's log the result and we can
				// have a look at the logs we get from people.
				Log.i(ListActivity.TAG, "Testing for root: "+Utils.deviceHasRoot());

				Runtime r = Runtime.getRuntime();
				Log.i(ListActivity.TAG, "Asking package manger to "+
						"change component state to "+
						(mDoEnable ? "enabled": "disabled"));
				Process p = r.exec(new String[] {
					"su", "-c", "sh "+activity.getFileStreamPath(scriptFile).getAbsolutePath() });
				p.waitFor();
				Log.d(ListActivity.TAG, "Process returned with "+p.exitValue());
				Log.d(ListActivity.TAG, "Process stdout was: "+
						Utils.readStream(p.getInputStream())+
						"; stderr: "+
						Utils.readStream(p.getErrorStream()));

				// In order to consider this a success, we require to
				// things: a) a proper exit value, and ...
				if (p.exitValue() != 0)
					return false;

				final PackageManager pm = activity.getPackageManager();
				ComponentName c = new ComponentName(
						mApp.packageName, mApp.componentName);
				mApp.currentEnabled = pm.getComponentEnabledSetting(c);
				return (mApp.isCurrentlyEnabled() == mDoEnable);
			}
			finally {
				activity.deleteFile(scriptFile);
			}
		} catch (FileNotFoundException e) {
			Log.e(ListActivity.TAG, "Failed to change state", e);
			return false;
		} catch (IOException e) {
			Log.e(ListActivity.TAG, "Failed to change state", e);
			return false;
		} catch (InterruptedException e) {
			Log.e(ListActivity.TAG, "Failed to change state", e);
			return false;
		}
	}
}