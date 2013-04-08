package com.elsdoerfer.android.autostarts;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class ViewOptionsFragment extends DialogFragment {

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final ListActivity activity = (ListActivity) getActivity();
		final MyExpandableListAdapter adapter = activity.mListAdapter;
		final SharedPreferences prefs = activity.mPrefs;

		// We are just hoping that the state of these vars can
		// never change without going through this dialog;
		// otherwise, we'd need to find a solution to ensure
		// that when the instance is reused for a showing at a
		// later point, that the state is still up-to-date, i.e.
		// setting the current state in onPrepareDialog. It's not
		// clear if making the state array a class member and
		// updating it would be enough, or if we actually have to
		// find the correct views and switch them...
		boolean[] initState = new boolean[] {
			adapter.getFilterSystemApps(),
			adapter.getShowChangedOnly(),
			adapter.getFilterUnknown(),
		};

		return new AlertDialog.Builder(activity)
			.setMultiChoiceItems(new CharSequence[]{
					getString(R.string.hide_sys_apps),
					getString(R.string.show_changed_only),
					getString(R.string.hide_unknown),
			},
			initState,
			new DialogInterface.OnMultiChoiceClickListener() {
				public void onClick(DialogInterface dialog, int which,
				                    boolean isChecked) {
					if (which == 0) {
						adapter.setFilterSystemApps(isChecked);
						adapter.notifyDataSetChanged();
						activity.updateEmptyText();
						prefs.edit().putBoolean(ListActivity.PREF_FILTER_SYS_APPS, isChecked).commit();
					} else if (which == 1) {
						adapter.setShowChangedOnly(isChecked);
						adapter.notifyDataSetChanged();
						activity.updateEmptyText();
						prefs.edit().putBoolean(ListActivity.PREF_FILTER_SHOW_CHANGED, isChecked).commit();
					} else if (which == 2) {
						adapter.setFilterUnknown(isChecked);
						adapter.notifyDataSetChanged();
						activity.updateEmptyText();
						prefs.edit().putBoolean(ListActivity.PREF_FILTER_UNKNOWN, isChecked).commit();
					}
				}
			})
			.setPositiveButton(android.R.string.ok, null).create();
	}
}
