package com.elsdoerfer.android.autostarts;

import java.util.ArrayList;

import com.elsdoerfer.android.autostarts.ReceiverReader.ActionWithReceivers;
import com.elsdoerfer.android.autostarts.ReceiverReader.OnLoadProgressListener;


// TODO: We could speed this up (probably not too much) by returning
// only the newly found apps during progress report. I don't think
// the list itself cares when notifyDatasetChanged() is called, but
// at least we don't need to re-filter the whole list on every progress
// report, but can only apply the filter to what comes in new.
// TODO: Note that this would also fix another bug: If the user already
// toggles an app right now while we are still loading, the state change
// will be lost when the next progress report is sent in.
class LoadTask extends ActivityAsyncTask<ListActivity, Object, ArrayList<ActionWithReceivers>, ArrayList<ActionWithReceivers>> {

	public LoadTask(ListActivity initialConnect) {
		super(initialConnect);
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		mWrapped.setProgressBarIndeterminateVisibility(true);
		if (mWrapped.mReloadItem != null)
			mWrapped.mReloadItem.setEnabled(false);
	}

	@Override
	protected ArrayList<ActionWithReceivers> doInBackground(
			Object... params) {
		ReceiverReader reader = new ReceiverReader(mWrapped, new OnLoadProgressListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onProgress(ArrayList<ActionWithReceivers> currentState) {
				publishProgress(currentState);
			}
		});
		return reader.load();
	}

	@Override
	protected void processPostExecute(ArrayList<ActionWithReceivers> result) {
		mWrapped.mReceiversByIntent = result;
		mWrapped.apply();

		mWrapped.setProgressBarIndeterminateVisibility(false);
		if (mWrapped.mReloadItem != null)
			mWrapped.mReloadItem.setEnabled(true);
	}

	@Override
	protected void onProgressUpdate(
			ArrayList<ActionWithReceivers>... values) {
		super.onProgressUpdate(values);
		if (mWrapped != null) {
			mWrapped.mReceiversByIntent = values[0];
			mWrapped.apply();
		}
	}
}