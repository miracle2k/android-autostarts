package com.elsdoerfer.android.autostarts;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.util.Log;

import com.elsdoerfer.android.autostarts.DatabaseHelper.ReceiverData;

/**
 * Takes care of toggling a component's state. This may take a
 * couple of seconds, so we use a thread.
 */
class ToggleTask extends AsyncTask<Object, Object, Boolean> {

	private ListActivity mActivity;
	private ProgressDialog mPg;
	private Boolean mDoEnable;
	private ReceiverData mApp;

	public ToggleTask(ListActivity wrapActivity) {
		super();
		apply(wrapActivity);
	}

	public void apply(ListActivity wrapActivity) {
		mActivity = wrapActivity;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		mPg = new ProgressDialog(mActivity);
		mPg.setIndeterminate(true);
		mPg.setMessage(mActivity.getResources().getString(R.string.please_wait));
		mPg.setCancelable(false);
		mPg.show();
	}

	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		mPg.cancel();
		if (!result) {
			new AlertDialog.Builder(mActivity)
				.setMessage(R.string.state_change_failed)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.error)
				.setPositiveButton(android.R.string.ok, null).show();
		}
		else {
			// We can reasonably expect that the component state
			// changed, so refresh the list.
			mActivity.mListAdapter.notifyDataSetInvalidated();

	        // As a one time warning, make sure the user knows about the
	        // problems with uninstalling the app while receivers are
	        // still disabled (i.e. only show the warning if the user is
			// disabling a component).
			if (!mDoEnable)
				mActivity.showUninstallWarning();
		}
	}

	@Override
	protected Boolean doInBackground(Object... params) {
		mApp = (ReceiverData)params[0];
		// We could also read this right now, but we want to ensure
		// we always do the state change that we announced to the user
		// through the menu item caption (it's unlikely but possible
		// that the component state changed in the background while
		// the user decided what to do).
		mDoEnable = (Boolean)params[1];

		// Remember that we disabled this component. We do this now,
		// before we even attempted to change the state - which might
		// not work. No big deal, the cache entry would just be ignored.
		// If however something does wrong, and the component is
		// disabled but NOT cached, this would be much much worse.
		if (!mDoEnable) {
			DatabaseHelper db = new DatabaseHelper(mActivity);
			db.cacheComponent(mApp);
			db.close();
		}

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
			f = mActivity.openFileOutput(
					scriptFile, ListActivity.MODE_PRIVATE);
			try {
				f.write(String.format("pm %s %s/%s",
						(mDoEnable ? "enable": "disable"),
						mApp.activityInfo.packageName,
						mApp.activityInfo.name).getBytes());
				f.close();

				Runtime r = Runtime.getRuntime();
				Log.i(ListActivity.TAG, "Asking package manger to "+
						"change component state to "+
						(mDoEnable ? "enabled": "disabled"));
				Process p = r.exec(new String[] {
					"su", "-c", "sh "+mActivity.getFileStreamPath(scriptFile).getAbsolutePath() });
				p.waitFor();
				Log.d(ListActivity.TAG, "Process returned with "+
						p.exitValue()+"; stdout: "+
						Utils.readStream(p.getInputStream())+
						"; stderr: "+
						Utils.readStream(p.getErrorStream()));

				// In order to consider this a success, we require to
				// things: a) a proper exit value, and ...
				if (p.exitValue() != 0)
					return false;

				// ..b) the component state must actually have changed.
				final PackageManager pm = mActivity.getPackageManager();
				Boolean newEnabledState;
				try {
					newEnabledState = ListActivity.isComponentEnabled(pm, mApp);
					// update the stored status while we're at it
					mApp.enabled = newEnabledState;
				} catch (NameNotFoundException e) {
					Log.e(ListActivity.TAG, "Unable to check success of state change", e);
					return false;
				}
				return (newEnabledState == mDoEnable);
			}
			finally {
				mActivity.deleteFile(scriptFile);
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